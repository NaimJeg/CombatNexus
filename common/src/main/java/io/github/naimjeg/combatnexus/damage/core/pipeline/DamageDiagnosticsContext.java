package io.github.naimjeg.combatnexus.damage.core.pipeline;

import io.github.naimjeg.combatnexus.damage.core.trace.DamageModuleTransaction;
import io.github.naimjeg.combatnexus.damage.core.trace.DamageModuleTransactionTracker;
import io.github.naimjeg.combatnexus.damage.core.trace.DamageObservationSnapshot;
import io.github.naimjeg.combatnexus.damage.core.config.DamageModuleSettings;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.CombatTrace;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.CombatTraceFactory;
import net.minecraft.world.entity.LivingEntity;

public final class DamageDiagnosticsContext {

    private final long damageId;
    private final CombatTrace trace;
    private final DamageObservationSnapshot incomingObservation;

    private DamageDiagnosticsContext(
            long damageId,
            CombatTrace trace,
            DamageObservationSnapshot incomingObservation
    ) {
        this.damageId = damageId;
        this.trace = trace;
        this.incomingObservation = incomingObservation;
    }

    public static DamageDiagnosticsContext create(
            long damageId,
            LivingEntity victim
    ) {
        DamageObservationSnapshot observation =
                victim != null && DamageModuleTransactionTracker.enabled()
                        ? DamageObservationSnapshot.capture(victim)
                        : null;

        return new DamageDiagnosticsContext(
                damageId,
                CombatTraceFactory.create(damageId),
                observation
        );
    }

    public long damageId() {
        return damageId;
    }

    public CombatTrace trace() {
        return trace;
    }

    public boolean transactionEnabled() {
        return incomingObservation != null;
    }

    DamageModuleTransaction createIncomingTransaction(
            DamageEventSnapshot event,
            DamagePipelineResult result,
            float eventAmountBeforeSet,
            float eventAmountAfterSet
    ) {
        if (incomingObservation == null) {
            return null;
        }

        return new DamageModuleTransaction(
                damageId,
                event.attacker(),
                event.victim(),
                event.source(),

                event.eventOriginalAmount(),
                event.initialBaseAmount(),
                result.offensiveTotal(),
                result.finalEventDamage(),

                eventAmountBeforeSet,
                eventAmountAfterSet,

                Float.NaN,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                false,

                incomingObservation.health(),
                incomingObservation.absorption(),
                incomingObservation.invulnerableTime(),
                incomingObservation.gameTime(),

                DamageModuleSettings.vanillaReductionMode(),
                DamageModuleSettings.suppressVanillaArmorReduction(),
                DamageModuleSettings.suppressVanillaEnchantmentReduction(),
                DamageModuleSettings.suppressVanillaMobEffectReduction(),
                DamageModuleSettings.suppressVanillaInnateResistanceReduction()
        );
    }
}
