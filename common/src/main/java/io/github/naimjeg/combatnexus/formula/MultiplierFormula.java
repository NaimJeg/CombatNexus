package io.github.naimjeg.combatnexus.formula;

import java.util.function.IntToDoubleFunction;

/** Ordered float arithmetic shared by combat domains. No registry or entity ownership. */
public final class MultiplierFormula {
    private MultiplierFormula() {}

    /** Add within each bucket, multiply across buckets; preserve float rounding and zero skips. */
    public static float applyBuckets(float amount, int count,
                                     IntToDoubleFunction first, IntToDoubleFunction second) {
        for (int i = 0; i < count; i++) {
            float value = 0.0f;
            value += (float) first.applyAsDouble(i);
            value += (float) second.applyAsDouble(i);
            if (value != 0.0f) amount *= 1.0f + value;
        }
        return amount;
    }

    public static float applyAdditive(float amount, int count, IntToDoubleFunction values) {
        for (int i = 0; i < count; i++) amount *= 1.0f + (float) values.applyAsDouble(i);
        return amount;
    }

    /** Negative reductions remain vulnerability; reductions above one cannot flip the sign. */
    public static float applyReductions(float amount, int count, IntToDoubleFunction values) {
        for (int i = 0; i < count; i++) {
            amount *= Math.max(0.0f, 1.0f - (float) values.applyAsDouble(i));
        }
        return amount;
    }

    /** Apply only at the existing stage boundary, never between factors. */
    public static float nonNegativeFinite(float amount) {
        return Float.isFinite(amount) ? Math.max(0.0f, amount) : 0.0f;
    }

    public static float resistanceEffectReduction(int amplifier) {
        return Math.max(0.0f, Math.min(1.0f, (amplifier + 1) * 0.20f));
    }
}
