package io.github.naimjeg.combatnexus.formula;

/** Deterministic rating math; no entity, registry or pipeline dependency. */
public final class ResistanceFormula {
    private ResistanceFormula() {}
    public static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    public static float totalRating(
            float channelRating,
            float temporaryRating,
            float categoryRating
    ) {
        double total = (double) finiteOrZero(channelRating)
                + finiteOrZero(temporaryRating)
                + finiteOrZero(categoryRating);
        if (total >= Float.MAX_VALUE) {
            return Float.MAX_VALUE;
        }
        if (total <= -Float.MAX_VALUE) {
            return -Float.MAX_VALUE;
        }
        return (float) total;
    }

    public static float reductionFor(float rating, float kValue) {
        double safeRating = finiteOrZero(rating);
        double safeK = Float.isFinite(kValue)
                ? Math.max(0.0001d, kValue)
                : 0.0001d;
        double reduction = safeRating >= 0.0d
                ? safeRating / (safeRating + safeK)
                : safeRating / safeK;

        // Preserve the positive safety cap while allowing negative ratings to
        // express unbounded vulnerability within the finite float domain.
        return (float) Math.max(
                -Float.MAX_VALUE,
                Math.min(0.95d, reduction)
        );
    }
}
