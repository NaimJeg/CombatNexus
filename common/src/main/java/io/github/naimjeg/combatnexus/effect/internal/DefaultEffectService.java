package io.github.naimjeg.combatnexus.effect.internal;
import io.github.naimjeg.combatnexus.effect.spi.EffectContributionPort;

import io.github.naimjeg.combatnexus.effect.api.*;
import net.minecraft.resources.Identifier;
import java.util.*;
import java.util.function.Predicate;

/** Source identity is (target, effect, source); removing one source never removes another's control. */
public final class DefaultEffectService implements EffectServiceControl {
    public EffectService businessView() {
        return new EffectService() {
            public boolean bind(EffectBinding binding) { return DefaultEffectService.this.bind(binding); }
            public boolean apply(EffectInstance effect, Identifier stacking) { return DefaultEffectService.this.apply(effect, stacking); }
            public io.github.naimjeg.combatnexus.combat.api.CommandResult<Boolean> applyInstant(EffectInstance effect) { return DefaultEffectService.this.applyInstant(effect); }
            public void remove(UUID target, Identifier effect, Identifier source, Removal reason) { DefaultEffectService.this.remove(target, effect, source, reason); }
            public List<EffectInstance> snapshot(UUID target) { return DefaultEffectService.this.snapshot(target); }
            public List<EffectInstance> snapshot(UUID target, java.util.function.IntConsumer budget) { return DefaultEffectService.this.snapshot(target, budget); }
            public Set<EffectSnapshot.RestrictedAction> restrictions(UUID target) { return DefaultEffectService.this.restrictions(target); }
            public Set<Identifier> pendingSources(UUID target) { return DefaultEffectService.this.pendingSources(target); }
        };
    }
    private record Key(UUID target, Identifier effect, Identifier source) {}
    private final Map<UUID, Map<Key, EffectInstance>> active = new LinkedHashMap<>();
    private final Set<Key> insertionOrder = new LinkedHashSet<>();
    private Map<Key, EffectInstance> group(UUID target) { return active.getOrDefault(target, Map.of()); }
    private List<EffectInstance> allActive() { return insertionOrder.stream().map(k -> group(k.target()).get(k)).toList(); }
    private final List<Change> changes = new ArrayList<>();
    private final Predicate<EffectInstance> immunity;
    private long tick = -1;
    private final Thread owner = Thread.currentThread();
    private boolean closed;
    private boolean lifecycle;
    private boolean bindingUpdate;
    @Override public boolean bind(EffectBinding binding) {
        requireOpen(); requireGameplayWrite();
        var s = binding.effect().snapshot();
        if (!binding.satisfied()) { remove(s.target(), s.effect(), s.source(), Removal.CONDITION_ENDED); return true; }
        boolean previous = bindingUpdate; bindingUpdate = true;
        try {
            boolean applied = apply(binding.effect(), binding.stacking());
            if (!applied) remove(s.target(), s.effect(), s.source(), Removal.CONDITION_ENDED);
            return applied;
        } finally { bindingUpdate = previous; }
    }
    private void requireGameplayWrite() {
        if (!lifecycle) io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase(io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.EFFECT);
        if (!lifecycle && io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.isExecuting()
                && !io.github.naimjeg.combatnexus.combat.internal.transaction.StateCommands.isApplying())
            throw new IllegalStateException("Effect gameplay writes require a non-computing world phase");
    }
    private final EffectContributionPort executor;
    private void requireOpen() {
        if (closed || Thread.currentThread() != owner) throw new IllegalStateException("Effect session closed or wrong thread");
    }
    @Override public void close() { requireOpen(); reset(); closed = true; }

