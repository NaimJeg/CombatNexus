package io.github.naimjeg.combatnexus.combat.damage.builtin.bridge;

import io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor;

import io.github.naimjeg.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.api.display.VanillaContributionDescriptors;
import io.github.naimjeg.combatnexus.combat.damage.api.context.NativeDamageInput;

public class VanillaArmorEffectivenessProcessor implements DamagePhaseProcessor {

    private static final String TRACE_ID = "vanilla:armor_effectiveness";

    @Override
    public void apply(DamageRuleContext context) {
        var ctx = context;

        if (ctx.nativeInput().armor()
                != NativeDamageInput.Reduction.COMBATNEXUS) return;

        final float multiplier = ctx.nativeInput().armorEffectiveness();

        if (multiplier != 1.0f) {
            DamageMutationResult result = ctx.tryMultiplyArmorEffectiveness(
                    multiplier,
                    TRACE_ID
            );

            ctx.recordContribution(
                    result,
                    () -> VanillaContributionDescriptors.vanillaArmorEffectiveness(
                            DamageModuleIds.id("vanilla_armor_effectiveness"),
                            DamagePhase.MITIGATION_SETUP,
                            multiplier,
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
        return DamageProcessorPriorities.VANILLA_ARMOR_EFFECTIVENESS;
    }
}
