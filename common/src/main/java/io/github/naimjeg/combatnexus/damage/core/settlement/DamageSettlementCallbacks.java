package io.github.naimjeg.combatnexus.damage.core.settlement;

import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.api.damage.DamageParentRef;
import io.github.naimjeg.combatnexus.damage.api.damage.DamageOrigin;
import io.github.naimjeg.combatnexus.damage.api.damage.DamageSettlementSnapshot;
import io.github.naimjeg.combatnexus.damage.api.damage.DamageSettlementStatus;
import io.github.naimjeg.combatnexus.damage.api.event.DamageSettlementCallback;
import io.github.naimjeg.combatnexus.damage.api.event.DamageSettlementListener;
import io.github.naimjeg.combatnexus.damage.core.lifecycle.DamageModuleLifecycle;
import io.github.naimjeg.combatnexus.damage.core.lifecycle.DamageModuleRegistrationAccess;
import io.github.naimjeg.combatnexus.damage.core.util.JvmFatalErrors;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DamageModuleDiagnosticState;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Frozen registry and per-listener dynamic authority boundary. */
public final class DamageSettlementCallbacks {

    @ApiStatus.Internal
    public static final int MIN_PRIORITY = -10_000;
    @ApiStatus.Internal
    public static final int MAX_PRIORITY = 10_000;

    private static final Comparator<Entry> ORDER = Comparator
            .comparingInt(Entry::priority).reversed()
            .thenComparing(entry -> entry.id().toString());
    private static final Map<Identifier, Entry> MUTABLE =
            new LinkedHashMap<>();
    private static volatile List<Entry> entries = List.of();
    private static volatile boolean frozen;

    private DamageSettlementCallbacks() {
    }

    public static synchronized void register(
            DamageModuleRegistrationAccess access,
            Identifier id,
            int priority,
            DamageSettlementListener listener
    ) {
        DamageModuleLifecycle.requireRegistering(
                access, "registerSettlementListener");
        if (frozen) {
            throw new IllegalStateException(
                    "Damage settlement listener registry is frozen");
        }
        Identifier safeId = Objects.requireNonNull(id, "id");
        if (priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            throw new IllegalArgumentException(
                    "Settlement listener priority must be between "
                            + MIN_PRIORITY + " and " + MAX_PRIORITY);
        }
        Entry entry = new Entry(
                safeId,
                priority,
                Objects.requireNonNull(listener, "listener")
        );
        if (MUTABLE.putIfAbsent(safeId, entry) != null) {
            throw new IllegalArgumentException(
                    "Duplicate damage settlement listener ID: " + safeId);
        }
    }

    public static synchronized void freeze(
            DamageModuleRegistrationAccess access
    ) {
        DamageModuleLifecycle.requireRegistering(
                access, "freezeSettlementListeners");
        if (frozen) {
            throw new IllegalStateException(
                    "Damage settlement listener registry already frozen");
        }
        ArrayList<Entry> sorted = new ArrayList<>(MUTABLE.values());
        sorted.sort(ORDER);
        entries = List.copyOf(sorted);
        frozen = true;
    }

    static void dispatch(DamageSettlementSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (!frozen) {
            throw new IllegalStateException(
                    "Damage settlement listener registry is not frozen");
        }
        List<Entry> currentEntries = entries;
        if (currentEntries.isEmpty()) {
            return;
        }
        Object serverIdentity = snapshot.level().getServer();
        if (serverIdentity == null
                || !snapshot.level().getServer().isSameThread()) {
            throw new IllegalStateException(
                    "Settlement callbacks require the authoritative server thread"
            );
        }

        boolean grantsChildAuthority = snapshot.status()
                == DamageSettlementStatus.APPLIED;
        for (Entry entry : currentEntries) {
            DamageParentRef authority = grantsChildAuthority
                    ? createAuthorityForInvocation(snapshot.origin())
                    : null;
            DamageSettlementCallback callback =
                    DamageSettlementCallback.createInternal(
                            snapshot, authority);
            try (DamageSettlementDispatchScope.Scope ignored =
                         DamageSettlementDispatchScope.openCallback(
                                 callback, authority, serverIdentity)) {
                entry.listener().onDamageSettled(callback);
            } catch (Throwable throwable) {
                JvmFatalErrors.rethrowIfFatal(throwable);
                if (DamageModuleDiagnosticState.shouldLog(
                        DamageModuleDiagnosticState.Domain.EVENT_DISPATCH,
                        entry.id().toString(),
                        "settlement_callback",
                        throwable.getClass().getName())) {
                    DamageModule.LOGGER.error(
                            "[DamageModule] Registered settlement callback {} "
                                    + "failed after damage {} was committed; "
                                    + "later callbacks will continue.",
                            entry.id(),
                            snapshot.lineage().damageId(),
                            throwable
                    );
                }
            }
        }
    }

    static DamageParentRef createAuthorityForInvocation(
            DamageOrigin parentOrigin
    ) {
        return DamageParentRef.createInternal(parentOrigin);
    }

    @ApiStatus.Internal
    public static List<Identifier> orderedIds() {
        return entries.stream().map(Entry::id).toList();
    }

    @ApiStatus.Internal
    public static synchronized void resetForTesting() {
        MUTABLE.clear();
        entries = List.of();
        frozen = false;
    }

    private record Entry(
            Identifier id,
            int priority,
            DamageSettlementListener listener
    ) {
    }
}
