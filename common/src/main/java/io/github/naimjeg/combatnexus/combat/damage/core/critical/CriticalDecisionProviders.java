package io.github.naimjeg.combatnexus.combat.damage.core.critical;

import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.critical.*;
import io.github.naimjeg.combatnexus.combat.damage.core.lifecycle.DamageModuleLifecycle;
import io.github.naimjeg.combatnexus.combat.damage.core.lifecycle.DamageModuleRegistrationAccess;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageModuleContext;
import io.github.naimjeg.combatnexus.combat.damage.core.util.JvmFatalErrors;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

/** Frozen registry and transactional execution boundary for critical providers. */
public final class CriticalDecisionProviders {
    public static final int MIN_PRIORITY = -10_000;
    public static final int MAX_PRIORITY = 10_000;
    private static final Comparator<Entry> ORDER = Comparator
            .comparingInt(Entry::priority).reversed()
            .thenComparing(entry -> entry.id().toString());
    private static final Map<Identifier, Entry> MUTABLE = new LinkedHashMap<>();
    private static volatile List<Entry> entries = List.of();
    private static boolean frozen;

    private CriticalDecisionProviders() {
    }

    public static synchronized void register(
            DamageModuleRegistrationAccess access,
            Identifier id,
            int priority,
            CriticalDecisionProvider provider
    ) {
        DamageModuleLifecycle.requireRegistering(access, "registerCriticalDecisionProvider");
        if (frozen) throw new IllegalStateException("Critical decision provider registry is frozen");
        Identifier safeId = Objects.requireNonNull(id, "id");
        if (priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            throw new IllegalArgumentException("Critical provider priority must be between "
                    + MIN_PRIORITY + " and " + MAX_PRIORITY);
        }
        Entry entry = new Entry(safeId, priority, Objects.requireNonNull(provider, "provider"));
        if (MUTABLE.putIfAbsent(safeId, entry) != null) {
            throw new IllegalArgumentException("Duplicate critical decision provider ID: " + safeId);
        }
    }

    public static synchronized void freeze(DamageModuleRegistrationAccess access) {
        DamageModuleLifecycle.requireRegistering(access, "freezeCriticalDecisionProviders");
        if (frozen) throw new IllegalStateException("Critical decision provider registry already frozen");
        ArrayList<Entry> sorted = new ArrayList<>(MUTABLE.values());
        sorted.sort(ORDER);
        entries = List.copyOf(sorted);
        frozen = true;
    }

    public static void collect(DamageModuleContext context) {
        Objects.requireNonNull(context, "context");
        if (!(context.victim().level() instanceof ServerLevel level)
                || !level.getServer().isSameThread()) {
            throw new IllegalStateException("Critical providers require the authoritative server thread");
        }
        DamageContextView view = context.restrictedContextView();
        for (Entry entry : entries) {
            ScopedCollector collector = new ScopedCollector(entry);
            try {
                entry.provider().contribute(view, collector);
                collector.commit(context);
            } catch (Throwable throwable) {
                JvmFatalErrors.rethrowIfFatal(throwable);

            } finally {
                collector.close();
            }
        }
    }

    public static List<Identifier> orderedIds() {
        return entries.stream().map(Entry::id).toList();
    }

    @ApiStatus.Internal
    public static synchronized void resetForTesting() {
        MUTABLE.clear();
        entries = List.of();
        frozen = false;
    }

    private record Entry(Identifier id, int priority, CriticalDecisionProvider provider) {
    }

    private static final class ScopedCollector implements CriticalDecisionCollector {
        private final Entry entry;
        private boolean open = true;
        private CriticalDecision decision = CriticalDecision.DEFAULT;

        private ScopedCollector(Entry entry) {
            this.entry = entry;
        }

        @Override
        public CriticalDecisionContributionResult contribute(CriticalDecision contribution) {
            if (!open) throw new IllegalStateException("Critical decision collector is closed");
            Objects.requireNonNull(contribution, "decision");
            if (contribution == CriticalDecision.DEFAULT) {
                return CriticalDecisionContributionResult.REJECTED_DEFAULT;
            }
            if (decision == contribution) return CriticalDecisionContributionResult.DUPLICATE;
            if (decision == CriticalDecision.DEFAULT) {
                decision = contribution;
                return CriticalDecisionContributionResult.ACCEPTED;
            }
            decision = CriticalDecision.SUPPRESS_CRITICAL;

            return CriticalDecisionContributionResult.CONFLICT_RESOLVED_TO_SUPPRESS;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        private void commit(DamageModuleContext context) {
            if (decision != CriticalDecision.DEFAULT) {
                context.addCriticalDecisionContribution(new CriticalDecisionContribution(
                        entry.id(), entry.priority(), decision));
            }
        }

        private void close() {
            open = false;
        }
    }
}
