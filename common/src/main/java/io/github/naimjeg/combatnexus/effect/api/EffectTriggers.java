package io.github.naimjeg.combatnexus.effect.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

/** Dispatch an adopted plan through registered capabilities; contains no effect-specific rules. */
public final class EffectTriggers {
    private EffectTriggers() {}
    public static void dispatch(LivingEntity entity, EffectInstance effect, EffectCapability.Trigger trigger, DamageSource source, float damage) {
        for (var component : effect.behavior().components()) if (component instanceof EffectCapabilityBehavior capability)
            io.github.naimjeg.combatnexus.effect.internal.EffectCapabilities.execute(capability.capability(),
                new EffectCapability.Context(entity, effect, trigger, source, damage));
    }
}
