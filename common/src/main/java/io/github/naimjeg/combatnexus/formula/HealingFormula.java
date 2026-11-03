package io.github.naimjeg.combatnexus.formula;

/** Multipliers are ratios, applied once in source -> recipient order. */
public final class HealingFormula {
    private HealingFormula() {}

    public static double modified(double amount, double outgoing, double received) {
        Amounts.nonNegative(amount, "amount");
        Amounts.nonNegative(outgoing, "outgoing");
        Amounts.nonNegative(received, "received");
        double sourceAdjusted = Amounts.nonNegative(amount * outgoing, "source adjusted healing");
        return Amounts.nonNegative(sourceAdjusted * received, "modified healing");
    }
}
