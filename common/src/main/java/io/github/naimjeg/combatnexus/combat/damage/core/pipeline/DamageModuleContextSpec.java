package io.github.naimjeg.combatnexus.combat.damage.core.pipeline;
import io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration;

import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageOrigin;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla.VanillaDamageCapture;
import io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla.VanillaDamageSourceProfile;
import net.minecraft.world.entity.LivingEntity;

/**
 * Internal immutable construction payload for DamageModuleContext.
 *
 * <p>The factory owns how this spec is built. The context owns how the spec is
 * decomposed into event/source/packet/combat state.</p>
 */
record DamageModuleContextSpec(
        DamageInput event,
        DamageResultWriter writer,
        LivingEntity attacker,
        LivingEntity victim,
        DamageOrigin origin,
        io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration definitions,
        VanillaDamageSourceProfile sourceProfile,
        float initialBaseAmount,
        VanillaDamageCapture.OffensiveSnapshot vanillaSnapshot,
        boolean rebuildVanillaOffensiveMobEffects,
        boolean rebuildVanillaOffensiveEnchantment,
        boolean rebuildVanillaPreEventDelta,
        float vanillaOffensiveMobEffectDelta,
        DamageApplicationBucket initialBaseBucket,
        DamageApplicationBucket vanillaOffensiveMobEffectBucket,
        DamageApplicationBucket vanillaOffensiveEnchantmentBucket
) {
    static DamageModuleContextSpec of(
            DamageInput event,
        DamageResultWriter writer,
            LivingEntity attacker,
            LivingEntity victim,
            DamageOrigin origin,
        io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration definitions,
            VanillaDamageSourceProfile sourceProfile,
            float initialBaseAmount,
            VanillaDamageCapture.OffensiveSnapshot vanillaSnapshot,
            boolean rebuildVanillaOffensiveMobEffects,
            boolean rebuildVanillaOffensiveEnchantment,
            boolean rebuildVanillaPreEventDelta,
            float vanillaOffensiveMobEffectDelta,
            DamageApplicationBucket initialBaseBucket,
            DamageApplicationBucket vanillaOffensiveMobEffectBucket,
            DamageApplicationBucket vanillaOffensiveEnchantmentBucket
    ) {
        return new DamageModuleContextSpec(
                event,
                writer,
                attacker,
                victim,
                origin, definitions,
                sourceProfile,
                initialBaseAmount,
                vanillaSnapshot,
                rebuildVanillaOffensiveMobEffects,
                rebuildVanillaOffensiveEnchantment,
                rebuildVanillaPreEventDelta,
                vanillaOffensiveMobEffectDelta,
                initialBaseBucket,
                vanillaOffensiveMobEffectBucket,
                vanillaOffensiveEnchantmentBucket
        );
    }
}

