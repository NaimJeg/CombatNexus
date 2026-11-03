package io.github.naimjeg.combatnexus.formula;

/** Vanilla witch innate reduction, independent of armor and resistance-effect policy. */
public final class InnateResistanceFormula {
    private InnateResistanceFormula() {}
    public static float reduction(float amount, boolean selfDamage, boolean resistantType) {
        Amounts.nonNegative(amount, "amount");
        return selfDamage ? amount : resistantType ? amount * 0.85f : 0;
    }
}
