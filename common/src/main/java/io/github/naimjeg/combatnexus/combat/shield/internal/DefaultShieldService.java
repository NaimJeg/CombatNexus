package io.github.naimjeg.combatnexus.combat.shield.internal;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatListeners;
import io.github.naimjeg.combatnexus.combat.shield.spi.ShieldPoolService;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatCompletionQueue;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatPublicationGate;

import io.github.naimjeg.combatnexus.combat.shield.api.*;
import io.github.naimjeg.combatnexus.formula.Amounts;
import net.minecraft.resources.Identifier;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.naimjeg.combatnexus.combat.api.transaction.*;

/** Source pools consume explicit channel proportions without owning native absorption or events. */
public final class DefaultShieldService implements ShieldPoolServiceControl {
    public ShieldPoolService businessView() {
        return new ShieldPoolService() {
            public ShieldGrantReceipt replace(UUID target, ShieldGrant pool) { return DefaultShieldService.this.replace(target, pool); }
            public ShieldGrantReceipt replace(UUID target, ShieldGrant pool, io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent parent) { return DefaultShieldService.this.replace(target, pool, parent); }
            public Reservation reserve(UUID target, long damageId, Identifier damageType, double amount,
                        io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent parent) { return DefaultShieldService.this.reserve(target, damageId, damageType, amount, parent); }
            public Reservation reserve(UUID target, long damageId, Identifier damageType, double amount, Map<Identifier, Float> channels) {
                return DefaultShieldService.this.reserve(target, damageId, damageType, amount, channels);
            }
            public void remove(UUID target, Identifier source, ShieldPoolSnapshot.RemovalReason reason) { DefaultShieldService.this.remove(target, source, reason); }
            public List<ShieldPoolSnapshot> snapshot(UUID target) { return DefaultShieldService.this.snapshot(target); }
            public List<ShieldPoolSnapshot> snapshot(UUID target, java.util.function.IntConsumer budget) { return DefaultShieldService.this.snapshot(target, budget); }
            public Reservation reserve(UUID target, long damageId, Identifier damageType, double amount) { return DefaultShieldService.this.reserve(target, damageId, damageType, amount); }
        };
    }
    private final CombatServerRuntime server;
    private final net.minecraft.server.level.ServerLevel level;
    private final io.github.naimjeg.combatnexus.combat.internal.CombatRuntime world;
    private final CombatListeners<ShieldSettlement> listeners = new CombatListeners<>();
    private boolean closed;
    public DefaultShieldService(CombatServerRuntime server, io.github.naimjeg.combatnexus.combat.internal.CombatRuntime world) { this.server = Objects.requireNonNull(server); this.world = Objects.requireNonNull(world); this.level = world.level(); listeners.freeze(); }
    private record Key(UUID target, Identifier source) {}
    private record DamageKey(UUID target, long damageId) {}
    private record Allocation(Pool pool, double amount, Map<Identifier, Double> channels) {}
    private static final int MAX_RESERVATIONS_PER_TICK = 4096;
    private final Map<UUID, Map<Key, Pool>> pools = new LinkedHashMap<>();
    private final Set<Key> insertionOrder = new LinkedHashSet<>();
    private Map<Key, Pool> group(UUID target) { return pools.getOrDefault(target, Map.of()); }
    private final Map<DamageKey, Token> reservations = new HashMap<>();
    private final List<Change> changes = new ArrayList<>();
    private long tick;
    private boolean started;

