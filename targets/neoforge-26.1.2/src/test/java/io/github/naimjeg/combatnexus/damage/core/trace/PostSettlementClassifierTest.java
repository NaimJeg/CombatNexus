package io.github.naimjeg.combatnexus.damage.core.trace;

import io.github.naimjeg.combatnexus.damage.config.VanillaReductionCompatibilityMode;
import org.junit.jupiter.api.Test;

import static io.github.naimjeg.combatnexus.damage.core.trace.PostSettlementClassifier.Kind;
import static io.github.naimjeg.combatnexus.damage.core.trace.PostSettlementClassifier.Severity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostSettlementClassifierTest {

    @Test
    void cooperativeArmorReductionIsExpectedAdjustment() {
        DamageModuleTransaction tx = tx(
                10, 6, 20, 0, 0,
                VanillaReductionCompatibilityMode.COOPERATIVE,
                false, false, false, false,
                0, 4, 0, 0, 0
        );

        var result = classify(tx, post(6, 6, 0, 14, 0, 0));

        assertEquals(Severity.EXPECTED_ADJUSTMENT, result.severity());
        assertEquals(Kind.VANILLA_OR_EXTERNAL_ADJUSTMENT, result.kind());
        assertFalse(result.mismatch());
    }

    @Test
    void configurableUnsuppressedReductionIsExpected() {
        DamageModuleTransaction tx = tx(
                10, 7, 20, 0, 0,
                VanillaReductionCompatibilityMode.CONFIGURABLE,
                false, true, true, true,
                0, 3, 0, 0, 0
        );

        assertEquals(
                Severity.EXPECTED_ADJUSTMENT,
                classify(tx, post(7, 7, 0, 13, 0, 0)).severity()
        );
    }

    @Test
    void fullySuppressedUnexplainedPreChangeIsMismatchCandidate() {
        DamageModuleTransaction tx = tx(
                10, 8, 20, 0, 0,
                VanillaReductionCompatibilityMode.CONFIGURABLE,
                true, true, true, true,
                0, 0, 0, 0, 0
        );

        var result = classify(tx, post(8, 8, 0, 12, 0, 0));
        assertTrue(result.mismatch());
        assertEquals(Kind.UNEXPLAINED_PRE_STAGE_CHANGE, result.kind());
    }

    @Test
    void absorptionAndOverkillAreExpected() {
        DamageModuleTransaction absorbed = tx(
                10, 10, 20, 5, 0,
                VanillaReductionCompatibilityMode.FULL_REPLACEMENT,
                true, true, true, true,
                0, 0, 0, 0, 0
        );
        assertEquals(
                Kind.ABSORPTION,
                classify(absorbed, post(10, 5, 5, 15, 0, 0)).kind()
        );

        DamageModuleTransaction overkill = tx(
                10, 10, 4, 0, 0,
                VanillaReductionCompatibilityMode.FULL_REPLACEMENT,
                true, true, true, true,
                0, 0, 0, 0, 0
        );
        assertEquals(
                Kind.OVERKILL_CAP,
                classify(overkill, post(10, 10, 0, 0, 0, 0)).kind()
        );
    }

    @Test
    void invulnerabilityLateZeroAndBatchedDeltaAreExpected() {
        DamageModuleTransaction invulnerability = tx(
                10, 6, 20, 0, 12,
                VanillaReductionCompatibilityMode.FULL_REPLACEMENT,
                true, true, true, true,
                4, 0, 0, 0, 0
        );
        assertEquals(
                Kind.VANILLA_INVULNERABILITY_ADJUSTMENT,
                classify(invulnerability, post(6, 6, 0, 14, 0, 12)).kind()
        );

        DamageModuleTransaction lateZero = tx(
                0, 0, 20, 0, 0,
                VanillaReductionCompatibilityMode.COOPERATIVE,
                false, false, false, false,
                0, 0, 0, 0, 0
        );
        assertEquals(
                Kind.LATE_ZERO_DAMAGE,
                classify(lateZero, post(0, 0, 0, 20, 0, 0)).kind()
        );

        DamageModuleTransaction batched = tx(
                5, 5, 20, 0, 5,
                VanillaReductionCompatibilityMode.COOPERATIVE,
                false, false, false, false,
                0, 0, 0, 0, 0
        );
        assertEquals(
                Kind.BATCHED_OBSERVED_DELTA,
                classify(batched, post(5, 5, 0, 12, 0, 5)).kind()
        );
    }

    @Test
    void unexplainedPostDeltaIsMismatch() {
        DamageModuleTransaction tx = tx(
                5, 5, 20, 0, 0,
                VanillaReductionCompatibilityMode.COOPERATIVE,
                false, false, false, false,
                0, 0, 0, 0, 0
        );

        assertEquals(
                Kind.POST_SETTLEMENT_DELTA,
                classify(tx, post(5, 5, 0, 18, 0, 0)).kind()
        );
    }

    private static PostSettlementClassifier.Result classify(
            DamageModuleTransaction tx,
            PostSettlementObservation post
    ) {
        return PostSettlementClassifier.classify(tx, post);
    }

    private static PostSettlementObservation post(
            float inflicted,
            float healthDamage,
            float absorptionReduction,
            float healthAfter,
            float absorptionAfter,
            int invulnerabilityAfter
    ) {
        return new PostSettlementObservation(
                inflicted,
                healthDamage,
                absorptionReduction,
                healthAfter,
                absorptionAfter,
                invulnerabilityAfter
        );
    }

    private static DamageModuleTransaction tx(
            float incomingAfter,
            float preDamage,
            float healthBefore,
            float absorptionBefore,
            int invulnerabilityBefore,
            VanillaReductionCompatibilityMode mode,
            boolean suppressArmor,
            boolean suppressEnchantments,
            boolean suppressMobEffects,
            boolean suppressInnate,
            float invulnerabilityReduction,
            float armorReduction,
            float enchantmentReduction,
            float mobEffectReduction,
            float innateReduction
    ) {
        return new DamageModuleTransaction(
                1,
                null,
                null,
                null,
                incomingAfter,
                incomingAfter,
                incomingAfter,
                incomingAfter,
                incomingAfter,
                incomingAfter,
                preDamage,
                0,
                invulnerabilityReduction,
                armorReduction,
                enchantmentReduction,
                mobEffectReduction,
                innateReduction,
                false,
                healthBefore,
                absorptionBefore,
                invulnerabilityBefore,
                100,
                mode,
                suppressArmor,
                suppressEnchantments,
                suppressMobEffects,
                suppressInnate
        );
    }
}
