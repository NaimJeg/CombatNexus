package io.github.naimjeg.combatnexus.combat.damage.core.pipeline;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

record DamageEventSnapshot(
        LivingEntity attacker,
        LivingEntity victim,
        DamageSource source,
        float eventOriginalAmount,
        float initialBaseAmount
) {
    static DamageEventSnapshot create(
            DamageInput event,
            LivingEntity attacker,
            LivingEntity victim,
            float initialBaseAmount
    ) {
        float original = event.originalAmount();

        float safeInitialBase =
                Float.isFinite(initialBaseAmount)
                        ? Math.max(0.0f, initialBaseAmount)
                        : Math.max(0.0f, original);

        return new DamageEventSnapshot(
                attacker,
                victim,
                event.source(),
                original,
                safeInitialBase
        );
    }
}
