package io.github.naimjeg.combatnexus.damage.builtin.bridge;

import io.github.naimjeg.combatnexus.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.damage.api.DamageProcessorPriorities;
import io.github.naimjeg.combatnexus.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.damage.core.contribution.VanillaContributionDescriptors;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageModuleContext;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class VanillaOffensiveMobEffectProcessor implements DamagePhaseProcessor {

    private static final float EPSILON = 0.0001f;
    private static final String TRACE_ID =
            "vanilla:mob_effect/strength_weakness";

    @Override
    public void apply(DamageRuleContext context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor"
        );

        float delta = ctx.getVanillaOffensiveMobEffectDelta();

        if (!Float.isFinite(delta) || Math.abs(delta) <= EPSILON) {
            return;
        }

        DamageMutationResult result = ctx.tryAddBaseDamage(
                ctx.getInitialChannel(),
                ctx.getVanillaOffensiveMobEffectBucket(),
                delta,
                TRACE_ID
        );

        ctx.contributions().record(
                result,
                () -> VanillaContributionDescriptors.vanillaMobEffectBase(
                        DamageModuleIds.id(
                                "vanilla_offensive_mob_effect/"
                                        + ctx.getVanillaOffensiveMobEffectBucket()
                                        .name()
                                        .toLowerCase(Locale.ROOT)
                        ),
                        DamagePhase.BASE_MODIFICATION,
                        ctx.getInitialChannel().id(),
                        ctx.getVanillaOffensiveMobEffectBucket(),
                        delta,
                        TRACE_ID
                )
        );
    }

    @Override
    public boolean canHandle(DamageRuleContext context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor predicate"
        );

        return ctx.shouldRebuildVanillaOffensiveMobEffects()
                && ctx.vanillaSourceProfile().shouldApplyMeleeOffensiveMobEffects()
                && Math.abs(ctx.getVanillaOffensiveMobEffectDelta()) > EPSILON;
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.BASE_MODIFICATION;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_OFFENSIVE_MOB_EFFECT;
    }
}
