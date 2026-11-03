package io.github.naimjeg.combatnexus.combat.config;

import com.google.gson.JsonObject;
import io.github.naimjeg.combatnexus.combat.damage.config.VanillaCompatibilitySettings;
import io.github.naimjeg.combatnexus.combat.damage.config.VanillaReductionCompatibilityMode;
import io.github.naimjeg.combatnexus.config.SettingsJson;
import io.github.naimjeg.combatnexus.formula.CombatFormulaSettings;
import java.util.Objects;

/** One validated server rules snapshot; local logging and UI preferences are separate. */
public record CombatServerSettings(CombatFormulaSettings formulas,
                                   VanillaCompatibilitySettings vanillaCompatibility,
                                   CombatSafetySettings safety) {
    public CombatServerSettings {
        Objects.requireNonNull(formulas); Objects.requireNonNull(vanillaCompatibility); Objects.requireNonNull(safety);
        Objects.requireNonNull(vanillaCompatibility.mode());
    }
    public static CombatServerSettings decode(JsonObject formulas, JsonObject damage) {
        SettingsJson.fields(formulas, "asymptotic_k", "resistance_k", "rating_per_protection_score");
        SettingsJson.fields(damage, "vanilla_reductions", "limits");
        JsonObject vanilla = SettingsJson.fields(damage.getAsJsonObject("vanilla_reductions"),
                "mode", "suppress_armor", "suppress_enchantments", "suppress_mob_effects", "suppress_innate_resistance");
        JsonObject limits = SettingsJson.fields(damage.getAsJsonObject("limits"),
                "max_recursion_depth", "max_derived_requests_per_root", "max_managed_requests_per_server_tick");
        return new CombatServerSettings(
                new CombatFormulaSettings(SettingsJson.number(formulas, "asymptotic_k"),
                        SettingsJson.number(formulas, "resistance_k"), SettingsJson.number(formulas, "rating_per_protection_score")),
                new VanillaCompatibilitySettings(SettingsJson.enumeration(vanilla, "mode", VanillaReductionCompatibilityMode.class),
                        SettingsJson.bool(vanilla, "suppress_armor"), SettingsJson.bool(vanilla, "suppress_enchantments"),
                        SettingsJson.bool(vanilla, "suppress_mob_effects"), SettingsJson.bool(vanilla, "suppress_innate_resistance")),
                new CombatSafetySettings(SettingsJson.integer(limits, "max_recursion_depth"),
                        SettingsJson.integer(limits, "max_derived_requests_per_root"),
                        SettingsJson.integer(limits, "max_managed_requests_per_server_tick")));
    }
}