    public DefaultEffectService(Predicate<EffectInstance> immunity) {
        this(immunity, change -> {
            if (java.util.stream.Stream.concat(change.before().stream(), change.after().stream())
                    .anyMatch(value -> value.ownership() == EffectInstance.Ownership.MANAGED))
                throw new IllegalStateException("Managed effects require a contribution port");
            return new EffectContributionPort.Prepared() {
                public EffectContributionPort.Receipt apply() { return new EffectContributionPort.Receipt(List.of(), true, null); }
                public EffectContributionPort.Receipt reconcile() { return apply(); }
            };
        });
    }
    public DefaultEffectService(Predicate<EffectInstance> immunity, EffectContributionPort executor) {
        this.immunity = Objects.requireNonNull(immunity, "immunity");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    private final Map<Key, EffectContributionPort.Prepared> pending = new LinkedHashMap<>();
    private static Key key(EffectInstance effect) {
        var value = effect.snapshot(); return new Key(value.target(), value.effect(), effect.display().aggregateSources()
                ? Identifier.fromNamespaceAndPath("combatnexus", "aggregate") : value.source());
    }
    private void reconcile(Set<Key> keys, boolean required) {
        for (var key : keys) {
            var prepared = pending.get(key);
            if (prepared == null) continue;
            try {
                var receipt = prepared.reconcile();
                if (receipt.completed()) pending.remove(key);

            } catch (RuntimeException failure) {
                if (required) throw new IllegalStateException("Effect source reconciliation failed", failure);
            }
        }
        if (required && keys.stream().anyMatch(pending::containsKey))
            throw new IllegalStateException("Effect source awaits reconciliation");
    }
    private void commit(Change change) {
        Set<Key> keys = new LinkedHashSet<>();
        change.before().forEach(value -> keys.add(key(value)));
        change.after().forEach(value -> keys.add(key(value)));
        reconcile(keys, true);
        var prepared = executor.prepare(change);
        var receipt = prepared.apply();
        if (!receipt.completed()) {
            keys.forEach(key -> pending.put(key, prepared));
            reconcile(keys, false);
            throw new IllegalStateException("Effect contribution application incomplete", receipt.failure());
        }
    }
    @Override public Set<Identifier> pendingSources(UUID target) {
        requireOpen();
        return pending.keySet().stream().filter(key -> key.target().equals(target))
                .map(Key::source).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override public boolean apply(EffectInstance effect, Identifier stacking) {
        requireOpen();
        requireGameplayWrite();
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(stacking, "stacking");
        var adoptedStacking = lifecycle || bindingUpdate && stacking.equals(Stacking.REPLACE.id()) ? effect.stacking() : stacking;
        effect = effect.withStacking(adoptedStacking);
        if (tick < 0) throw new IllegalStateException("Initialize world tick before applying effects");
        var snapshot = effect.snapshot();
        if (expired(snapshot)) return false;
        try (var compute = io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope.isActive()
                ? io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope.computation() : null) {
            if (immunity.test(effect)) return false;
        }
        if (snapshot.lifetime() == EffectLifetime.Kind.INSTANT) return executor.instant(effect).applied();
        Key key = key(effect);
        reconcile(Set.of(key), true);
        EffectInstance previous = group(key.target()).get(key);
        if (previous != null && previous.ownership() != effect.ownership()) {
            throw new IllegalStateException("Revoke old execution owner before replacing it");
        }
        var strategy = EffectStackingStrategies.get(stacking);
        if (previous != null) {
            effect = Objects.requireNonNull(strategy.resolve(previous, effect, tick));
            if (!key(effect).equals(key)) throw new IllegalArgumentException("Stacking changed installation identity");
        }
        effect = effect.withStacking(adoptedStacking);
        if (effect.equals(previous)) return true;
        Change change = new Change(previous == null ? List.of() : List.of(previous), List.of(effect), Optional.empty(), bindingUpdate);
        commit(change);
        active.computeIfAbsent(key.target(), ignored -> new LinkedHashMap<>()).put(key, effect);
        insertionOrder.add(key);
        changes.add(change);
        executor.committed(change);
        executor.activated(effect);
        return true;
    }

    @Override public io.github.naimjeg.combatnexus.combat.api.CommandResult<Boolean> applyInstant(EffectInstance effect) {
        requireOpen(); requireGameplayWrite();
        if (tick < 0) throw new IllegalStateException("Initialize world tick before applying effects");
        if (effect.snapshot().lifetime() != EffectLifetime.Kind.INSTANT)
            throw new IllegalArgumentException("Expected an instant effect");
        try (var compute = io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope.isActive()
                ? io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope.computation() : null) {
            if (immunity.test(effect)) return io.github.naimjeg.combatnexus.combat.api.CommandResult.rejected(
                    io.github.naimjeg.combatnexus.combat.api.CommandRejection.Code.IMMUNE, "Effect immunity rejected application");
        }
        return executor.instant(effect);
    }

    @Override public void remove(UUID target, Identifier effect, Identifier source, Removal reason) {
        requireOpen();
        requireGameplayWrite();
        Objects.requireNonNull(reason, "reason");
        Key key = new Key(target, effect, source);
        var aggregate = new Key(target, effect, Identifier.fromNamespaceAndPath("combatnexus", "aggregate"));
        var grouped = group(target).get(aggregate);
        if (grouped != null && (grouped.snapshot().source().equals(source)
                || grouped.hidden().stream().anyMatch(layer -> layer.snapshot().source().equals(source)))) key = aggregate;
        reconcile(Set.of(key), true);
        EffectInstance previous = group(key.target()).get(key);
        if (previous != null) {
            if (previous.display().aggregateSources() && reason == Removal.REMOVED) {
                var remaining = EffectLayers.withoutSource(previous, source, tick);
                if (remaining.isPresent()) {
                    boolean oldBinding = bindingUpdate; bindingUpdate = true;
                    try { apply(remaining.get(), Stacking.REPLACE); }
                    finally { bindingUpdate = oldBinding; }
                    return;
                }
            }
            if (reason == Removal.DISPELLED && !previous.policy().dispellable())
                throw new IllegalArgumentException("Effect is not dispellable");
            Change change = new Change(List.of(previous), List.of(), Optional.of(reason));
            commit(change);
            var group = active.get(target);
            group.remove(key);
            insertionOrder.remove(key);
            if (group.isEmpty()) active.remove(target);
            changes.add(change);
            executor.committed(change);
        }
    }

    @Override public void tick(long gameTick) {
        requireOpen();
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        if (gameTick < 0 || gameTick < tick) throw new IllegalArgumentException("Invalid world clock");
        if (gameTick == tick) return;
        reconcile(new LinkedHashSet<>(pending.keySet()), false);
        tick = gameTick;
        boolean beforeLifecycle = lifecycle;
        lifecycle = true;
        try {
        for (EffectInstance effect : allActive()) {
            var snapshot = effect.snapshot();
            if (expired(snapshot) && !pending.containsKey(key(effect))) {
                var restored = EffectLayers.active(effect, tick);
                if (restored.isPresent()) apply(restored.orElseThrow(), Stacking.REPLACE);
                else if (executor.mayExpire(effect)) remove(snapshot.target(), snapshot.effect(), snapshot.source(), Removal.EXPIRED);
            }
        }
        } finally { lifecycle = beforeLifecycle; }
    }

    private boolean expired(EffectSnapshot snapshot) {
        return snapshot.expiresAtTick().isPresent() && snapshot.expiresAtTick().getAsLong() <= tick;
    }

    @Override public List<EffectInstance> snapshot(UUID target) {
        requireOpen();
        return snapshot(target, count -> {});
    }
    @Override public List<EffectInstance> snapshot(UUID target, java.util.function.IntConsumer budget) {
        requireOpen(); List<EffectInstance> result = new ArrayList<>();
        for (var effect : group(target).values()) {
            budget.accept(1); result.add(effect);
        }
        return List.copyOf(result);
    }

    @Override public Set<EffectSnapshot.RestrictedAction> restrictions(UUID target) {
        requireOpen();
        var result = EnumSet.noneOf(EffectSnapshot.RestrictedAction.class);
        snapshot(target).forEach(effect -> result.addAll(effect.snapshot().restrictions()));
        return Set.copyOf(result);
    }

    @Override public void clear(UUID target, Removal reason) {
        requireOpen();
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        Objects.requireNonNull(reason, "reason");
        boolean beforeLifecycle = lifecycle; lifecycle = true;
        try {
            reconcile(pending.keySet().stream().filter(key -> key.target().equals(target)).collect(java.util.stream.Collectors.toSet()), true);
            for (EffectInstance effect : snapshot(target)) {
                if (reason == Removal.DISPELLED && !effect.policy().dispellable()) continue;
                var snapshot = effect.snapshot();
                remove(target, snapshot.effect(), snapshot.source(), reason);
            }
        } finally { lifecycle = beforeLifecycle; }
    }

    /** World shutdown only; consumers must revoke their managed contributions before dropping the session. */
    @Override public void reset() {
        requireOpen();
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        reconcile(Set.copyOf(pending.keySet()), true);
        boolean beforeLifecycle = lifecycle; lifecycle = true;
        try {
        for (var effect : allActive()) {
            var snapshot = effect.snapshot();
            remove(snapshot.target(), snapshot.effect(), snapshot.source(), Removal.WORLD_EXIT);
        }
        changes.clear(); tick = -1;
        } finally { lifecycle = beforeLifecycle; }
    }
    @Override public List<Change> drainChanges() {
        requireOpen();
        List<Change> result = List.copyOf(changes);
        changes.clear();
        return result;
    }
}
