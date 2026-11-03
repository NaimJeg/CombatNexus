package io.github.naimjeg.combatnexus.config;

import com.google.gson.*;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Bounded scalar/list vocabulary for domain definitions. No executable expressions. */
public final class DefinitionJson {
    private DefinitionJson() {}
    public static double number(JsonObject json, String key, double min, double max) {
        var value = json.getAsJsonPrimitive(key);
        if (value == null || !value.isNumber()) throw new IllegalArgumentException("Expected number: " + key);
        return range(value.getAsDouble(), min, max, key);
    }
    public static double range(double value, double min, double max, String key) {
        if (!Double.isFinite(value) || value < min || value > max) throw new IllegalArgumentException("Out of range: " + key);
        return value;
    }
    public static int ticks(JsonObject json, String key) {
        int value = SettingsJson.integer(json, key); range(value, 1, 1200000, key); return value;
    }
    public static String string(JsonObject json, String key) {
        var value = json.getAsJsonPrimitive(key);
        if (value == null || !value.isString() || value.getAsString().length() > 256)
            throw new IllegalArgumentException("Invalid string: " + key);
        return value.getAsString();
    }
    public static Identifier id(JsonObject json, String key) { return Identifier.parse(string(json, key)); }
    public static JsonArray array(JsonObject json, String key, int maximum) {
        var value = json.getAsJsonArray(key);
        if (value == null || value.size() > maximum) throw new IllegalArgumentException("Array budget exceeded: " + key);
        return value;
    }
    public static Set<Identifier> ids(JsonObject json, String key) {
        return Set.copyOf(orderedIds(json, key));
    }
    public static List<Identifier> orderedIds(JsonObject json, String key) {
        Set<Identifier> result = new LinkedHashSet<>();
        for (var value : array(json, key, 32)) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || value.getAsString().length() > 256)
                throw new IllegalArgumentException("Invalid identifier in " + key);
            if (!result.add(Identifier.parse(value.getAsString()))) throw new IllegalArgumentException("Duplicate identifier in " + key);
        }
        return List.copyOf(result);
    }
}
