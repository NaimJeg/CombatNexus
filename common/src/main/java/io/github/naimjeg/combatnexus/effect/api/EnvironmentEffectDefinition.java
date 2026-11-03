package io.github.naimjeg.combatnexus.effect.api;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/** A stable environmental binding with native-triggered or effect-scheduled damage. */
public record EnvironmentEffectDefinition(Identifier id, Identifier damageType, Predicate<LivingEntity> condition,
        ToDoubleFunction<LivingEntity> minimumHealth, boolean timed, ToDoubleFunction<LivingEntity> baseAmount, int intervalTicks, boolean scheduled, int initialDelayTicks) {
    public EnvironmentEffectDefinition(Identifier id, Identifier damageType, Predicate<LivingEntity> condition, ToDoubleFunction<LivingEntity> minimumHealth, boolean timed, ToDoubleFunction<LivingEntity> baseAmount, int intervalTicks, boolean scheduled) {
        this(id, damageType, condition, minimumHealth, timed, baseAmount, intervalTicks, scheduled, 0);
    }
    public EnvironmentEffectDefinition(Identifier id, Identifier damageType, Predicate<LivingEntity> condition, ToDoubleFunction<LivingEntity> minimumHealth, boolean timed, ToDoubleFunction<LivingEntity> baseAmount, int intervalTicks) {
        this(id, damageType, condition, minimumHealth, timed, baseAmount, intervalTicks, false);
    }
    public EnvironmentEffectDefinition(Identifier id, Identifier damageType, Predicate<LivingEntity> condition, ToDoubleFunction<LivingEntity> minimumHealth, boolean timed) {
        this(id, damageType, condition, minimumHealth, timed, entity -> 1, timed ? 20 : 0);
    }
    public EnvironmentEffectDefinition(Identifier id, Identifier damageType, Predicate<LivingEntity> condition, ToDoubleFunction<LivingEntity> minimumHealth) {
        this(id, damageType, condition, minimumHealth, false);
    }
    public EnvironmentEffectDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(damageType); Objects.requireNonNull(condition); Objects.requireNonNull(minimumHealth);
        Objects.requireNonNull(baseAmount);
        if (initialDelayTicks < 0) throw new IllegalArgumentException("Negative initial environment delay");
        if (intervalTicks < 0) throw new IllegalArgumentException("Negative environment interval");
        if (scheduled && intervalTicks < 1) throw new IllegalArgumentException("Scheduled environment requires a positive interval");
    }
    public EnvironmentDamage parameters(LivingEntity entity) { return new EnvironmentDamage(damageType, baseAmount.applyAsDouble(entity), minimumHealth.applyAsDouble(entity), intervalTicks); }
    public io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorPlan behavior(LivingEntity entity) {
        var parameters = parameters(entity);
        return new io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorPlan(0, java.util.List.of(scheduled ? new ScheduledEnvironmentDamage(parameters, initialDelayTicks) : parameters));
    }
}
