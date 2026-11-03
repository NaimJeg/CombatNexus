package io.github.naimjeg.combatnexus.formula;

/** Armor-only penetration: choose the larger of proportional and flat removal, floored at zero. */
public final class PenetrationFormula {
    private PenetrationFormula() {}
    public record Result(double originalArmor, double proportionalRemoval, double penetration, double effectiveArmor) {}

    /** Does not change toughness/resistance or decide whether a damage component bypasses armor. */
    public static Result calculate(double armor, double proportional, double flat) {
        Amounts.nonNegative(armor, "armor");
        Amounts.nonNegative(proportional, "proportional");
        Amounts.nonNegative(flat, "flat");
        if (proportional > 1) throw new IllegalArgumentException("Penetration ratio exceeds one");
        double proportionalRemoval = armor * proportional;
        double penetration = Math.max(proportionalRemoval, flat);
        return new Result(armor, proportionalRemoval, penetration, Math.max(0, armor - penetration));
    }
}
