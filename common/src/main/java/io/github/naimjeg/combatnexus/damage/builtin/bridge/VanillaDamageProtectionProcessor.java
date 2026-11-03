package io.github.naimjeg.combatnexus.damage.builtin.bridge;

import io.github.naimjeg.combatnexus.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.damage.api.DamageProcessorPriorities;
import io.github.naimjeg.combatnexus.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.damage.config.DamageModuleConfig;
import io.github.naimjeg.combatnexus.damage.core.DamageComponent;
import io.github.naimjeg.combatnexus.damage.core.contribution.VanillaContributionDescriptors;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageModuleContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class VanillaDamageProtectionProcessor implements DamagePhaseProcessor {

    private static final String TRACE_ID = "vanilla:damage_protection";

    @Override
    public void apply(DamageRuleContext context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor"
        );

        if (ctx.victim() == null) return;
        if (!(ctx.victim().level() instanceof ServerLevel serverLevel)) return;

        if (ctx.source().is(DamageTypeTags.BYPASSES_EFFECTS)) return;
        if (ctx.source().is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) return;

        float score = EnchantmentHelper.getDamageProtection(
                serverLevel,
                ctx.victim(),
                ctx.source()
        );

        if (score <= 0.0f) {
            return;
        }

        float rating = score * DamageModuleConfig.current()
                .formulas()
                .ratingPerProtScore();

        for (int i = 0; i < ctx.getActiveComponentCount(); i++) {
            DamageComponent component = ctx.getActiveComponent(i);

            DamageMutationResult result = ctx.tryAddTemporaryResistance(
                    component.channel,
                    rating,
                    TRACE_ID
            );

            ctx.contributions().record(
                    result,
                    () -> VanillaContributionDescriptors.vanillaTemporaryResistance(
                            DamageModuleIds.id(
                                    "vanilla_damage_protection/"
                                            + component.channel.id().getPath()
                            ),
                            DamagePhase.MITIGATION_SETUP,
                            component.channel.id(),
                            rating,
                            TRACE_ID
                    )
            );
        }

        if (ctx.trace().enabled()) {
            ctx.trace().calculation().enchantmentProtection(
                    TRACE_ID,
                    0,
                    score,
                    rating
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
