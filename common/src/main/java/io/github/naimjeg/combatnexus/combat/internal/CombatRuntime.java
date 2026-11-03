package io.github.naimjeg.combatnexus.combat.internal;
import io.github.naimjeg.combatnexus.effect.spi.EffectContributionPort;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatDispatchScope;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope;

import io.github.naimjeg.combatnexus.attribute.api.AttributeSnapshot;
import io.github.naimjeg.combatnexus.attribute.api.AttributeSnapshotService;
import io.github.naimjeg.combatnexus.combat.healing.spi.HealingApplicationPort;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingService;
import io.github.naimjeg.combatnexus.combat.regeneration.spi.RegenerationConditions;
import io.github.naimjeg.combatnexus.combat.regeneration.api.RegenerationScheduler;
import io.github.naimjeg.combatnexus.combat.shield.spi.ShieldPoolService;
import io.github.naimjeg.combatnexus.effect.api.EffectInstance;
import io.github.naimjeg.combatnexus.effect.api.EffectService;
import io.github.naimjeg.combatnexus.presentation.api.CombatPanelService;
import io.github.naimjeg.combatnexus.presentation.api.CombatPanelSnapshot;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * Explicit world-session composition root. Construct on the server thread, tick from one adapter,
 * and discard on world unload. The active target owns creation and lifecycle events.
 * The effect executor applies/revokes managed contributions synchronously before values are captured.
 */
