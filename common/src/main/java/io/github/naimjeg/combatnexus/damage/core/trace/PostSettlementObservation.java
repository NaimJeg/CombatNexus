package io.github.naimjeg.combatnexus.damage.core.trace;

/** Immutable values observed at NeoForge Post plus state deltas since Pre. */
public record PostSettlementObservation(
        float postInflictedDamage,
        float postHealthDamage,
        float absorptionReduction,
        float healthAfter,
        float absorptionAfter,
        int invulnerableTimeAfter
) {
    public float healthDelta(DamageModuleTransaction tx) {
        return Math.max(0.0f, tx.victimHealthBefore() - healthAfter);
    }

    public float absorptionDelta(DamageModuleTransaction tx) {
        return Math.max(0.0f, tx.victimAbsorptionBefore() - absorptionAfter);
    }

    public float observedTotalDelta(DamageModuleTransaction tx) {
        return healthDelta(tx) + absorptionDelta(tx);
    }
}
