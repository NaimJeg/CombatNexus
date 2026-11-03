package io.github.naimjeg.combatnexus.damage.builtin.bridge;

import io.github.naimjeg.combatnexus.damage.api.DamageModulePreMultiplierBuckets;

import io.github.naimjeg.combatnexus.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.damage.api.DamageProcessorPriorities;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.enums.DamageApplicationBucket;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.damage.bridge.vanilla.PreEventDeltaKind;
import io.github.naimjeg.combatnexus.damage.bridge.vanilla.VanillaPreEventScalingBridge;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageModuleContext;
import io.github.naimjeg.combatnexus.damage.registry.PreMultiplierBuckets;

public final class VanillaProjectileScalingProcessor implements DamagePhaseProcessor {

    private static final String TRACE_ID = "vanilla:projectile_scaling";

    @Override
    public boolean canHandle(DamageRuleContext context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor predicate"
        );

        return VanillaPreEventScalingBridge.canApply(
                ctx,
                PreEventDeltaKind.PROJECTILE_SCALING,
                true
        );
    }

    @Override
    public void apply(DamageRuleContext context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor"
        );

        VanillaPreEventScalingBridge.applyApplicationPreMultiplierToAll(
                ctx,
                PreEventDeltaKind.PROJECTILE_SCALING,
                PreMultiplierBuckets.vanillaProjectile(),
                DamageModulePreMultiplierBuckets.VANILLA_PROJECTILE,
                TRACE_ID,
                true,
                DamageApplicationBucket.VANILLA_PROJECTILE_BASE,
                DamageApplicationBucket.VANILLA_PROJECTILE_ENCHANTMENT
        );
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.GLOBAL_ADJUSTMENT;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_PROJECTILE_SCALING;
    }
}
