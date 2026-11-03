package io.github.naimjeg.combatnexus.combat.damage.platform.pipeline;

import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageModuleContext;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageApplicationPlan;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageResultWriter;

import io.github.naimjeg.combatnexus.combat.damage.config.VanillaCompatibilitySettings;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

final class DamageEventWriter implements DamageResultWriter {
    private final net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent platformEvent;
    private final boolean witch, selfDamage, resistantType;
    private VanillaCompatibilitySettings compatibility;

    DamageEventWriter(
            net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event
    ) {
        this.platformEvent = event;
        witch = event.getEntity() instanceof net.minecraft.world.entity.monster.Witch;
        selfDamage = event.getSource().getEntity() == event.getEntity();
        resistantType = event.getSource().is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO);
    }

    @Override
    public void applyIncomingDamage(DamageModuleContext context, DamageApplicationPlan result) {
        if (result.cancelled()) platformEvent.setCanceled(true);
        this.compatibility = context.settings().values().vanillaCompatibility();
        platformEvent.setAmount(result.resolvedDamage());

        suppressVanillaReductions();
    }

    private void suppressVanillaReductions() {
        if (compatibility.shouldSuppressArmor()) {
            suppressVanillaReduction(DamageContainer.Reduction.ARMOR);
        }

        if (compatibility.shouldSuppressEnchantments()) {
            suppressVanillaReduction(DamageContainer.Reduction.ENCHANTMENTS);
        }

        if (compatibility.shouldSuppressMobEffects()) {
            suppressVanillaReduction(DamageContainer.Reduction.MOB_EFFECTS);
        }

        if (compatibility.shouldSuppressInnateResistance()) {
            // Replace the known native calculation at its original stage, not a second reduction.
            platformEvent.addReductionModifier(DamageContainer.Reduction.INNATE_RESISTANCE,
                    (container, nativeReduction) -> witch
                            ? io.github.naimjeg.combatnexus.formula.InnateResistanceFormula.reduction(
                                    container.getNewDamage(), selfDamage, resistantType)
                            : nativeReduction);
        }
    }

    private void suppressVanillaReduction(
            DamageContainer.Reduction reduction
    ) {
        platformEvent.addReductionModifier(
                reduction,
                (container, vanillaReduction) -> 0.0f
        );
    }

}
