package io.github.naimjeg.combatnexus.combat.damage.builtin.bridge;

import io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor;

import io.github.naimjeg.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;

import io.github.naimjeg.combatnexus.combat.damage.api.display.VanillaContributionDescriptors;
import io.github.naimjeg.combatnexus.combat.damage.api.context.NativeDamageInput;

public final class VanillaDamageProtectionProcessor implements DamagePhaseProcessor {

    private static final String TRACE_ID = "vanilla:damage_protection";

    @Override
    public void apply(DamageRuleContext context) {
        var ctx = context;

        if (ctx.nativeInput().enchantments()
                != NativeDamageInput.Reduction.COMBATNEXUS) return;

        float score = ctx.nativeInput().protectionScore();

        if (score <= 0.0f) {
            return;
        }

        float rating = score * ctx.nativeInput().ratingPerProtectionScore();

        for (var channel : ctx.activeChannels()) {

            DamageMutationResult result = ctx.tryAddTemporaryResistance(
                    channel,
                    rating,
                    TRACE_ID
            );

            ctx.recordContribution(
                    result,
                    () -> VanillaContributionDescriptors.vanillaTemporaryResistance(
                            DamageModuleIds.id(
                                    "vanilla_damage_protection/"
                                            + channel.id().getPath()
                            ),
                            DamagePhase.MITIGATION_SETUP,
                            channel.id(),
                            rating,
                            TRACE_ID
                    )
            );
        }

    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.MITIGATION_SETUP;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_DAMAGE_PROTECTION;
    }
}
