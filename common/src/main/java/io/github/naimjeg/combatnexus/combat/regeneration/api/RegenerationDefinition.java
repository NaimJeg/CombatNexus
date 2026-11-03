package io.github.naimjeg.combatnexus.combat.regeneration.api;
import com.google.gson.JsonObject;
import io.github.naimjeg.combatnexus.config.*;
/** Installation-time recovery policy; vanilla timers retain their own scheduling. */
public record RegenerationDefinition(double healthPerSecond, long damageDelayTicks,
        boolean respectsNaturalRegenerationRule, boolean requiresOutOfCombat, int durationTicks) {
    public RegenerationDefinition {
        DefinitionJson.range(healthPerSecond, 0, 1000000, "health_per_second");
        DefinitionJson.range(damageDelayTicks, 0, 1200000, "damage_delay_ticks");
        DefinitionJson.range(durationTicks, 1, 1200000, "duration_ticks");
    }
    public static RegenerationDefinition decode(JsonObject json) {
        SettingsJson.fields(json, "health_per_second", "damage_delay_ticks", "respects_natural_regeneration_rule", "requires_out_of_combat", "duration_ticks");
        return new RegenerationDefinition(DefinitionJson.number(json, "health_per_second", 0, 1000000), SettingsJson.integer(json, "damage_delay_ticks"),
                SettingsJson.bool(json, "respects_natural_regeneration_rule"), SettingsJson.bool(json, "requires_out_of_combat"), DefinitionJson.ticks(json, "duration_ticks"));
    }
}
