package io.github.naimjeg.combatnexus.combat.damage.builtin.bridge;

import io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.api.display.VanillaContributionDescriptors;

import java.util.Locale;

public final class VanillaOffensiveMobEffectProcessor implements DamagePhaseProcessor {

    private static final float EPSILON = 0.0001f;
    private static final String TRACE_ID =
            "vanilla:mob_effect/strength_weakness";

    @Override
    public void apply(DamageRuleContext context) {
        var ctx = context;

        float delta = ctx.nativeInput().mobEffectDelta();

        if (!Float.isFinite(delta) || Math.abs(delta) <= EPSILON) {
            return;
        }

        DamageMutationResult result = ctx.tryAddBaseDamage(
                ctx.getInitialChannel(),
                ctx.nativeInput().mobEffectBucket(),
                delta,
                TRACE_ID
        );

        ctx.recordContribution(
                result,
                () -> VanillaContributionDescriptors.vanillaMobEffectBase(
                        DamageModuleIds.id(
                                "vanilla_offensive_mob_effect/"
                                        + ctx.nativeInput().mobEffectBucket()
                                        .name()
                                        .toLowerCase(Locale.ROOT)
                        ),
                        DamagePhase.BASE_MODIFICATION,
                        ctx.getInitialChannel().id(),
                        ctx.nativeInput().mobEffectBucket(),
                        delta,
                        TRACE_ID
                )
        );
    }

    @Override
    public boolean canHandle(DamageContextView context) {
        var ctx = context;

        return ctx.nativeInput().rebuildMobEffects()
                && ctx.nativeInput().meleeEffects()
                && Math.abs(ctx.nativeInput().mobEffectDelta()) > EPSILON;
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
