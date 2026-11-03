package io.github.naimjeg.combatnexus.combat.damage.builtin.bridge;

import io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor;

import io.github.naimjeg.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;

import io.github.naimjeg.combatnexus.combat.damage.api.display.VanillaContributionDescriptors;
import io.github.naimjeg.combatnexus.combat.damage.api.context.NativeDamageInput;
import net.minecraft.tags.DamageTypeTags;

public final class VanillaResistanceEffectProcessor implements DamagePhaseProcessor {

    private static final String TRACE_ID = "vanilla:mob_effect/resistance";

    @Override
    public void apply(DamageRuleContext context) {
        var ctx = context;

        if (ctx.nativeInput().effects()
                != NativeDamageInput.Reduction.COMBATNEXUS) return;

        if (ctx.victim() == null) return;

        if (ctx.source().is(DamageTypeTags.BYPASSES_EFFECTS)) return;
        if (ctx.source().is(DamageTypeTags.BYPASSES_RESISTANCE)) return;

        var amplifier = ctx.facts(ctx.victim().getUUID()).amplifier(net.minecraft.world.effect.MobEffects.RESISTANCE);
        float reduction = amplifier.isPresent() ? io.github.naimjeg.combatnexus.formula.MultiplierFormula
                .resistanceEffectReduction(amplifier.getAsInt()) : 0;

        if (reduction <= 0.0f) {
            return;
        }

        for (var channel : ctx.activeChannels()) {

            DamageMutationResult result = ctx.tryAddChannelMitigation(
                    channel,
                    reduction,
                    TRACE_ID
            );

            ctx.recordContribution(
                    result,
                    () -> VanillaContributionDescriptors.vanillaMitigation(
                            DamageModuleIds.id(
                                    "vanilla_resistance_effect/"
                                            + channel.id().getPath()
                            ),
                            DamagePhase.MITIGATION_SETUP,
                            channel.id(),
                            reduction,
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
        return DamageProcessorPriorities.VANILLA_RESISTANCE_EFFECT;
    }
}
