package io.github.naimjeg.combatnexus.damage.config;

public record DamageModuleConfigValues(
        DeveloperSettings developer,
        DiagnosticsSettings diagnostics,
        TooltipSettings tooltips,
        CombatFormulaSettings formulas,
        VanillaCompatibilitySettings vanillaCompatibility,
        DamageSafetySettings safety
) {
    public static DamageModuleConfigValues defaults() {
        return new DamageModuleConfigValues(
                DeveloperSettings.defaults(),
                DiagnosticsSettings.defaults(),
                TooltipSettings.defaults(),
                CombatFormulaSettings.defaults(),
                VanillaCompatibilitySettings.defaults(),
                DamageSafetySettings.defaults()
        );
    }
}
