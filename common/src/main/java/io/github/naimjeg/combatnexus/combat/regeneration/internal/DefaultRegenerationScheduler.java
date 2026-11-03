package io.github.naimjeg.combatnexus.combat.regeneration.internal;
import io.github.naimjeg.combatnexus.combat.regeneration.spi.RegenerationConditions;

import io.github.naimjeg.combatnexus.combat.regeneration.api.*;
import io.github.naimjeg.combatnexus.combat.healing.api.*;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Custom recovery only, server-thread confined. Missed ticks never generate catch-up healing. */
public final class DefaultRegenerationScheduler implements RegenerationSchedulerControl {
    public RegenerationScheduler businessView() {
        return new RegenerationScheduler() {
            public void replace(RegenerationPlan plan) { DefaultRegenerationScheduler.this.replace(plan); }
            public void remove(UUID target, net.minecraft.resources.Identifier source) { DefaultRegenerationScheduler.this.remove(target, source); }
        };
    }
    public java.util.List<RegenerationPlan> snapshot(UUID target) { requireOpen(); return plans.values().stream().filter(p -> p.plan.target().equals(target)).map(p -> p.plan).toList(); }
    private record Key(UUID target, Identifier source) {}
    private static final class Pending {
        final RegenerationPlan plan;
        double accrued;
        int ticks;
        long task;
        final long order;
        Pending(RegenerationPlan plan, long order) { this.plan = plan; this.order = order; }
        void reset() { accrued = 0; ticks = 0; }
    }
    private final Map<Key, Pending> plans = new LinkedHashMap<>();
    private final io.github.naimjeg.combatnexus.combat.api.scheduling.DueQueue<Key> due = new io.github.naimjeg.combatnexus.combat.api.scheduling.DueQueue<>(4096);
    private long sequence;
    private final RegenerationConditions conditions;
    private final Map<UUID, Long> lastDamage = new HashMap<>();
    private static final long COMBAT_WINDOW_TICKS = 100;

    @Override public void recordDamage(UUID target, long gameTick) {
        // Internal settlement bookkeeping is valid even when the completed hit forbids child gameplay.
        // CombatRuntime verifies the current settlement callback and world before invoking this control.
        requireOpen();
        if (gameTick < 0) throw new IllegalArgumentException("Negative damage tick");
        lastDamage.merge(Objects.requireNonNull(target), gameTick, Math::max);
    }
    private final Thread owner = Thread.currentThread();
    private boolean closed;
    private void requireOpen() {
        if (closed || Thread.currentThread() != owner) throw new IllegalStateException("Recovery session closed or wrong thread");
    }
    @Override public void close() { requireOpen(); reset(); closed = true; }
    private long lastTick = Long.MIN_VALUE;

    public DefaultRegenerationScheduler(RegenerationConditions conditions) {
        this.conditions = Objects.requireNonNull(conditions, "conditions");
    }

    @Override public void replace(RegenerationPlan plan) {
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase(io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.REGENERATION);
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase(); requireOpen();
        Key key = new Key(plan.target(), plan.source().id());
        Pending previous = plans.get(key);
        if (previous == null || !previous.plan.equals(plan)) {
            if (previous == null && plans.size() >= 4096) throw new IllegalStateException("Recovery plan capacity exceeded");
            if (previous != null) due.cancel(previous.task);
            var pending = new Pending(plan, previous == null ? ++sequence : previous.order);
            pending.task = due.schedule(lastTick == Long.MIN_VALUE ? 0 : Math.addExact(lastTick, 1), key);
            plans.put(key, pending);
        }
    }

