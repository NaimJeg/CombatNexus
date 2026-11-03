package io.github.naimjeg.combatnexus.combat.internal.registration;
import io.github.naimjeg.combatnexus.combat.api.behavior.*;
import io.github.naimjeg.combatnexus.combat.api.action.TriggerLimit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/** Built-in gameplay registrations; adding another type does not change the storage or scheduler. */
final class BuiltinBehaviorTypes {
    private static final com.mojang.serialization.MapCodec<TriggerLimit> LIMIT = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.xmap(TriggerLimit.Scope::valueOf, Enum::name).fieldOf("trigger_scope").forGetter(TriggerLimit::scope),
        Codec.intRange(1,64).fieldOf("maximum_triggers").forGetter(TriggerLimit::maximum)).apply(i, TriggerLimit::new));
    static void register() {
        com.mojang.serialization.MapCodec<io.github.naimjeg.combatnexus.effect.api.EnvironmentDamage> environment = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("damage_type").forGetter(io.github.naimjeg.combatnexus.effect.api.EnvironmentDamage::damageType),
            Codec.DOUBLE.fieldOf("amount").forGetter(io.github.naimjeg.combatnexus.effect.api.EnvironmentDamage::amount),
            Codec.DOUBLE.fieldOf("minimum_health").forGetter(io.github.naimjeg.combatnexus.effect.api.EnvironmentDamage::minimumHealth),
            Codec.INT.fieldOf("interval_ticks").forGetter(io.github.naimjeg.combatnexus.effect.api.EnvironmentDamage::intervalTicks)).apply(i, io.github.naimjeg.combatnexus.effect.api.EnvironmentDamage::new));
        add("environment_damage", io.github.naimjeg.combatnexus.effect.api.EnvironmentDamage.class, environment);
        add("scheduled_environment_damage", io.github.naimjeg.combatnexus.effect.api.ScheduledEnvironmentDamage.class, RecordCodecBuilder.mapCodec(i -> i.group(
            environment.forGetter(io.github.naimjeg.combatnexus.effect.api.ScheduledEnvironmentDamage::parameters),
            Codec.intRange(0, 1200000).fieldOf("first_delay_ticks").forGetter(io.github.naimjeg.combatnexus.effect.api.ScheduledEnvironmentDamage::initialDelayTicks)
        ).apply(i, io.github.naimjeg.combatnexus.effect.api.ScheduledEnvironmentDamage::new)));
        BehaviorTypes.register(new BehaviorType<>(Identifier.fromNamespaceAndPath("combatnexus", "effect_capability"),
            io.github.naimjeg.combatnexus.effect.api.EffectCapabilityBehavior.class,
            RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("capability").forGetter(io.github.naimjeg.combatnexus.effect.api.EffectCapabilityBehavior::capability),
                Codec.INT.optionalFieldOf("first_delay_ticks", 1).forGetter(io.github.naimjeg.combatnexus.effect.api.EffectCapabilityBehavior::initialDelayTicks))
                    .apply(i, io.github.naimjeg.combatnexus.effect.api.EffectCapabilityBehavior::new))));
        add("lethal_protection", BehaviorComponent.LethalProtection.class, com.mojang.serialization.MapCodec.unit(new BehaviorComponent.LethalProtection()));
        add("reflection", BehaviorComponent.Reflection.class, RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("ratio").forGetter(BehaviorComponent.Reflection::ratio),
            LIMIT.codec().fieldOf("limit").forGetter(BehaviorComponent.Reflection::limit)).apply(i, BehaviorComponent.Reflection::new)));
        add("overflow_shield", BehaviorComponent.OverflowShield.class, RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("ratio").forGetter(BehaviorComponent.OverflowShield::ratio),
            Codec.DOUBLE.fieldOf("capacity").forGetter(BehaviorComponent.OverflowShield::capacity),
            Codec.INT.fieldOf("duration_ticks").forGetter(BehaviorComponent.OverflowShield::durationTicks),
            LIMIT.codec().fieldOf("limit").forGetter(BehaviorComponent.OverflowShield::limit)).apply(i, BehaviorComponent.OverflowShield::new)));
        add("periodic_healing", BehaviorComponent.PeriodicHealing.class, RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("amount").forGetter(BehaviorComponent.PeriodicHealing::amount),
            Codec.INT.fieldOf("interval_ticks").forGetter(BehaviorComponent.PeriodicHealing::intervalTicks),
            Codec.INT.optionalFieldOf("first_delay_ticks", -1).forGetter(BehaviorComponent.PeriodicHealing::firstDelayTicks),
            Codec.BOOL.optionalFieldOf("only_when_missing_health", false).forGetter(BehaviorComponent.PeriodicHealing::onlyWhenMissingHealth),
            Codec.STRING.xmap(PeriodicPhase::valueOf, Enum::name).optionalFieldOf("phase", PeriodicPhase.RESTART).forGetter(BehaviorComponent.PeriodicHealing::phase)).apply(i, BehaviorComponent.PeriodicHealing::new)));
        add("healing_additive", BehaviorComponent.HealingAdditive.class, Codec.DOUBLE.fieldOf("amount").xmap(BehaviorComponent.HealingAdditive::new, BehaviorComponent.HealingAdditive::amount));
        add("instant_healing", BehaviorComponent.InstantHealing.class, Codec.DOUBLE.fieldOf("amount").xmap(BehaviorComponent.InstantHealing::new, BehaviorComponent.InstantHealing::amount));
        add("instant_damage", BehaviorComponent.InstantDamage.class, RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("amount").forGetter(BehaviorComponent.InstantDamage::amount),
            Identifier.CODEC.fieldOf("damage_type").forGetter(BehaviorComponent.InstantDamage::damageType),
            net.minecraft.core.UUIDUtil.CODEC.optionalFieldOf("direct").forGetter(BehaviorComponent.InstantDamage::direct)).apply(i, BehaviorComponent.InstantDamage::new)));

        add("periodic_damage", BehaviorComponent.PeriodicDamage.class, RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("amount").forGetter(BehaviorComponent.PeriodicDamage::amount),
            Identifier.CODEC.fieldOf("damage_type").forGetter(BehaviorComponent.PeriodicDamage::damageType),
            Codec.INT.fieldOf("interval_ticks").forGetter(BehaviorComponent.PeriodicDamage::intervalTicks),
            Codec.FLOAT.optionalFieldOf("minimum_health", 0.0f).forGetter(BehaviorComponent.PeriodicDamage::minimumHealth),
            Codec.INT.optionalFieldOf("first_delay_ticks", -1).forGetter(BehaviorComponent.PeriodicDamage::firstDelayTicks),
            Codec.STRING.xmap(PeriodicPhase::valueOf, Enum::name).optionalFieldOf("phase", PeriodicPhase.RESTART).forGetter(BehaviorComponent.PeriodicDamage::phase)).apply(i, BehaviorComponent.PeriodicDamage::new)));
    }
    private static <T extends BehaviorComponent> void add(String id, Class<T> implementation, com.mojang.serialization.MapCodec<T> codec) {
        BehaviorTypes.register(new BehaviorType<>(Identifier.fromNamespaceAndPath("combatnexus", id), implementation, codec));
    }
}
