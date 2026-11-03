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

public final class VanillaInitialBaseDamageProcessor implements DamagePhaseProcessor {

    private static final float EPSILON = 0.0001f;

    @Override
    public void apply(DamageRuleContext context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor"
        );

        String traceId = "vanilla:initial_base/"
                + ctx.getInitialBaseBucket().name().toLowerCase(Locale.ROOT);

        DamageMutationResult result = ctx.tryAddBaseDamage(
                ctx.getInitialChannel(),
                ctx.getInitialBaseBucket(),
                ctx.getInitialBaseAmount(),
                traceId
        );

        ctx.contributions().record(
                result,
                () -> VanillaContributionDescriptors.vanillaBase(
                        DamageModuleIds.id(
                                "vanilla_initial_base/"
                                        + ctx.getInitialBaseBucket()
                                                .name()
                                                .toLowerCase(Locale.ROOT)
                        ),
                        DamagePhase.BASE_MODIFICATION,
                        ctx.getInitialChannel().id(),
                        ctx.getInitialBaseBucket(),
                        ctx.getInitialBaseAmount(),
                        traceId
                )
        );
    }

    @Override
    public boolean canHandle(DamageRuleContext context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor predicate"
        );

        return ctx.getInitialBaseAmount() > EPSILON;
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.BASE_MODIFICATION;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_INITIAL_BASE;
    }
}
