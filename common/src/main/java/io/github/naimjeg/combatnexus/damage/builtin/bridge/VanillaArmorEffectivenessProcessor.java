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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class VanillaArmorEffectivenessProcessor implements DamagePhaseProcessor {

    private static final String TRACE_ID = "vanilla:armor_effectiveness";

    @Override
    public void apply(DamageRuleContext context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor"
        );

        if (ctx.logicalAttacker() == null || ctx.victim() == null) return;
        if (!(ctx.victim().level() instanceof ServerLevel serverLevel)) return;

        ItemStack weaponStack = ctx.logicalAttacker().getWeaponItem();

        if (weaponStack.isEmpty()) {
            return;
        }

        float rawMultiplier = EnchantmentHelper.modifyArmorEffectiveness(
                serverLevel,
                weaponStack,
                ctx.victim(),
                ctx.source(),
                1.0f
        );

        if (!Float.isFinite(rawMultiplier)) {
            return;
        }

        final float multiplier = Math.max(0.0f, rawMultiplier);

        if (multiplier != 1.0f) {
            DamageMutationResult result = ctx.tryMultiplyArmorEffectiveness(
                    multiplier,
                    TRACE_ID
            );

            ctx.contributions().record(
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
