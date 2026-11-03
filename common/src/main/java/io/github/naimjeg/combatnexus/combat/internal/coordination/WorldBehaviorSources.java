package io.github.naimjeg.combatnexus.combat.internal.coordination;
import io.github.naimjeg.combatnexus.effect.spi.EffectContributionPort;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.behavior.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard;
import io.github.naimjeg.combatnexus.effect.api.*;
import java.util.*;
import net.minecraft.resources.Identifier;

/** Composes behavior writes with the existing native receipt and compensation protocol. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class WorldBehaviorSources implements BehaviorSources {
    private record Key(UUID target, Identifier effect, Identifier source) {}
    private final CombatRuntime world;
    private final Map<UUID, Map<Key, BehaviorSource>> active = new LinkedHashMap<>();
    private Map<Key, BehaviorSource> group(UUID target) { return active.getOrDefault(target, Map.of()); }
    private final ProtectionExecutor protection = new ProtectionExecutor();
    private final PeriodicBehaviorExecutor periodic;
    private final CombatReactions reactions;
    private final Set<Key> blocked = new HashSet<>();
    private long sequence;
    private long lastTick = -1;
    private boolean closed;
    public WorldBehaviorSources(CombatRuntime world) { this(world, new CombatReactions(world)); }
    public WorldBehaviorSources(CombatRuntime world, CombatReactions reactions) {
        this.world = world; this.reactions = reactions; periodic = new PeriodicBehaviorExecutor(world);
    }
    /** Return a separate read facade, never the installation executor. */
    public BehaviorSources readOnly() {
        return new BehaviorSources() {
            public List<BehaviorSource> capture(UUID target) { return WorldBehaviorSources.this.capture(target); }
            public List<BehaviorView> view(UUID target) { return WorldBehaviorSources.this.view(target); }
            public boolean valid(BehaviorSource source) { return WorldBehaviorSources.this.valid(source); }
        };
    }
    private void check() { if (closed) throw new IllegalStateException("Behavior session closed"); world.level(); }
    private static Key key(EffectInstance e) { var s = e.snapshot(); return new Key(s.target(), s.effect(), s.source()); }
    private static Key key(BehaviorSource s) { return new Key(s.target(), s.effect(), s.source()); }
    @Override public List<BehaviorSource> capture(UUID target) {
        check(); return group(target).values().stream().filter(s -> !blocked.contains(key(s))).sorted(
                Comparator.comparingInt((BehaviorSource s) -> s.plan().priority()).reversed()
                .thenComparing(s -> s.source().toString()).thenComparingLong(BehaviorSource::installation)).toList();
    }
    @Override public List<BehaviorView> view(UUID target) { return view(target, count -> {}); }
    public List<BehaviorView> view(UUID target, java.util.function.IntConsumer budget) {
        check(); var result = new ArrayList<BehaviorView>();
        for (var source : group(target).values()) if (!blocked.contains(key(source))) {
            budget.accept(1); result.add(new BehaviorView(source, protectionAvailable(source)));
        }
        result.sort(Comparator.comparingInt((BehaviorView v) -> v.source().plan().priority()).reversed()
            .thenComparing(v -> v.source().source().toString()).thenComparingLong(v -> v.source().installation()));
        return List.copyOf(result);
    }
    @Override public boolean valid(BehaviorSource source) {
        check(); return !blocked.contains(key(source)) && source.session().equals(world.sessionId()) && source.equals(group(source.target()).get(key(source)))
                && (source.expiresAtTick().isEmpty() || source.expiresAtTick().getAsLong() > world.level().getGameTime());
    }
    public io.github.naimjeg.combatnexus.combat.spi.CombatBehaviorAccess.ProtectionDecision protect(
            UUID target, List<BehaviorSource> captured, float before, float proposed, io.github.naimjeg.combatnexus.combat.api.behavior.ReactionInputs inputs, java.util.Optional<io.github.naimjeg.combatnexus.combat.api.CombatSource> origin) {
        check(); return protection.decide(world, this, target, captured, before, proposed, inputs, origin);
    }
    private boolean protectionAvailable(BehaviorSource source) { return valid(source) && protection.available(source); }
    public EffectContributionPort compose(EffectContributionPort attributes) {
        return new EffectContributionPort() {
          public void committed(EffectService.Change change) {
            attributes.committed(change);
            var before = change.before().stream().findFirst();
            var after = change.after().stream().findFirst();
            var identity = after.or(() -> before).orElseThrow();
            var oldState = before.map(WorldBehaviorSources.this::momentState);
            var newState = after.map(WorldBehaviorSources.this::momentState);
            var kind = before.isEmpty() ? io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.EffectChange.ADDED
                : after.isEmpty() ? io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.EffectChange.REMOVED
                : after.get().snapshot().level() > before.get().snapshot().level() ? io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.EffectChange.UPGRADED
                : after.get().snapshot().level() == before.get().snapshot().level() ? io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.EffectChange.REFRESHED
                : io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.EffectChange.CHANGED;
            io.github.naimjeg.combatnexus.combat.spi.CombatMomentAccess.publish(world.level(), new io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.Effect(
                identity.snapshot().target(), identity.snapshot().effect(), oldState, newState, kind, Optional.ofNullable(identity.display().owner()), Optional.of(change)));
            if (world.level().getEntity(identity.snapshot().target()) instanceof net.minecraft.world.entity.LivingEntity entity) {
                if (after.isEmpty()) before.ifPresent(effect -> EffectTriggers.dispatch(entity, effect, EffectCapability.Trigger.END, null, 0));
                else after.ifPresent(effect -> EffectTriggers.dispatch(entity, effect, EffectCapability.Trigger.APPLY, null, 0));
            }
          }
          public boolean mayExpire(EffectInstance effect) { return attributes.mayExpire(effect); }
          public void activated(EffectInstance effect) {
            var source = group(effect.snapshot().target()).get(key(effect));
            if (source != null)
                periodic.tick(world.level().getGameTime(), WorldBehaviorSources.this::valid);
          }
          public CommandResult<Boolean> instant(EffectInstance effect) {
            check(); CombatMutationGuard.requireWorldWritePhase();
            var s = effect.snapshot();
            return periodic.instant(new BehaviorSource(world.sessionId(), s.target(), s.effect(), s.source(),
                ++sequence, OptionalLong.empty(), effect.behavior(), Optional.ofNullable(effect.display().owner())
                    .or(() -> world.sourceInstallations().identity(s.target(), s.source()).flatMap(origin -> origin.owner()))));
          }
          public Prepared prepare(EffectService.Change change) {
            check(); CombatMutationGuard.requireWorldWritePhase();
            long preparedAt = world.level().getGameTime();
            var nativePrepared = attributes.prepare(change);
            Map<Key, BehaviorSource> before = new LinkedHashMap<>(), after = new LinkedHashMap<>();
            for (var e : change.before()) { var k = key(e); before.put(k, group(k.target()).get(k)); after.put(k, null); }
            for (var e : change.after()) {
                var k = key(e); before.putIfAbsent(k, group(k.target()).get(k));
                var s = e.snapshot();
                var old = before.get(k);
                var owner = Optional.ofNullable(e.display().owner()).or(() -> world.sourceInstallations().identity(s.target(), s.source()).flatMap(origin -> origin.owner()));
                after.put(k, e.behavior().empty() ? null : old != null && old.plan().equals(e.behavior())
                        && old.expiresAtTick().equals(s.expiresAtTick()) && old.owner().equals(owner) ? old
                        : new BehaviorSource(world.sessionId(), s.target(), s.effect(), s.source(), ++sequence, s.expiresAtTick(), e.behavior(), owner));
            }
            var beforeConsumed = new HashMap<Long, Map<Identifier, Integer>>();
            var beforePeriodic = new HashMap<Long, Map<Identifier, Long>>();
            for (var source : before.values()) if (source != null)
                beforePeriodic.put(source.installation(), periodic.remaining(source.installation(), preparedAt));
            for (var source : before.values()) if (source != null) beforeConsumed.put(source.installation(), protection.consumed(source.installation()));
            var beforeTriggers = before.values().stream().filter(Objects::nonNull).map(s -> reactions.checkpoint(s.installation())).toList();
            var proposed = new HashMap<Key, BehaviorSource>();
            active.values().forEach(proposed::putAll);
            after.forEach((k,v) -> { if (v == null) proposed.remove(k); else proposed.put(k,v); });
            var reserved = new HashSet<>(proposed.keySet());
            reserved.addAll(blocked); // Failed installations still occupy bounded reconciliation slots.
            var counts = new HashMap<UUID, Integer>();
            for (var key : reserved) if (counts.merge(key.target(), 1, Integer::sum) > 64) throw new IllegalStateException("Behavior source capacity exceeded");
            periodic.validate(proposed.values(), world.level().getGameTime());
            return new EffectContributionPort.Prepared() {
                public EffectContributionPort.Receipt apply() {
                    check(); CombatMutationGuard.requireWorldWritePhase();
                    block(after.keySet());
                    try {
                        var receipt = nativePrepared.apply();
                        if (!receipt.completed()) return receipt;
                        var applied = write(after, receipt);
                        if (applied.completed()) for (var entry : after.entrySet()) {
                            var source = entry.getValue();
                            if (source != null && source.equals(before.get(entry.getKey())))
                                periodic.restore(source, beforePeriodic.get(source.installation()), preparedAt);
                            else if (source != null && before.get(entry.getKey()) != null)
                                periodic.refresh(source, beforePeriodic.get(before.get(entry.getKey()).installation()), preparedAt, change.preservePeriodicPhase());
                        }
                        if (applied.completed()) blocked.removeAll(after.keySet());
                        return applied;
                    } catch (RuntimeException failure) { return new EffectContributionPort.Receipt(List.of(), false, failure); }
                }
                public EffectContributionPort.Receipt reconcile() {
                    check(); CombatMutationGuard.requireWorldWritePhase();
                    block(after.keySet());
                    try {
                        var receipt = nativePrepared.reconcile();
                        if (!receipt.completed()) return receipt;
                        for (var checkpoint : beforeTriggers) reactions.restore(checkpoint);
                        var restored = write(before, receipt);
                        if (restored.completed()) for (var source : before.values()) if (source != null)
                            periodic.restore(source, beforePeriodic.get(source.installation()), preparedAt);
                        for (var source : before.values()) if (source != null && beforeConsumed.containsKey(source.installation())) protection.restore(source.installation(), beforeConsumed.get(source.installation()));
                        if (restored.completed()) blocked.removeAll(after.keySet());
                        return restored;
                    } catch (RuntimeException failure) { return new EffectContributionPort.Receipt(List.of(), false, failure); }
                }
                private EffectContributionPort.Receipt write(Map<Key, BehaviorSource> desired, EffectContributionPort.Receipt receipt) {
                    List<EffectContributionPort.Write> writes = new ArrayList<>(receipt.writes());
                    try {
                        desired.forEach((k,v) -> { if (Objects.equals(group(k.target()).get(k), v)) { if (v != null) periodic.install(v, world.level().getGameTime()); return; } remove(k); if (v != null) { active.computeIfAbsent(k.target(), ignored -> new LinkedHashMap<>()).put(k,v); periodic.install(v, world.level().getGameTime()); }
                            writes.add(new EffectContributionPort.Write("behavior/" + k.source(), v != null)); });
                        return new EffectContributionPort.Receipt(writes, true, null);
                    } catch (RuntimeException failure) { return new EffectContributionPort.Receipt(writes, false, failure); }
                }
            };
          }
        };
    }
    private io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.EffectState momentState(EffectInstance effect) {
        var state = effect.snapshot();
        return new io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.EffectState(state.expiresAtTick().isEmpty() ? -1
            : (int)Math.min(Integer.MAX_VALUE, Math.max(0, state.expiresAtTick().getAsLong() - world.level().getGameTime())), state.level());
    }
    private void block(Set<Key> keys) {
        blocked.addAll(keys);
        for (var key : keys) {
            var source = group(key.target()).get(key);
            if (source == null) continue;
            periodic.cancel(source.installation());
        }
    }
    private void remove(Key key) {
        var old = group(key.target()).get(key); if (old == null) return;
        for (var component : old.plan().components()) component.revoke(old, pool -> world.shields().remove(old.target(), pool, io.github.naimjeg.combatnexus.combat.shield.api.ShieldPoolSnapshot.RemovalReason.REMOVED));
        var group = active.get(key.target()); group.remove(key); if (group.isEmpty()) active.remove(key.target());
        periodic.cancel(old.installation());
        protection.remove(old.installation());
        reactions.removeSource(old.installation());
    }
    public void tick(long now) {
        check(); CombatMutationGuard.requireWorldWritePhase();
        if (now < lastTick) throw new IllegalArgumentException("Behavior clock moved backwards");
        if (now == lastTick) return; lastTick = now;
        for (var s : active.values().stream().flatMap(g -> g.values().stream()).toList()) if (!blocked.contains(key(s)) && s.expiresAtTick().isPresent() && s.expiresAtTick().getAsLong() <= now) remove(key(s));
        periodic.tick(now, this::valid);
    }
    public record SavedBehavior(Identifier effect, Identifier source, Map<Identifier, Identifier> shieldPools, Map<Identifier, Integer> consumed, Map<Identifier, Long> periodicRemaining, Map<String, Integer> triggers) {
        public SavedBehavior {
            Objects.requireNonNull(effect); Objects.requireNonNull(source); shieldPools = Map.copyOf(shieldPools); consumed = Map.copyOf(consumed); periodicRemaining = Map.copyOf(periodicRemaining);
            triggers = Map.copyOf(triggers);
            if (periodicRemaining.values().stream().anyMatch(v -> v < 0 || v > 1200000) || triggers.size() > 64
                    || triggers.values().stream().anyMatch(v -> v < 0 || v > 64))
                throw new IllegalArgumentException("Invalid saved behavior progress");
        }
    }
    public Set<Identifier> persistentPools(UUID target, Set<Identifier> sources) {
        check();
        return capture(target).stream().filter(s -> sources.contains(s.source())).flatMap(s -> s.plan().ownedShields(s).values().stream())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    public Map<Identifier, Identifier> restoredPools(UUID target, List<SavedBehavior> saved) {
        check();
        var result = new HashMap<Identifier, Identifier>();
        for (var value : saved) {
            var source = group(target).get(new Key(target, value.effect(), value.source()));
            if (source != null) { var current = source.plan().ownedShields(source); value.shieldPools().forEach((id, pool) -> { if (current.containsKey(id)) result.put(pool, current.get(id)); }); }
        }
        return Map.copyOf(result);
    }
    public List<SavedBehavior> save(UUID target, Set<Identifier> sources, long now) {
        check();
        return capture(target).stream().filter(s -> sources.contains(s.source())).map(s -> new SavedBehavior(s.effect(), s.source(), s.plan().ownedShields(s),
                protection.consumed(s.installation()), periodic.remaining(s.installation(), now), reactions.persistentCounts(s.installation()))).toList();
    }
    public void restore(UUID target, List<SavedBehavior> saved, long now) {
        check(); CombatMutationGuard.requireWorldWritePhase();
        for (var value : saved) {
            var source = group(target).get(new Key(target, value.effect(), value.source()));
            if (source == null) continue;
            protection.restore(source.installation(), value.consumed());
            reactions.restoreCounts(source.installation(), value.triggers());
            periodic.restore(source, value.periodicRemaining(), now);
        }
    }
    public void clear(UUID target) { check(); CombatMutationGuard.requireWorldWritePhase();
        List.copyOf(group(target).keySet()).forEach(this::remove); blocked.removeIf(k -> k.target().equals(target)); }
    public void close() { check(); CombatMutationGuard.requireWorldWritePhase(); active.clear(); blocked.clear(); periodic.clear(); protection.clear(); closed = true; }
}
