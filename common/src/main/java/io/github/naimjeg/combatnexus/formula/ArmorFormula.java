package io.github.naimjeg.combatnexus.formula;
/** Existing armor math, extracted without changing operation order or caps. */
public final class ArmorFormula {
    private ArmorFormula() {}
    public record Result(float effectiveArmor, float reduction) {}
    public static Result calculate(float armor, float damage, float toughnessFactor, float kValue) {
        float effectiveArmor = Math.max(0.0f, armor - (damage / toughnessFactor));
        float reduction = effectiveArmor / (effectiveArmor + kValue);
        return new Result(effectiveArmor, Math.max(0.0f, Math.min(0.995f, reduction)));
    }
}