public final class CombatRuntime {
    private final io.github.naimjeg.combatnexus.combat.internal.coordination.DecisionSamples decisionSamples=new io.github.naimjeg.combatnexus.combat.internal.coordination.DecisionSamples();
    public io.github.naimjeg.combatnexus.combat.internal.coordination.DecisionSamples decisionSamples(){requireOwner();return decisionSamples;}
    private final io.github.naimjeg.combatnexus.combat.internal.coordination.BridgeExecutions bridgeExecutions=new io.github.naimjeg.combatnexus.combat.internal.coordination.BridgeExecutions(this);
    public io.github.naimjeg.combatnexus.combat.api.bridge.BridgeCalls bridges() { requireOwner(); return bridgeExecutions; }
    private final io.github.naimjeg.combatnexus.combat.internal.coordination.SourceInstallations sources = new io.github.naimjeg.combatnexus.combat.internal.coordination.SourceInstallations(this);
    public io.github.naimjeg.combatnexus.combat.internal.coordination.SourceInstallations sourceInstallations() { requireOwner(); return sources; }
    private final Thread owner = Thread.currentThread();
    private final ServerLevel level;
    private final UUID sessionId = UUID.randomUUID();
    private final io.github.naimjeg.combatnexus.combat.healing.internal.HealingServiceControl healing;
    private final HealingService healingView;
    private final io.github.naimjeg.combatnexus.combat.internal.coordination.CombatReactions reactions;
    private final io.github.naimjeg.combatnexus.combat.internal.coordination.WorldBehaviorSources behaviors;
    private final io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorSources behaviorView;
    private final io.github.naimjeg.combatnexus.combat.api.reaction.CombatReactionService reactionView;
    private final CombatServerRuntime serverRuntime;
    private final io.github.naimjeg.combatnexus.combat.shield.internal.ShieldPoolServiceControl shields;
    private final ShieldPoolService shieldsView;
    private final io.github.naimjeg.combatnexus.combat.regeneration.internal.RegenerationSchedulerControl regeneration;
    private final RegenerationScheduler regenerationView;
    private final io.github.naimjeg.combatnexus.effect.internal.EffectServiceControl effects;
    private final EffectService effectsView;
    private final AttributeSnapshotService attributes = new AttributeSnapshotService();
    private final io.github.naimjeg.combatnexus.presentation.internal.DefaultCombatPanelService panels;
    private final CombatPanelService panelView;
    private final java.util.Map<UUID, java.util.Set<net.minecraft.resources.Identifier>> persistentSources = new java.util.HashMap<>();
    public enum SourcePersistence { SESSION, PERSISTENT }
    public void sourcePersistence(UUID target, net.minecraft.resources.Identifier source, SourcePersistence policy) {
        requireOwner(); io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        Objects.requireNonNull(target); Objects.requireNonNull(source); Objects.requireNonNull(policy);
        if (!(level.getEntity(target) instanceof LivingEntity entity) || !entity.isAlive())
            throw new IllegalArgumentException("Persistence policy requires a live target in this world");
        var sources = persistentSources.computeIfAbsent(target, ignored -> new java.util.HashSet<>());
        if (policy == SourcePersistence.PERSISTENT) {
            if (sources.size() >= 64 && !sources.contains(source)) throw new IllegalStateException("Persistent source capacity exceeded");
            sources.add(source);
        } else sources.remove(source);
        if (sources.isEmpty()) persistentSources.remove(target);
    }
    public String saveLifecycle(UUID target, io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner();
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        var explicit = persistentSources.getOrDefault(target, java.util.Set.of());
        var sources = new java.util.HashSet<>(explicit); sources.addAll(equipment.sources(target)); sources.addAll(this.sources.persistentKeys(target));
        var retained = new java.util.HashSet<>(explicit);
        effects.snapshot(target).stream().filter(e -> e.display().persistent()
            && (!e.display().environment() || e.snapshot().lifetime() == io.github.naimjeg.combatnexus.effect.api.EffectLifetime.Kind.TIMED))
            .forEach(e -> retained.add(e.snapshot().source()));
        sources.addAll(retained);
        var shieldSources = new java.util.HashSet<>(sources); shieldSources.addAll(behaviors.persistentPools(target, sources));
        var state = new io.github.naimjeg.combatnexus.combat.internal.persistence.CombatPersistentState(target, level.getGameTime(), retained,
                effects.snapshot(target).stream().filter(e -> e.ownership() == EffectInstance.Ownership.MANAGED && sources.contains(e.snapshot().source())
                    && !(e.display().environment() && e.snapshot().lifetime() == io.github.naimjeg.combatnexus.effect.api.EffectLifetime.Kind.PERMANENT)).toList(),
                behaviors.save(target, sources, level.getGameTime()),
                ((io.github.naimjeg.combatnexus.combat.shield.internal.DefaultShieldService)shields).save(target, shieldSources),
                ((io.github.naimjeg.combatnexus.combat.regeneration.internal.DefaultRegenerationScheduler)regeneration).save(target, sources, level.getGameTime()), equipment.save(target), this.sources.save(target));
        return io.github.naimjeg.combatnexus.combat.internal.persistence.CombatStateCodec.encode(state);
    }
    public void restoreLifecycle(UUID target, String encoded, io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner();
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        var state = io.github.naimjeg.combatnexus.combat.internal.persistence.CombatStateCodec.decode(encoded);
        if (!state.target().equals(target)) throw new IllegalArgumentException("Persistent target mismatch");
        long now = level.getGameTime();
        var acceptedEquipment = equipment.restore(target, state.equipment());
        this.sources.restore(target, state.sources().stream().filter(s -> !s.source().provider().equals(net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus", "equipment"))
            || acceptedEquipment.contains(io.github.naimjeg.combatnexus.combat.internal.coordination.SourceInstallations.key(s.source()))).toList());
        var sources = new java.util.HashSet<>(state.persistentSources()); sources.addAll(acceptedEquipment); sources.addAll(this.sources.persistentKeys(target));
        persistentSources.put(target, new java.util.HashSet<>(state.persistentSources()));
        for (var value : state.effects()) {
            var s = value.snapshot(); if (!sources.contains(s.source())) continue;
            var restored = io.github.naimjeg.combatnexus.effect.api.EffectLayers.active(
                    io.github.naimjeg.combatnexus.effect.api.EffectLayers.rebase(value, state.savedTick(), now), now);
            restored.ifPresent(effect -> effects.apply(effect, effect.stacking()));
        }
        behaviors.restore(target, state.behaviors(), now);
        var pools = behaviors.restoredPools(target, state.behaviors());
        ((io.github.naimjeg.combatnexus.combat.shield.internal.DefaultShieldService)shields).restore(target,
                state.shields().stream().filter(s -> sources.contains(s.grant().source().id()) || pools.containsKey(s.grant().source().id()))
                    .map(s -> {
                        var grant = s.grant(); var source = grant.source();
                        var rebound = new CombatSource(pools.getOrDefault(source.id(), source.id()), source.owner(), java.util.Optional.empty());
                        return new io.github.naimjeg.combatnexus.combat.shield.internal.DefaultShieldService.SavedShield(
                            new io.github.naimjeg.combatnexus.combat.shield.api.ShieldGrant(rebound, grant.definition(), grant.initialBalance(), grant.expiresAtTick()),
                            s.balance(), s.damageAge(), s.damaged());
                    }).toList(), state.savedTick(), now);
        ((io.github.naimjeg.combatnexus.combat.regeneration.internal.DefaultRegenerationScheduler)regeneration).restore(target,
                state.recovery().stream().filter(s -> sources.contains(s.plan().source().id())).toList(), state.savedTick(), now);
        this.sources.notifyRestored(target);
    }
    private long tick = -1;
    private boolean closed;
    private final io.github.naimjeg.combatnexus.combat.internal.coordination.EquipmentBehaviorController equipment;

    public CombatRuntime(ServerLevel level, HealingApplicationPort healingPort, RegenerationConditions conditions,
                         Predicate<EffectInstance> immunity, io.github.naimjeg.combatnexus.effect.spi.EffectContributionPort effectExecutor,
                         long panelIntervalTicks, int panelMaximumEntries) {
        this.level = Objects.requireNonNull(level, "level");
        serverRuntime = CombatServerRuntime.of(level.getServer());
        serverRuntime.requireThread();
        reactions = new io.github.naimjeg.combatnexus.combat.internal.coordination.CombatReactions(this);
        equipment = new io.github.naimjeg.combatnexus.combat.internal.coordination.EquipmentBehaviorController(this);
        reactionView = reactions.readOnly();
        var shieldsImplementation = new io.github.naimjeg.combatnexus.combat.shield.internal.DefaultShieldService(serverRuntime, this);
        shields = shieldsImplementation;
        shieldsView = shieldsImplementation.businessView();
        var healingImplementation = new io.github.naimjeg.combatnexus.combat.healing.internal.DefaultHealingService(serverRuntime, this, healingPort);
        healing = healingImplementation;
        healingView = healingImplementation.businessView();
        var regenerationImplementation = new io.github.naimjeg.combatnexus.combat.regeneration.internal.DefaultRegenerationScheduler(conditions);
        regeneration = regenerationImplementation;
        regenerationView = regenerationImplementation.businessView();
        behaviors = new io.github.naimjeg.combatnexus.combat.internal.coordination.WorldBehaviorSources(this, reactions);
        behaviorView = behaviors.readOnly();
        var effectsImplementation = new io.github.naimjeg.combatnexus.effect.internal.DefaultEffectService(immunity, behaviors.compose(effectExecutor));
        effects = effectsImplementation;
        effectsView = effectsImplementation.businessView();
        panels = new io.github.naimjeg.combatnexus.presentation.internal.DefaultCombatPanelService(panelIntervalTicks, panelMaximumEntries);
        panelView = panels.businessView();
        serverRuntime.attach(level, this);
    }

    /** Expire state before processing actions; adapters apply effect changes before tickRecovery. */
    private void beginTick() {
        requireOwner();
        long now = level.getGameTime();
        if (now < tick) throw new IllegalStateException("World clock moved backwards; recreate the runtime");
        if (now == tick) return;
        serverRuntime.lifecycle(this, () -> { effects.tick(now); shields.tick(now); effects.drainChanges(); });
        tick = now;
    }

    /** Call after managed contributions and conditions have been reconciled for this tick. */
    public void advanceLifecycle(io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess.Permit permit) {
        java.util.Objects.requireNonNull(permit).require(this); beginTick();
    }
    public void tickRecoveryLifecycle(io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess.Permit permit) {
        java.util.Objects.requireNonNull(permit).require(this);
        requireTick();
        equipment.tick(serverRuntime.definitions().orElseThrow().generation());
        sources.tick();
        regeneration.tick(tick, healing);
        behaviors.tick(tick);
    }

    public AttributeSnapshot captureAttributes(LivingEntity entity, Iterable<Holder<Attribute>> requested) {
        requireEntity(entity);
        return attributes.capture(entity, requested);
    }

    public AttributeSnapshot captureAttributes(LivingEntity entity, Iterable<Holder<Attribute>> requested,
                                                java.util.function.IntConsumer budget) {
        requireEntity(entity);
        return attributes.capture(entity, requested, budget);
    }

    public io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame captureInput(LivingEntity entity,
            long generation) {
        requireEntity(entity);
        Iterable<Holder<Attribute>> requested = () -> net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE
                .listElements().<Holder<Attribute>>map(holder -> holder).iterator();
        var snapshot = captureAttributes(entity, requested);
        return new io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame(sessionId,
                level.dimension().identifier(), entity.getUUID(), snapshot.revision(), snapshot.gameTick(),
                generation, snapshot, io.github.naimjeg.combatnexus.combat.api.input.CombatEntityFacts.capture(entity), behaviors.capture(entity.getUUID()));
    }

    /** The same snapshot object can be passed to combat consumers and to this presentation projection. */
    public CombatPanelSnapshot panel(LivingEntity entity, AttributeSnapshot snapshot) {
        return panel(entity, snapshot, count -> {});
    }
    public CombatPanelSnapshot panel(LivingEntity entity, AttributeSnapshot snapshot, java.util.function.IntConsumer budget) {
        requireEntity(entity);
        if (attributes.latest(entity.getUUID()).orElse(null) != snapshot || snapshot.gameTick() != tick) {
            throw new IllegalArgumentException("Expected this runtime's current authoritative attribute snapshot");
        }
        var behaviorViews = behaviors.view(entity.getUUID(), budget);
        var reactionFacts = reactions.facts(entity.getUUID(), budget);
        var input=new io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame(sessionId,level.dimension().identifier(),entity.getUUID(),snapshot.revision(),snapshot.gameTick(),
                serverRuntime.definitions().orElseThrow().generation(),snapshot,io.github.naimjeg.combatnexus.combat.api.input.CombatEntityFacts.capture(entity),behaviors.capture(entity.getUUID()));
        var descriptions=io.github.naimjeg.combatnexus.combat.internal.registration.CombatExtensions.describe(input);budget.accept(descriptions.size());
        return new CombatPanelSnapshot(snapshot, entity.getHealth(), entity.getAbsorptionAmount(),
                shields.snapshot(entity.getUUID(), budget), io.github.naimjeg.combatnexus.effect.api.EffectFacts.capture(entity, budget).views(), behaviorViews, reactionFacts, descriptions);
    }

    /** Death, respawn, world exit and dimension transfer reset all local counters and owned state. */
    public void clearLifecycle(UUID entity, EffectService.Removal reason, io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess.Permit permit) {
        java.util.Objects.requireNonNull(permit).require(this);
        requireOwner();
        serverRuntime.lifecycle(this, () -> {
        persistentSources.remove(entity);
        effects.clear(entity, reason);
        equipment.clear(entity);
        sources.clear(entity);
        regeneration.clear(entity);
        behaviors.clear(entity);
        shields.clear(entity);
        attributes.clear(entity);
        panels.clear(entity);
        });
    }

    public ServerLevel commandLevel() { return level; }
    public boolean commandSessionClosed() { return closed; }
    public ServerLevel level() { requireOwner(); return level; }
    public io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorSources behaviors() { requireOwner(); return behaviorView; }
    public io.github.naimjeg.combatnexus.combat.api.reaction.CombatReactionService reactions() { requireOwner(); return reactionView; }
    public void dispatchSettlement(io.github.naimjeg.combatnexus.combat.api.transaction.CombatCallback<io.github.naimjeg.combatnexus.combat.api.behavior.SettlementEnvelope> callback,
            java.util.List<io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorSource> captured,
            io.github.naimjeg.combatnexus.combat.spi.CombatBehaviorAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this);requireOwner();reactions.completed(callback,captured);
    }
    public void dispatchDamageReaction(io.github.naimjeg.combatnexus.combat.damage.api.event.DamageSettlementCallback callback,
            io.github.naimjeg.combatnexus.combat.spi.CombatBehaviorAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner(); reactions.damage(callback);
    }
    public void recordRecoveryDamage(io.github.naimjeg.combatnexus.combat.damage.api.event.DamageSettlementCallback callback,
            io.github.naimjeg.combatnexus.combat.spi.CombatBehaviorAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner();
        if (!CombatDispatchScope.delivering(callback, level.getServer()) || callback.snapshot().level() != level)
            throw new IllegalStateException("Recovery delay requires this world's active damage settlement");
        var snapshot = callback.snapshot();
        if (snapshot.successfulHit())
            regeneration.recordDamage(snapshot.target().getUUID(), level.getGameTime());
    }
    public void dispatchHealingReaction(io.github.naimjeg.combatnexus.combat.api.transaction.CombatCallback<io.github.naimjeg.combatnexus.combat.healing.api.HealingSettlement> callback,
            io.github.naimjeg.combatnexus.combat.spi.CombatBehaviorAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner(); reactions.healing(callback);
    }
    public io.github.naimjeg.combatnexus.combat.spi.CombatBehaviorAccess.ProtectionDecision decideProtection(java.util.UUID target,
            java.util.List<io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorSource> captured, float before, float proposed, io.github.naimjeg.combatnexus.combat.api.behavior.ReactionInputs inputs, java.util.Optional<io.github.naimjeg.combatnexus.combat.api.CombatSource> origin,
            io.github.naimjeg.combatnexus.combat.spi.CombatBehaviorAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner(); return behaviors.protect(target, captured, before, proposed, inputs, origin);
    }
    public java.util.Map<net.minecraft.resources.Identifier, io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorDefinition> behaviorDefinitions() {
        requireOwner(); return serverRuntime.definitions().orElseThrow().behaviors();
    }
    public HealingService healing() { requireOwner(); return healingView; }
    public void equipmentChanged(UUID target, net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.world.item.ItemStack current,
            io.github.naimjeg.combatnexus.combat.spi.CombatEquipmentAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner(); equipment.changed(target, slot, current);
    }
    public void trackEquipment(UUID target, io.github.naimjeg.combatnexus.combat.spi.CombatEquipmentAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner(); equipment.track(target); sources.track(target);
    }
    public DomainDefinitions domainDefinitions() {
        requireOwner(); return serverRuntime.definitions().orElseThrow().domains();
    }
    /** Future snapshot transport includes this ID so clients reject packets from a prior world session. */
    public UUID sessionId() { requireOwner(); return sessionId; }
    public ShieldPoolService shields() { requireOwner(); return shieldsView; }
    public java.util.List<io.github.naimjeg.combatnexus.combat.regeneration.api.RegenerationPlan> regenerationPlans(UUID target) { requireOwner(); return ((io.github.naimjeg.combatnexus.combat.regeneration.internal.DefaultRegenerationScheduler)regeneration).snapshot(target); }
    public RegenerationScheduler regeneration() { requireOwner(); return regenerationView; }
    public EffectService effects() { requireOwner(); return effectsView; }
    public CombatPanelService panels() { requireOwner(); return panelView; }

