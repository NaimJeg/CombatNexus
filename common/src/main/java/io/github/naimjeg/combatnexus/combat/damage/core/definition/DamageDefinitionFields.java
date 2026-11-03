package io.github.naimjeg.combatnexus.combat.damage.core.definition;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import io.github.naimjeg.combatnexus.combat.damage.registry.rule.*;
import net.minecraft.resources.Identifier;
import java.util.*;
/** Reject misspelled fields before codecs apply their defaults; extension codecs declare their own keys. */
public final class DamageDefinitionFields {
    private DamageDefinitionFields() {}
    public static void validate(String directory, JsonObject json) {
        switch (directory) {
            case "combatnexus_channels" -> keys(json, "channel", "trigger_tags", "resistance_attribute", "affected_by_armor", "priority");
            case "combatnexus_rules" -> rule(json);
            case "combatnexus_entry_templates" -> entry(json);
            case "combatnexus_affix_templates" -> {
                keys(json, "id", "display", "slot", "rarity", "entries", "stacking", "stacking_group");
                if (json.has("display")) display(json.getAsJsonObject("display"));
                if (json.has("entries")) for (var value : json.getAsJsonArray("entries")) entry(value.getAsJsonObject());
            }
            default -> throw new IllegalArgumentException("Unknown definition directory: " + directory);
        }
    }
    private static void entry(JsonObject json) {
        keys(json, "id", "display", "slot", "rules", "stacking", "stacking_group");
        if (json.has("display")) display(json.getAsJsonObject("display"));
        if (json.has("rules")) for (var value : json.getAsJsonArray("rules")) rule(value.getAsJsonObject());
    }
    private static void display(JsonObject json) { keys(json, "name", "authored_summary", "flavor_text", "show_rule_breakdown"); }
    private static void rule(JsonObject json) {
        keys(json, "id", "role", "phase", "priority", "conditions", "operations", "stacking", "stacking_group");
        if (json.has("conditions")) for (var value : json.getAsJsonArray("conditions")) condition(value.getAsJsonObject());
        if (json.has("operations")) for (var value : json.getAsJsonArray("operations")) {
            var object = value.getAsJsonObject();
            var codec = DamageRuleOperationTypes.codec(Identifier.parse(object.get("type").getAsString()));
            typedKeys(object, codec.keys(JsonOps.INSTANCE).map(JsonElement::getAsString).toList());
        }
    }
    private static void condition(JsonObject json) {
        var type = Identifier.parse(json.get("type").getAsString());
        typedKeys(json, DamageRuleConditionTypes.codec(type).keys(JsonOps.INSTANCE).map(JsonElement::getAsString).toList());
        // Built-in composite children. Extension payload semantics belong to their registered codec.
        if (type.equals(Identifier.fromNamespaceAndPath("combatnexus", "all_of")) || type.equals(Identifier.fromNamespaceAndPath("combatnexus", "any_of")))
            for (var value : json.getAsJsonArray("conditions")) condition(value.getAsJsonObject());
        if (type.equals(Identifier.fromNamespaceAndPath("combatnexus", "not"))) condition(json.getAsJsonObject("condition"));
    }
    private static void typedKeys(JsonObject json, List<String> fields) {
        var allowed = new HashSet<>(fields); allowed.add("type");
        if (!allowed.containsAll(json.keySet())) throw new IllegalArgumentException("Unknown typed definition field: " + json.keySet());
    }
    private static void keys(JsonObject json, String... fields) {
        if (!Set.of(fields).containsAll(json.keySet())) throw new IllegalArgumentException("Unknown definition field: " + json.keySet());
    }
}
