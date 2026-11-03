package io.github.naimjeg.combatnexus.effect.platform;

import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import io.github.naimjeg.combatnexus.attribute.api.AttributeSnapshot;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorPlan;
import io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleApi;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.*;
import io.github.naimjeg.combatnexus.effect.api.*;
import io.github.naimjeg.combatnexus.combat.api.platform.NativeEffectProjection;
import io.github.naimjeg.combatnexus.combat.platform.EquipmentCombatAdapter;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Version-bound effect capabilities and projection adapter. All timers live in the common service. */
@EventBusSubscriber(modid = "combatnexus")
public final class NativeEffectBridge {
    // Explicit combat ownership; other vanilla effects retain their native lifecycle.
    private static final Set<String> COMBAT_EFFECTS = Set.of(
        "speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health", "instant_damage",
        "jump_boost", "regeneration", "resistance", "fire_resistance", "invisibility", "blindness",
        "weakness", "poison", "wither", "health_boost", "absorption", "glowing", "levitation",
        "slow_falling", "darkness", "wind_charged", "weaving", "oozing", "infested");
    private static final Set<Identifier> MANAGED = new LinkedHashSet<>();
    private static final Map<Identifier, java.util.function.IntFunction<BehaviorPlan>> PLANS = new HashMap<>();
    private static final Map<LivingEntity, List<EffectInstance>> DEATH_CALLBACKS = new WeakHashMap<>();
    private record Pending(MobEffectInstance effect, UUID source, boolean force, io.github.naimjeg.combatnexus.combat.api.source.SourceProvenance provenance) {}
    private static final Map<LivingEntity, List<Pending>> PENDING = new WeakHashMap<>();
    private NativeEffectBridge() {}
    public static void registerProjection(Identifier id) { MANAGED.add(id); }
    @SubscribeEvent public static void register(io.github.naimjeg.combatnexus.combat.platform.api.CombatRegisterEvent event) {
        for (var holder : BuiltInRegistries.MOB_EFFECT.listElements().toList()) {
            var id = holder.key().identifier();
            if (!id.getNamespace().equals("minecraft") || !COMBAT_EFFECTS.contains(id.getPath())) continue;
            MANAGED.add(id);
            event.registrar().effectDisplay(id, new EffectDisplay(id, false, true, true, null, true, true, false));
            event.registrar().effectCapability(id, new EffectCapability() {
                public boolean execute(Context context) { return NativeEffectBridge.execute(holder, context); }
                public List<net.minecraft.network.chat.Component> describe(int amplifier) {
                    return io.github.naimjeg.combatnexus.presentation.platform.VanillaCombatEffectDescriptions.describe(id, amplifier);
                }
            });
        }
        PLANS.put(Identifier.parse("minecraft:poison"), amplifier -> new BehaviorPlan(0, List.of(
            new io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent.PeriodicDamage(1, Identifier.parse("neoforge:poison"), Math.max(1, 25 >> amplifier), 1, -1,
                io.github.naimjeg.combatnexus.combat.api.behavior.PeriodicPhase.REMAINING_DURATION))));
        PLANS.put(Identifier.parse("minecraft:wither"), amplifier -> new BehaviorPlan(0, List.of(
            new io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent.PeriodicDamage(1, Identifier.parse("minecraft:wither"), Math.max(1, 40 >> amplifier), 0, -1,
                io.github.naimjeg.combatnexus.combat.api.behavior.PeriodicPhase.REMAINING_DURATION))));
        PLANS.put(Identifier.parse("minecraft:regeneration"), amplifier -> new BehaviorPlan(0, List.of(
            new io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent.PeriodicHealing(1, Math.max(1, 50 >> amplifier), -1, true,
                io.github.naimjeg.combatnexus.combat.api.behavior.PeriodicPhase.REMAINING_DURATION))));
        PLANS.put(Identifier.parse("minecraft:absorption"), amplifier -> new BehaviorPlan(0, List.of(new EffectCapabilityBehavior(Identifier.parse("minecraft:absorption"), 1))));
    }
    public static boolean managed(Holder<MobEffect> effect) {
        var id = effect.unwrapKey().orElseThrow().identifier();
        return MANAGED.contains(id) || EquipmentCombatAdapter.effects().projected(id);
    }
    public static boolean ready(LivingEntity entity) {
        return entity.level() instanceof ServerLevel level && CombatWorldSessions.current(level).isPresent()
                && level.getEntity(entity.getUUID()) == entity;
    }
    public static Optional<EffectInstance> state(LivingEntity entity, Holder<MobEffect> effect) {
        if (!(entity.level() instanceof ServerLevel level)) return Optional.empty();
        var id = effect.unwrapKey().orElseThrow().identifier();
        return CombatWorldSessions.managedEffects(level, entity.getUUID()).stream().filter(e -> id.equals(e.display().nativeId())).findFirst();
    }
    public static MobEffectInstance projection(EffectInstance effect, long now) {
        var d = effect.display(); var s = effect.snapshot();
        return new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.get(d.nativeId()).orElseThrow(),
            s.expiresAtTick().isEmpty() ? -1 : (int)Math.min(Integer.MAX_VALUE, Math.max(0, s.expiresAtTick().getAsLong() - now)),
            s.level(), d.ambient(), d.particles(), d.icon());
    }
    public static void synchronizeDescriptions(LivingEntity entity, Collection<Identifier> previous) {
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer player)) return;
        var states = CombatWorldSessions.managedEffects(player.level(), player.getUUID());
        var remaining = new HashSet<>(previous);
        for (var state : states) if (state.display().nativeId() != null) {
            var id = state.display().nativeId(); remaining.remove(id);
            String encoded;
            try {
            var snapshot = state.snapshot();
            var lifetime = snapshot.expiresAtTick().isEmpty() ? EffectLifetime.permanent()
                : EffectLifetime.timed((int)Math.min(Integer.MAX_VALUE, Math.max(1, snapshot.expiresAtTick().getAsLong() - player.level().getGameTime())));
            var definition = new EffectDefinition(snapshot.level(), lifetime,
                state.stacking(),
                state.policy(), state.attributes(), state.behavior(), snapshot.restrictions());
            var block = EffectTooltip.nativeInstance(id, definition);
            var summary = new ArrayList<>(block.summary());
            var details = new ArrayList<>(block.details());
            if (state.display().environment()) {
                summary.add(net.minecraft.network.chat.Component.translatable("tooltip.combatnexus.environment." + id.getPath()));
                summary.add(net.minecraft.network.chat.Component.translatable("tooltip.combatnexus.effect.condition_ends"));
            }
            for (var layer : java.util.stream.Stream.concat(java.util.stream.Stream.of(state), state.hidden().stream()).toList())
                details.add(net.minecraft.network.chat.Component.translatable("tooltip.combatnexus.effect.source", io.github.naimjeg.combatnexus.presentation.api.SourceProvenanceText.describe(layer.display().provenance())));
            var projected = new io.github.naimjeg.combatnexus.presentation.api.TooltipBlock(block.title(), summary, details);
            encoded = io.github.naimjeg.combatnexus.presentation.api.DescriptionDocument.CODEC.encodeStart(
                net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, player.registryAccess()), io.github.naimjeg.combatnexus.presentation.api.DescriptionDocument.leaf(projected)).getOrThrow().toString();
            if (encoded.length() > 24000) encoded = "";
            } catch (Exception failure) {
                encoded = "";
            }
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new io.github.naimjeg.combatnexus.presentation.network.EffectTooltipPayload(
                player.getUUID(), CombatWorldSessions.current(player.level()).orElseThrow(),
                io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleApi.definitionGeneration(player.level().getServer()), player.level().dimension().identifier(), id, encoded));
        }
        for (var id : remaining) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            new io.github.naimjeg.combatnexus.presentation.network.EffectTooltipPayload(player.getUUID(), CombatWorldSessions.current(player.level()).orElseThrow(),
                io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleApi.definitionGeneration(player.level().getServer()), player.level().dimension().identifier(), id, ""));
    }
    public static boolean add(LivingEntity entity, MobEffectInstance incoming, Entity source, boolean force) {
        return add(entity, incoming, source, source == null ? null : source.getUUID(), force, EquipmentCombatAdapter.effects().origin(entity, source == null ? null : source.getUUID()));
    }
    private static boolean add(LivingEntity entity, MobEffectInstance incoming, Entity source, UUID sourceId, boolean force, io.github.naimjeg.combatnexus.combat.api.source.SourceProvenance provenance) {
        if (!ready(entity)) {
            PENDING.computeIfAbsent(entity, ignored -> new ArrayList<>()).add(new Pending(new MobEffectInstance(incoming), sourceId, force, provenance));
            return true;
        }
        if (!net.neoforged.neoforge.common.CommonHooks.canMobEffectBeApplied(entity, incoming, source)) return false;
        var old = state(entity, incoming.getEffect());
        var level = (ServerLevel)entity.level();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added(
            entity, old.map(e -> projection(e, level.getGameTime())).orElse(null), incoming, source));
        if (incoming.getEffect().value().isInstantenous()) {
            return instant(incoming.getEffect(), entity, source, source, incoming.getAmplifier(), 1).outcome() == EffectResult.Outcome.APPLIED;
        }
        if (incoming.getDuration() == 0) return false;
        var id = incoming.getEffect().unwrapKey().orElseThrow().identifier();
        var origin = Identifier.fromNamespaceAndPath("combatnexus", sourceId == null ? "unknown_effect_source" : "entity/" + sourceId);
        var attrs = new ArrayList<EffectInstance.AttributeContribution>();
        incoming.getEffect().value().createModifiers(incoming.getAmplifier(), (attribute, modifier) -> {
            if (entity.getAttribute(attribute) != null) attrs.add(new EffectInstance.AttributeContribution(attribute.unwrapKey().orElseThrow().identifier(),
                new AttributeSnapshot.Contribution(modifier.id(), modifier.amount(), modifier.operation())));
        });
        var lifetime = incoming.isInfiniteDuration() ? EffectLifetime.permanent() : EffectLifetime.timed(incoming.getDuration());
        var display = new EffectDisplay(id, incoming.isAmbient(), incoming.isVisible(), incoming.showIcon(), sourceId, true, true, false, provenance);
        var plan = PLANS.getOrDefault(id, amplifier -> new BehaviorPlan(0, List.of(new EffectCapabilityBehavior(id, 0)))).apply(incoming.getAmplifier());
        if (incoming.isInfiniteDuration()) plan = new BehaviorPlan(plan.priority(), plan.components().stream().map(component -> {
            if (component instanceof io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent.PeriodicHealing healing)
                return (io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent)new io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent.PeriodicHealing(
                    healing.amount(), healing.intervalTicks(), Math.floorMod(-entity.tickCount, healing.intervalTicks()), healing.onlyWhenMissingHealth(), healing.phase());
            if (component instanceof io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent.PeriodicDamage damage)
                return (io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent)new io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent.PeriodicDamage(
                    damage.amount(), damage.damageType(), damage.intervalTicks(), damage.minimumHealth(), Math.floorMod(-entity.tickCount, damage.intervalTicks()), damage.phase());
            return component;
        }).toList());
        var instance = new EffectInstance(new EffectSnapshot(entity.getUUID(), id, origin, incoming.getAmplifier(), lifetime.deadline(level.getGameTime()), Set.of(), lifetime.kind()),
            attrs, EffectInstance.Ownership.MANAGED, plan,
            new EffectPolicy(EffectPolicy.Category.valueOf(incoming.getEffect().value().getCategory().name()), true), List.of(), display);
        var result = CombatAccess.find(level).orElseThrow().effects().apply(new EffectApplication(UUID.randomUUID(), instance,
            force ? EffectService.Stacking.REPLACE : EffectService.Stacking.STRONGER_WITH_HIDDEN));
        if (result.outcome() != EffectResult.Outcome.APPLIED) return false;
        if (!force) {
            if (old.isEmpty()) incoming.getEffect().value().onEffectAdded(entity, incoming.getAmplifier());
            incoming.getEffect().value().onEffectStarted(entity, incoming.getAmplifier());
        }
        var after = state(entity, incoming.getEffect());
        return !old.equals(after);
    }
    public static boolean remove(LivingEntity entity, Holder<MobEffect> type, boolean event) {
        var current = state(entity, type);
        if (current.isEmpty() || !current.get().policy().dispellable()) return false;
        if (event && net.neoforged.neoforge.event.EventHooks.onEffectRemoved(entity, type)) return false;
        var s = current.orElseThrow().snapshot();
        return CombatAccess.find((ServerLevel)entity.level()).orElseThrow().effects()
            .remove(UUID.randomUUID(), entity.getUUID(), s.effect(), s.source(), EffectService.Removal.DISPELLED).applied();
    }
    public static void readyInputs(LivingEntity entity) {
        var pending = PENDING.remove(entity); if (pending == null) return;
        var level = (ServerLevel)entity.level();
        for (var value : pending) add(entity, value.effect(), value.source() == null ? null : level.getEntity(value.source()), value.source(), value.force(), value.provenance());
    }
    public static EffectResult instant(Holder<MobEffect> type, LivingEntity target, Entity direct, Entity owner, int amplifier, double intensity) {
        if (!Double.isFinite(intensity) || intensity < 0) throw new IllegalArgumentException("Invalid instant intensity");
        var id = type.unwrapKey().orElseThrow().identifier();
        io.github.naimjeg.combatnexus.combat.api.behavior.InstantBehavior behavior;
        if (type.is(MobEffects.INSTANT_DAMAGE) != target.isInvertedHealAndHarm())
            behavior = new io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent.InstantDamage(Math.max(0, (int)(intensity * (6 << amplifier) + 0.5)),
                Identifier.withDefaultNamespace(direct == null ? "magic" : "indirect_magic"), Optional.ofNullable(direct).map(Entity::getUUID));
        else behavior = new io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent.InstantHealing(Math.max(0, (int)(intensity * (4 << amplifier) + 0.5)));
        var source = Identifier.fromNamespaceAndPath("combatnexus", owner == null ? "unknown_effect_source" : "entity/" + owner.getUUID());
        var instance = new EffectInstance(new EffectSnapshot(target.getUUID(), id, source, amplifier, OptionalLong.empty(), Set.of(), EffectLifetime.Kind.INSTANT),
            List.of(), EffectInstance.Ownership.MANAGED, new BehaviorPlan(0, List.of(behavior)), EffectPolicy.DEFAULT, List.of(),
            new EffectDisplay(id, false, false, false, owner == null ? null : owner.getUUID(), true, false, false, EquipmentCombatAdapter.effects().origin(target, owner == null ? null : owner.getUUID())));
        return CombatAccess.find((ServerLevel)target.level()).orElseThrow().effects().apply(new EffectApplication(UUID.randomUUID(), instance, EffectService.Stacking.REPLACE));
    }
    public static boolean mayExpire(ServerLevel level, EffectInstance effect) {
                if (effect.display().nativeId() == null || !(level.getEntity(effect.snapshot().target()) instanceof LivingEntity entity)) return true;
                return !net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired(
                    entity, projection(effect, level.getGameTime()))).isCanceled();
    }
    public static void committed(ServerLevel level, EffectService.Change change) {
                Set<UUID> targets = new LinkedHashSet<>();
                change.before().forEach(e -> targets.add(e.snapshot().target())); change.after().forEach(e -> targets.add(e.snapshot().target()));
                for (var id : targets) if (level.getEntity(id) instanceof LivingEntity entity) {
                    if (change.removal().orElse(null) == EffectService.Removal.DEATH)
                        DEATH_CALLBACKS.computeIfAbsent(entity, ignored -> new ArrayList<>()).addAll(change.before());
                    ((NativeEffectProjection)entity).combatnexus$projectEffects(true);
                    synchronizeDescriptions(entity, change.before().stream().map(e -> e.display().nativeId()).filter(Objects::nonNull).toList());
                }
    }
    public static void trigger(LivingEntity entity, EffectInstance effect, EffectCapability.Trigger trigger,
            net.minecraft.world.damagesource.DamageSource damageSource, float damage) {
        EffectTriggers.dispatch(entity, effect, trigger, damageSource, damage);
    }
    public static void removed(LivingEntity entity, Entity.RemovalReason reason) {
        var captured = DEATH_CALLBACKS.remove(entity);
        if (reason != Entity.RemovalReason.KILLED) return;
        if (captured == null && entity.level() instanceof ServerLevel level) captured = CombatWorldSessions.managedEffects(level, entity.getUUID());
        if (captured != null) for (var effect : captured) trigger(entity, effect, EffectCapability.Trigger.DEATH, null, 0);
    }
    private static boolean execute(Holder<MobEffect> type, EffectCapability.Context context) {
        var entity = context.entity(); var level = (ServerLevel)entity.level(); var state = context.effect().snapshot();
        int amplifier = state.level();
        switch (context.trigger()) {
            case HURT -> type.value().onMobHurt(level, entity, amplifier, context.damageSource(), context.damage());
            case DEATH -> type.value().onMobRemoved(level, entity, amplifier, Entity.RemovalReason.KILLED);
            case APPLY -> { } // Native application hooks also run for accepted weaker applications.
            case END -> { }
            case TICK -> {
                int phase = state.expiresAtTick().isEmpty() ? entity.tickCount
                    : (int)Math.max(0, state.expiresAtTick().getAsLong() - level.getGameTime());
                if (!type.value().shouldApplyEffectTickThisTick(phase, amplifier)) return true;
                return type.value().applyEffectTick(level, entity, amplifier);
            }
        }
        return true;
    }
}
