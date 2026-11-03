package io.github.naimjeg.combatnexus.formula;

/**
 * Baked combat formula parameters. Resistance uses rating / (rating + K) for
 * non-negative ratings and rating / K for negative vulnerability ratings.
 */
public record CombatFormulaSettings(
        float asymptoticKValue,
        float resistanceKValue,
        float ratingPerProtScore
) {
    public CombatFormulaSettings {
        requireRange("asymptoticKValue", asymptoticKValue, 1, 1000);
        requireRange("resistanceKValue", resistanceKValue, 1, 1000);
        requireRange("ratingPerProtScore", ratingPerProtScore, 0, 100);
    }
    private static void requireRange(String name, float value, float min, float max) {
        if (!Float.isFinite(value) || value < min || value > max)
            throw new IllegalArgumentException(name + " must be finite and in [" + min + ", " + max + "]");
    }
    public static CombatFormulaSettings defaults() {
        return new CombatFormulaSettings(
                15.0f,
                50.0f,
                3.5f
        );
    }
}