    @Override public void tick(long gameTick, HealingService healing) { requireOpen();
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        Objects.requireNonNull(healing, "healing");
        if (gameTick < 0 || lastTick != Long.MIN_VALUE && gameTick < lastTick) {
            throw new IllegalArgumentException("Clock moved backwards; reset the world session first");
        }
        if (gameTick == lastTick) return;
        lastTick = gameTick;
        var ready = due.poll(gameTick).stream().map(task -> Map.entry(task.value(), plans.get(task.value())))
                .sorted(Comparator.comparingLong(entry -> entry.getValue().order)).toList();
        // Reserve all next opportunities before any healing callback can throw or replace a source.
        for (var entry : ready) entry.getValue().task = due.schedule(Math.addExact(gameTick, 1), entry.getKey());
        for (var entry : ready) {
            Pending pending = entry.getValue();
            RegenerationPlan plan = pending.plan;
            if (plans.get(entry.getKey()) != pending) continue;
            if (plan.expiresAtTick().isPresent() && gameTick >= plan.expiresAtTick().getAsLong()) {
                remove(plan.target(), plan.source().id()); continue;
            }
            var nativeState = conditions.read(plan.target());
            Long damage = lastDamage.get(plan.target());
            var state = nativeState;
            boolean inCombat = damage != null && (gameTick < damage || gameTick - damage < COMBAT_WINDOW_TICKS);
            if (!state.alive()
                    || plan.definition().respectsNaturalRegenerationRule() && !state.naturalRegeneration()
                    || plan.definition().requiresOutOfCombat() && inCombat) {
                pending.reset();
                continue;
            }
            if (damage != null) {
                long damageTick = damage;
                if (gameTick < damageTick || gameTick - damageTick < plan.definition().damageDelayTicks()) {
                    pending.reset();
                    continue;
                }
            }
            // Aggregate 20 eligible ticks so fractional HP/s is not truncated every tick at a float health write.
            pending.accrued = io.github.naimjeg.combatnexus.formula.Amounts.nonNegative(
                    pending.accrued + plan.definition().healthPerSecond() / 20.0, "accrued recovery");
            if (++pending.ticks == 20) {
                double amount = pending.accrued;
                pending.reset(); // Consume before callback; cancellation/full health never accumulates a retry burst.
                if (amount > 0) healing.submit(new HealingRequest(UUID.randomUUID(), plan.target(),
                        plan.source(), HealingRequest.Reason.NATURAL_REGENERATION, amount));
            }
        }
    }

    public record SavedRecovery(RegenerationPlan plan, double accrued, int ticks, Long damageAge) {
        public SavedRecovery {
            Objects.requireNonNull(plan);
            if (ticks < 0 || ticks >= 20 || !Double.isFinite(accrued) || accrued < 0 || damageAge != null && damageAge < 0)
                throw new IllegalArgumentException("Invalid saved recovery progress");
        }
    }
    public List<SavedRecovery> save(UUID target, Set<Identifier> sources, long now) {
        requireOpen();
        return plans.entrySet().stream().filter(e -> e.getKey().target().equals(target) && sources.contains(e.getKey().source()))
                .map(e -> new SavedRecovery(e.getValue().plan, e.getValue().accrued, e.getValue().ticks,
                        lastDamage.containsKey(target) ? Math.max(0, now - lastDamage.get(target)) : null)).toList();
    }
    public void restore(UUID target, List<SavedRecovery> saved, long savedTick, long now) {
        requireOpen();
        for (var value : saved) {
            var plan = value.plan();
            long remaining = plan.expiresAtTick().isPresent() ? plan.expiresAtTick().getAsLong() - savedTick : -1;
            if (plan.expiresAtTick().isPresent() && remaining <= 0) continue;
            var source = new io.github.naimjeg.combatnexus.combat.api.CombatSource(plan.source().id(), plan.source().owner(), Optional.empty());
            var restored = new RegenerationPlan(target, source, plan.definition(), remaining < 0 ? OptionalLong.empty() : OptionalLong.of(Math.addExact(now, remaining)));
            replace(restored);
            var pending = plans.get(new Key(target, source.id())); pending.accrued = value.accrued(); pending.ticks = value.ticks();
            if (value.damageAge() != null) lastDamage.put(target, now - value.damageAge());
        }
    }
    @Override public void remove(UUID target, Identifier source) {
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase(io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.REGENERATION);
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase(); requireOpen(); var previous = plans.remove(new Key(target, source)); if (previous != null) due.cancel(previous.task); }
    @Override public void clear(UUID target) {
        io.github.naimjeg.combatnexus.combat.internal.transaction.WorldLifecycleScope.requireIdleOrLifecycle(); requireOpen(); plans.entrySet().removeIf(entry -> { if (!entry.getKey().target().equals(target)) return false; due.cancel(entry.getValue().task); return true; }); lastDamage.remove(target); }
    @Override public void reset() {
        io.github.naimjeg.combatnexus.combat.internal.transaction.WorldLifecycleScope.requireIdleOrLifecycle(); requireOpen(); plans.clear(); due.clear(); lastDamage.clear(); lastTick = Long.MIN_VALUE; }
}