    private final class Pool {
        final ShieldGrant grant;
        final Set<Identifier> eligibleDamageTypes;
        double balance;
        double held;
        long lastLoss;
        boolean damaged;
        Pool(ShieldGrant grant) {
            this.grant = grant; balance = grant.initialBalance();
            var registry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
            var definition = grant.definition();
            var eligible = new HashSet<Identifier>();
            for (var id : definition.damageTypes()) {
                if (registry.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, id)).isEmpty())
                    throw new IllegalArgumentException("Unknown shield damage type: " + id);
                eligible.add(id);
            }
            for (var id : definition.damageTypeTags()) {
                var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, id);
                var values = registry.get(tag).orElseThrow(() -> new IllegalArgumentException("Unknown shield damage tag: " + id));
                values.forEach(holder -> eligible.add(holder.unwrapKey().orElseThrow().identifier()));
            }
            eligibleDamageTypes = Set.copyOf(eligible);
        }
        ShieldPoolSnapshot snapshot() {
            return new ShieldPoolSnapshot(grant.source(), grant.definition(), balance, grant.expiresAtTick(), eligibleDamageTypes);
        }
    }

    public record SavedShield(ShieldGrant grant, double balance, long damageAge, boolean damaged) {
        public SavedShield {
            Objects.requireNonNull(grant); Amounts.nonNegative(balance, "saved balance");
            if (balance > grant.definition().capacity() || damageAge < 0) throw new IllegalArgumentException("Invalid saved shield state");
        }
    }
    public List<SavedShield> save(UUID target, Set<Identifier> sources) {
        requireOpen(); requireNoPending(target);
        return ordered(target).stream().filter(p -> sources.contains(p.grant.source().id()))
                .map(p -> new SavedShield(p.grant, p.balance, Math.max(0, tick - p.lastLoss), p.damaged)).toList();
    }
    public void restore(UUID target, List<SavedShield> saved, long savedTick, long now) {
        requireStarted();
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        for (var value : saved) {
            var grant = value.grant();
            long remaining = grant.expiresAtTick().isPresent() ? grant.expiresAtTick().getAsLong() - savedTick : -1;
            if (grant.expiresAtTick().isPresent() && remaining <= 0) continue;
            var source = new io.github.naimjeg.combatnexus.combat.api.CombatSource(grant.source().id(), grant.source().owner(), Optional.empty());
            var pool = new Pool(new ShieldGrant(source, grant.definition(), value.balance(), remaining < 0 ? OptionalLong.empty() : OptionalLong.of(Math.addExact(now, remaining))));
            pool.damaged = value.damaged(); pool.lastLoss = now - value.damageAge();
            var key = new Key(target, source.id());
            pools.computeIfAbsent(target, ignored -> new LinkedHashMap<>()).put(key, pool); insertionOrder.add(key);
        }
    }
    @Override public List<ShieldPoolSnapshot> snapshot(UUID target) {
        requireOpen();
        return snapshot(target, count -> {});
    }
    @Override public List<ShieldPoolSnapshot> snapshot(UUID target, java.util.function.IntConsumer budget) {
        requireOpen();
        var result = new ArrayList<ShieldPoolSnapshot>();
        for (var entry : group(target).entrySet()) {
            budget.accept(1); result.add(entry.getValue().snapshot());
        }
        result.sort(Comparator.comparingInt((ShieldPoolSnapshot value) -> value.definition().priority()).reversed()
                .thenComparing(value -> value.source().id().toString()));
        return List.copyOf(result);
    }

    private List<Pool> ordered(UUID target) {
        Objects.requireNonNull(target, "target");
        return group(target).entrySet().stream()
                .map(Map.Entry::getValue).sorted(Comparator
                        .comparingInt((Pool p) -> p.grant.definition().priority()).reversed()
                        .thenComparing(p -> p.grant.source().id().toString())).toList();
    }

    @Override public ShieldGrantReceipt replace(UUID target, ShieldGrant grant) { return replace(target, grant, null); }
    @Override public ShieldGrantReceipt replace(UUID target, ShieldGrant grant, CombatParent parent) {
        requireStarted();
        Objects.requireNonNull(grant, "grant");
        if (!(level.getEntity(Objects.requireNonNull(target, "target")) instanceof net.minecraft.world.entity.LivingEntity living) || !living.isAlive())
            throw new IllegalArgumentException("Shield grant requires a live target");
        var definition = grant.definition();
        var captured = world.behaviors().capture(target);
        requireNoPending(target);
        if (grant.expiresAtTick().isPresent() && grant.expiresAtTick().getAsLong() <= tick) {
            throw new IllegalArgumentException("Cannot install an expired shield");
        }
        Key key = new Key(Objects.requireNonNull(target, "target"), grant.source().id());
        Pool previous = group(target).get(key);
        Pool replacement = new Pool(grant);
        if (previous != null && definition.stacking() == ShieldDefinition.Stacking.REFRESH_SAME_SOURCE) {
            replacement.balance = Math.min(previous.balance, definition.capacity());
            replacement.lastLoss = previous.lastLoss;
            replacement.damaged = previous.damaged;
        }
        if (io.github.naimjeg.combatnexus.combat.internal.transaction.StateCommands.isApplying()) {
            var active = CombatExecutionScope.currentLineage(server.server()).orElseThrow();
            if (active.operation() != CombatOperation.SOURCE && active.operation() != CombatOperation.SHIELD)
                throw new IllegalStateException("State command cannot grant shields");
            server.ticket(active).bindSession(level, target);
            pools.computeIfAbsent(target, ignored -> new LinkedHashMap<>()).put(key, replacement); insertionOrder.add(key);
            return new ShieldGrantReceipt(active.id(), Math.max(0, replacement.balance - (previous == null ? 0 : previous.balance)));
        }
        CombatLineage lineage = server.requestLineage(CombatOperation.SHIELD, parent);
        if (!server.admit(lineage).admitted()) throw new IllegalStateException("Shield grant budget exhausted");
        server.ticket(lineage).bindSession(level, target);
        try (var completion = server.reserveCompletion(lineage)) {
        try (var applying = server.ticket(lineage).enter(CombatExecutionScope.Phase.APPLICATION)) {
            pools.computeIfAbsent(target, ignored -> new LinkedHashMap<>()).put(key, replacement);
            insertionOrder.add(key);
        }
        double granted = Math.max(0, replacement.balance - (previous == null ? 0 : previous.balance));
        publish(completion, lineage, new ShieldSettlement(target, ShieldSettlement.Kind.GRANTED, granted, OptionalLong.empty()), captured, new io.github.naimjeg.combatnexus.combat.api.ApplicationJournal(List.of(),true));
        return new ShieldGrantReceipt(lineage.id(), granted);
        } finally { server.finish(lineage); }
    }

    @Override public void remove(UUID target, Identifier source, ShieldPoolSnapshot.RemovalReason reason) {
        requireOpen();
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase(CombatOperation.SHIELD);
        Objects.requireNonNull(reason, "reason");
        if (reason == ShieldPoolSnapshot.RemovalReason.BROKEN || reason == ShieldPoolSnapshot.RemovalReason.EXPIRED) {
            throw new IllegalArgumentException("Consumption and expiration are service-owned reasons");
        }
        requireNoPending(target);
        var group = pools.get(target);
        Pool removed = group == null ? null : group.remove(new Key(target, source));
        insertionOrder.remove(new Key(target, source));
        if (group != null && group.isEmpty()) pools.remove(target);
        if (removed != null) changes.add(new Change(target, removed.snapshot(), reason));
    }

    @Override public Reservation reserve(UUID target, long damageId, Identifier damageType, double amount) {
        return reserve(target, damageId, damageType, amount, (CombatParent) null);
    }
    @Override public Reservation reserve(UUID target, long damageId, Identifier damageType, double amount, CombatParent parent) {
        return reserve(target, damageId, damageType, amount, Map.of(), parent);
    }
    @Override public Reservation reserve(UUID target, long damageId, Identifier damageType, double amount, Map<Identifier, Float> channels) {
        return reserve(target, damageId, damageType, amount, channels, null);
    }
    private Reservation reserve(UUID target, long damageId, Identifier damageType, double amount, Map<Identifier, Float> channels, CombatParent parent) {
        requireStarted();
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(damageType, "damageType");
        Amounts.nonNegative(amount, "amount");
        var channelRemainder = new LinkedHashMap<Identifier, Double>();
        double total = 0;
        for (var entry : channels.entrySet()) {
            Objects.requireNonNull(entry.getKey()); Amounts.nonNegative(entry.getValue(), "channel damage");
            total += entry.getValue();
        }
        if (total > 0) {
            for (var entry : channels.entrySet()) channelRemainder.put(entry.getKey(), amount * entry.getValue() / total);
        } else channelRemainder.put(Identifier.fromNamespaceAndPath("combatnexus", "untyped"), amount);
        DamageKey key = new DamageKey(target, damageId);
        if (reservations.containsKey(key)) throw new IllegalStateException("Damage already reserved this tick");
        if (!CombatExecutionScope.isActive() && reservations.size() >= MAX_RESERVATIONS_PER_TICK) throw new IllegalStateException("Reservation budget exceeded");
        CombatLineage lineage = CombatExecutionScope.isActive()
                ? server.enlistedLineage(CombatOperation.SHIELD, damageId)
                : server.requestLineage(CombatOperation.SHIELD, parent);
        if (!CombatExecutionScope.isActive() && !server.admit(lineage).admitted()) throw new IllegalStateException("Shield consumption budget exhausted");
        server.ticket(lineage).bindSession(level, target);
        List<Allocation> allocations = new ArrayList<>();
        double remaining = amount;
        for (Pool pool : ordered(target)) {
            if (remaining == 0) break;
            if (!pool.grant.definition().unrestricted() && !pool.eligibleDamageTypes.contains(damageType)) continue;
            var selected = pool.grant.definition().channels();
            double eligible = channelRemainder.entrySet().stream()
                    .filter(e -> selected.isEmpty() || selected.contains(e.getKey())).mapToDouble(Map.Entry::getValue).sum();
            double take = Math.min(remaining, Math.min(eligible, Math.max(0, pool.balance - pool.held)));
            if (take == 0) continue;
            pool.held += take;
            var absorbedChannels = new LinkedHashMap<Identifier, Double>();
            for (var entry : channelRemainder.entrySet()) {
                if (!selected.isEmpty() && !selected.contains(entry.getKey())) continue;
                double absorbed = take * (entry.getValue() / eligible);
                absorbedChannels.put(entry.getKey(), absorbed);
                entry.setValue(Math.max(0, entry.getValue() - absorbed));
            }
            allocations.add(new Allocation(pool, take, Map.copyOf(absorbedChannels)));
            remaining = Math.max(0, remaining - take);
        }
        Token token = new Token(target, damageId, lineage, allocations, amount - remaining, remaining);
        reservations.put(key, token);
        return token;
    }

    private final class Token implements Reservation {
        private final CombatLineage lineage;
        private final long damageId;
        private final UUID target;
        private final List<Allocation> allocations;
        private final List<io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorSource> captured;
        private final double absorbed;
        private final double remaining;
        private boolean pending = true;
        private boolean committed;
        private ShieldCommitReceipt receipt;
        @Override public Optional<ShieldCommitReceipt> receipt() { return Optional.ofNullable(receipt); }
        Token(UUID target, long damageId, CombatLineage lineage, List<Allocation> allocations, double absorbed, double remaining) {
            this.damageId = damageId;
            this.lineage = lineage;
            this.target = target;
            this.allocations = List.copyOf(allocations);
            this.captured = world.behaviors().capture(target);
            this.absorbed = absorbed;
            this.remaining = remaining;
        }
        @Override public double absorbed() { return absorbed; }
        @Override public double remainingDamage() { return remaining; }
        @Override public ShieldCommitReceipt commit() {
            requireStarted();
            if (committed) return receipt;
            if (!pending) throw new IllegalStateException("Reservation released or expired");
            try (var completion = server.reserveCompletion(lineage)) {
            var writes = new ArrayList<io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Entry>();
            pending = false;
            committed = true;
            try (var applying = server.ticket(lineage).enter(CombatExecutionScope.Phase.APPLICATION)) {
                for (Allocation allocation : allocations) {
                    Pool pool = allocation.pool();
                    double before = pool.balance;
                    pool.held = Math.max(0, pool.held - allocation.amount());
                    pool.balance = Math.max(0, before - allocation.amount());
                    writes.add(io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Entry.write(
                            io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Resource.SHIELD, pool.grant.source().id(), before, pool.balance, before - pool.balance, true));
                    pool.lastLoss = tick;
                    pool.damaged = true;
                    if (before > 0 && pool.balance == 0) {
                        changes.add(new Change(target, pool.snapshot(), ShieldPoolSnapshot.RemovalReason.BROKEN));
                    }
                }
            }
            var absorbedChannels = new LinkedHashMap<Identifier, Double>();
            allocations.forEach(a -> a.channels().forEach((channel, value) -> absorbedChannels.merge(channel, value, Double::sum)));
            receipt = new ShieldCommitReceipt(target, damageId, absorbed, writes, absorbedChannels);
            if (absorbed > 0) publish(completion, lineage, new ShieldSettlement(target, ShieldSettlement.Kind.CONSUMED, absorbed, OptionalLong.of(damageId), writes.stream().filter(w->w.before().orElse(0)>0&&w.after().orElse(1)==0).flatMap(w->w.source().stream()).collect(java.util.stream.Collectors.toSet())), captured, new io.github.naimjeg.combatnexus.combat.api.ApplicationJournal(writes,true));
            return receipt;
            } finally { if (lineage.operation() == CombatOperation.SHIELD) server.finish(lineage); }
        }
        @Override public void close() {
            server.requireThread();
            if (!pending) return;
            pending = false;
            for (Allocation allocation : allocations) {
                Pool pool = allocation.pool();
                pool.held = Math.max(0, pool.held - allocation.amount());
            }
            if (lineage.operation() == CombatOperation.SHIELD) server.finish(lineage);
        }
    }

    @Override public void tick(long gameTick) {
        io.github.naimjeg.combatnexus.combat.internal.transaction.WorldLifecycleScope.requireIdleOrLifecycle();
        requireOpen();
        if (gameTick < 0 || started && gameTick < tick) throw new IllegalArgumentException("Invalid world clock");
        if (started && gameTick == tick) return;
        reservations.values().forEach(Token::close);
        reservations.clear();
        boolean recover = started;
        started = true;
        tick = gameTick;
        var iterator = insertionOrder.iterator();
        while (iterator.hasNext()) {
            var key = iterator.next();
            var group = pools.get(key.target());
            Pool pool = group.get(key);
            var grant = pool.grant;
            var definition = grant.definition();
            if (grant.expiresAtTick().isPresent() && grant.expiresAtTick().getAsLong() <= tick) {
                changes.add(new Change(key.target(), pool.snapshot(), ShieldPoolSnapshot.RemovalReason.EXPIRED));
                group.remove(key);
                if (group.isEmpty()) pools.remove(key.target());
                iterator.remove();
            } else if (recover && (!pool.damaged || tick - pool.lastLoss >= definition.recoveryDelayTicks())) {
                // One game-tick installment only; missing/offline time is never compensated.
                double recovery = Math.min(definition.capacity() - pool.balance, definition.recoveryPerSecond() / 20.0);
                pool.balance += recovery;
            }
        }
    }

    private void requireOpen() {
        server.requireThread();
        if (closed || server.world(level).orElse(null) != world) throw new IllegalStateException("Shield world session closed");
    }
    private void publish(CombatCompletionQueue.Slot completion, CombatLineage lineage, ShieldSettlement settlement, List<io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorSource> captured, io.github.naimjeg.combatnexus.combat.api.ApplicationJournal journal) {
        CombatPublicationGate gate = new CombatPublicationGate();
        completion.publish(() -> {
            if (gate.claim()) {
                var identity=new io.github.naimjeg.combatnexus.combat.api.CommandContext.Identity(lineage.id(),lineage.rootId(),lineage.parentId(),lineage.depth());
                io.github.naimjeg.combatnexus.combat.spi.CombatBehaviorAccess.completed(world,lineage,new io.github.naimjeg.combatnexus.combat.api.behavior.SettlementEnvelope(CombatOperation.SHIELD,identity,settlement.target(),Optional.empty(),journal,settlement),captured);
                io.github.naimjeg.combatnexus.combat.internal.registration.CombatExtensions.shieldCompleted(server.server(), settlement);
                listeners.dispatch(server.server(), lineage, settlement, settlement.amount() > 0 ? EnumSet.allOf(CombatOperation.class) : Set.of());
            }
        });
        server.drain();
    }
    @Override public void close() { requireOpen(); reset(); listeners.clear(); closed = true; }
    private void requireStarted() {
        requireOpen();
        if (!started) throw new IllegalStateException("Initialize world tick before using shields");
    }
    private void requireNoPending(UUID target) {
        if (reservations.entrySet().stream().anyMatch(e -> e.getKey().target().equals(target) && e.getValue().pending)) {
            throw new IllegalStateException("Cannot mutate pools during a damage reservation");
        }
    }
    @Override public void clear(UUID target) {
        io.github.naimjeg.combatnexus.combat.internal.transaction.WorldLifecycleScope.requireIdleOrLifecycle();
        requireOpen();
        // Keep closed damage identities until next tick, preventing same-tick replay after a clear.
        reservations.forEach((key, token) -> { if (key.target().equals(target)) token.close(); });
        var removed = pools.remove(target);
        if (removed != null) insertionOrder.removeAll(removed.keySet());
        changes.removeIf(change -> change.target().equals(target));
    }
    @Override public void reset() {
        io.github.naimjeg.combatnexus.combat.internal.transaction.WorldLifecycleScope.requireIdleOrLifecycle();
        requireOpen();
        reservations.values().forEach(Token::close);
        reservations.clear();
        pools.clear();
        insertionOrder.clear();
        changes.clear();
        started = false;
    }
    @Override public List<Change> drainChanges() {
        requireOpen();
        List<Change> result = List.copyOf(changes);
        changes.clear();
        return result;
    }
}
