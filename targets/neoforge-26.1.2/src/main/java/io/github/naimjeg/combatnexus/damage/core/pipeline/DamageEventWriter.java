package io.github.naimjeg.combatnexus.damage.core.pipeline;

import io.github.naimjeg.combatnexus.damage.core.config.DamageModuleSettings;
import io.github.naimjeg.combatnexus.damage.core.trace.DamageModuleTransaction;
import io.github.naimjeg.combatnexus.damage.core.trace.DamageModuleTransactionTracker;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.CombatTrace;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

final class DamageEventWriter {

    private final DamageEventSnapshot event;
    private final DamageDiagnosticsContext diagnostics;
    private final CombatTrace trace;

    DamageEventWriter(
            DamageEventSnapshot event,
            DamageDiagnosticsContext diagnostics,
            CombatTrace trace
    ) {
        this.event = event;
        this.diagnostics = diagnostics;
        this.trace = trace;
    }

    void applyIncomingDamage(DamagePipelineResult result) {
        float eventAmountBeforeSet =
                event.neoforgeEvent().getAmount();

        event.neoforgeEvent().setAmount(
                result.finalEventDamage()
        );

        float eventAmountAfterSet =
                event.neoforgeEvent().getAmount();

        trace.transaction().apply(
                event.eventOriginalAmount(),
                event.initialBaseAmount(),
                result.offensiveTotal(),
                result.finalEventDamage()
        );

        recordIncomingCandidate(
                result,
                eventAmountBeforeSet,
                eventAmountAfterSet
        );

        trace.calculation().defensiveSummary(
                result.finalEventDamage()
        );

        suppressVanillaReductions();
    }

    private void recordIncomingCandidate(
            DamagePipelineResult result,
            float eventAmountBeforeSet,
            float eventAmountAfterSet
    ) {
        if (!diagnostics.transactionEnabled()) {
            return;
        }

        DamageModuleTransaction tx =
                diagnostics.createIncomingTransaction(
                        event,
                        result,
                        eventAmountBeforeSet,
                        eventAmountAfterSet
                );

        if (tx == null) {
            return;
        }

        DamageModuleTransactionTracker.recordIncomingCandidate(
                event.neoforgeEvent().getContainer(),
                tx
        );
    }

    private void suppressVanillaReductions() {
        switch (DamageModuleSettings.vanillaReductionMode()) {
            case FULL_REPLACEMENT -> suppressAllVanillaReductions();
            case CONFIGURABLE -> suppressConfiguredVanillaReductions();
            case COOPERATIVE -> {
                // Intentionally do not suppress vanilla reductions.
            }
        }
    }

    private void suppressAllVanillaReductions() {
        suppressVanillaReduction(DamageContainer.Reduction.ARMOR);
        suppressVanillaReduction(DamageContainer.Reduction.ENCHANTMENTS);
        suppressVanillaReduction(DamageContainer.Reduction.MOB_EFFECTS);
        suppressVanillaReduction(DamageContainer.Reduction.INNATE_RESISTANCE);
    }

    private void suppressConfiguredVanillaReductions() {
        if (DamageModuleSettings.suppressVanillaArmorReduction()) {
            suppressVanillaReduction(DamageContainer.Reduction.ARMOR);
        }

        if (DamageModuleSettings.suppressVanillaEnchantmentReduction()) {
            suppressVanillaReduction(DamageContainer.Reduction.ENCHANTMENTS);
        }

        if (DamageModuleSettings.suppressVanillaMobEffectReduction()) {
            suppressVanillaReduction(DamageContainer.Reduction.MOB_EFFECTS);
        }

        if (DamageModuleSettings.suppressVanillaInnateResistanceReduction()) {
            suppressVanillaReduction(DamageContainer.Reduction.INNATE_RESISTANCE);
        }
    }

    private void suppressVanillaReduction(
            DamageContainer.Reduction reduction
    ) {
        event.neoforgeEvent().addReductionModifier(
                reduction,
                (container, vanillaReduction) -> 0.0f
        );
    }

}
