package io.github.naimjeg.combatnexus.formula;

/** Validation shared by settlement contracts; does not silently repair invalid requests. */
public final class Amounts {
    private Amounts() {}

    public static double nonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and nonnegative");
        }
        return value;
    }
}
