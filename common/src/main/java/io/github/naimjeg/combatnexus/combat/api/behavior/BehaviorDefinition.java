package io.github.naimjeg.combatnexus.combat.api.behavior;
import com.google.gson.JsonObject;
import io.github.naimjeg.combatnexus.config.DefinitionJson;
import io.github.naimjeg.combatnexus.config.SettingsJson;
import java.util.*;
/** Definitions remain inert until explicitly installed. */
public record BehaviorDefinition(BehaviorPlan plan, int durationTicks) {
    public BehaviorDefinition {
        Objects.requireNonNull(plan); DefinitionJson.range(durationTicks, 1, 1200000, "duration_ticks");
    }
    public static BehaviorDefinition decode(JsonObject json) {
        SettingsJson.fields(json, "priority", "duration_ticks", "components");
        List<BehaviorComponent> components = new ArrayList<>();
        for (var value : DefinitionJson.array(json, "components", 64)) components.add(BehaviorComponent.decode(value.getAsJsonObject()));
        return new BehaviorDefinition(new BehaviorPlan(SettingsJson.integer(json, "priority"), components), DefinitionJson.ticks(json, "duration_ticks"));
    }
}
