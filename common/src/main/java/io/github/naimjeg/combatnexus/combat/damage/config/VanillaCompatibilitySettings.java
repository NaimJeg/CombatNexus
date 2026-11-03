package io.github.naimjeg.combatnexus.combat.damage.config;

public record VanillaCompatibilitySettings(
        VanillaReductionCompatibilityMode mode,
        boolean rawSuppressArmor,
        boolean rawSuppressEnchantments,
        boolean rawSuppressMobEffects,
        boolean rawSuppressInnateResistance
) {
    public VanillaCompatibilitySettings {
        java.util.Objects.requireNonNull(mode, "mode");
    }

    /** Native mechanisms only; custom channel resistance remains an independent contribution. */
    public enum Reduction { ARMOR, ENCHANTMENTS, MOB_EFFECTS, INNATE_RESISTANCE }
    public enum Executor { VANILLA, COMBATNEXUS, DISABLED }

    /** One decision shared by calculation and native reduction suppression. */
    public Executor executor(Reduction reduction) {
        java.util.Objects.requireNonNull(reduction, "reduction");
        boolean suppress = switch (mode) {
            case FULL_REPLACEMENT -> true;
            case COOPERATIVE -> false;
            case CONFIGURABLE -> switch (reduction) {
                case ARMOR -> rawSuppressArmor;
                case ENCHANTMENTS -> rawSuppressEnchantments;
                case MOB_EFFECTS -> rawSuppressMobEffects;
                case INNATE_RESISTANCE -> rawSuppressInnateResistance;
            };
        };
        if (!suppress) return Executor.VANILLA;
        return Executor.COMBATNEXUS;
    }

    public static VanillaCompatibilitySettings defaults() {
        return new VanillaCompatibilitySettings(
                VanillaReductionCompatibilityMode.FULL_REPLACEMENT,
                true,
                true,
                true,
                true
        );
    }

    public boolean shouldSuppressArmor() {
        return executor(Reduction.ARMOR) != Executor.VANILLA;
    }

    public boolean shouldSuppressEnchantments() {
        return executor(Reduction.ENCHANTMENTS) != Executor.VANILLA;
    }

    public boolean shouldSuppressMobEffects() {
        return executor(Reduction.MOB_EFFECTS) != Executor.VANILLA;
    }

    public boolean shouldSuppressInnateResistance() {
        return executor(Reduction.INNATE_RESISTANCE) != Executor.VANILLA;
    }
}
