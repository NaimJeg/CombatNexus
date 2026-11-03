package io.github.naimjeg.combatnexus.combat.damage.core.settlement;

import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageParentRef;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageOrigin;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageSettlementStatus;
import io.github.naimjeg.combatnexus.combat.damage.api.event.DamageSettlementCallback;
import io.github.naimjeg.combatnexus.combat.damage.api.event.DamageSettlementListener;
import io.github.naimjeg.combatnexus.combat.damage.core.lifecycle.DamageModuleLifecycle;
import io.github.naimjeg.combatnexus.combat.damage.core.lifecycle.DamageModuleRegistrationAccess;
import io.github.naimjeg.combatnexus.combat.damage.core.util.JvmFatalErrors;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatListenerRegistry;
import java.util.List;
import java.util.Objects;

/** Frozen registry and per-listener dynamic authority boundary. */
public final class DamageSettlementCallbacks {
    @ApiStatus.Internal
    public static final int MIN_PRIORITY = -10_000;
    @ApiStatus.Internal
    public static final int MAX_PRIORITY = 10_000;

    private static final CombatListenerRegistry<DamageSettlementListener> REGISTRY = new CombatListenerRegistry<>();

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
        REGISTRY.register(id, priority, listener);
    }

    public static synchronized void freeze(
            DamageModuleRegistrationAccess access
    ) {
        DamageModuleLifecycle.requireRegistering(
                access, "freezeSettlementListeners");
        REGISTRY.freeze();
    }

    static void dispatch(DamageSettlementSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        var currentEntries = REGISTRY.entries();
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
        for (var entry : currentEntries) {
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
        return REGISTRY.frozen() ? REGISTRY.entries().stream().map(CombatListenerRegistry.Entry::id).toList() : List.of();
    }

    @ApiStatus.Internal
    public static synchronized void resetForTesting() {
        REGISTRY.clear();
    }

}