    private void requireEntity(LivingEntity entity) {
        requireTick();
        if (entity.level() != level) throw new IllegalArgumentException("Entity belongs to another world session");
    }
    private void requireTick() {
        requireOwner();
        if (tick < 0 || tick != level.getGameTime()) throw new IllegalStateException("World lifecycle has not advanced to the current tick");
    }
    private void requireOwner() {
        serverRuntime.requireThread();
        if (closed || Thread.currentThread() != owner || !level.getServer().isSameThread()) {
            throw new IllegalStateException("Combat runtime requires its owning server thread and an open session");
        }
    }
    /** World unload only. Entity contributions must be revoked by the adapter before closing. */
    public void closeLifecycle(io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess.Permit permit) {
        java.util.Objects.requireNonNull(permit).require(this);
        if (closed) return;
        requireOwner();
        if (io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope.isActive()
                || io.github.naimjeg.combatnexus.combat.internal.transaction.CombatDispatchScope.isActive()) {
            throw new IllegalStateException("Cannot close a world runtime during combat execution or delivery");
        }
        serverRuntime.drain();
        effects.close();
        equipment.close();
        sources.close();
        regeneration.close();
        behaviors.close();
        reactions.clear();
        shields.close();
        healing.close();
        attributes.clear();
        panels.close();
        persistentSources.clear();
        serverRuntime.detach(level, this);
        closed = true;
    }
}
