package io.github.naimjeg.combatnexus.combat.internal.persistence;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;
import io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent;

/** Current-format entity storage codec. JDK optional types and polymorphic components have explicit adapters. */
public final class CombatStateCodec {
    private CombatStateCodec() {}
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Identifier.class, new IdentifierAdapter())
        .registerTypeAdapter(OptionalLong.class, new OptionalLongAdapter())
        .registerTypeAdapter(CombatSource.class, new SourceAdapter())
        .registerTypeAdapter(io.github.naimjeg.combatnexus.combat.api.source.SourceInstance.class, new SourceInstanceAdapter())
        .registerTypeAdapter(BehaviorComponent.class, new ComponentAdapter()).create();
    public static String encode(CombatPersistentState state) {
        String value = GSON.toJson(state);
        if (value.length() > 1048576) throw new IllegalArgumentException("Combat save exceeds 1 MiB character budget");
        return value;
    }
    public static CombatPersistentState decode(String value) {
        if (value.length() > 1048576) throw new IllegalArgumentException("Combat save exceeds budget");
        var reader = new com.google.gson.stream.JsonReader(new java.io.StringReader(value));
        reader.setStrictness(Strictness.STRICT);
        var json = JsonParser.parseReader(reader).getAsJsonObject();
        io.github.naimjeg.combatnexus.config.SettingsJson.fields(json, "target", "savedTick", "persistentSources",
                "effects", "behaviors", "shields", "recovery", "equipment", "sources");
        CombatPersistentState state = GSON.fromJson(json, CombatPersistentState.class);
        try { if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Trailing combat save content"); }
        catch (java.io.IOException failure) { throw new IllegalArgumentException("Invalid combat save", failure); }
        return Objects.requireNonNull(state);
    }
    private static final class IdentifierAdapter implements JsonSerializer<Identifier>, JsonDeserializer<Identifier> {
        public JsonElement serialize(Identifier value, java.lang.reflect.Type type, JsonSerializationContext context) { return new JsonPrimitive(value.toString()); }
        public Identifier deserialize(JsonElement value, java.lang.reflect.Type type, JsonDeserializationContext context) { return Identifier.parse(value.getAsString()); }
    }
    private static final class OptionalLongAdapter implements JsonSerializer<OptionalLong>, JsonDeserializer<OptionalLong> {
        public JsonElement serialize(OptionalLong value, java.lang.reflect.Type type, JsonSerializationContext context) { return new JsonPrimitive(value.orElse(-1)); }
        public OptionalLong deserialize(JsonElement value, java.lang.reflect.Type type, JsonDeserializationContext context) {
            long number = value.getAsLong(); if (number < -1) throw new IllegalArgumentException("Invalid saved deadline");
            return number == -1 ? OptionalLong.empty() : OptionalLong.of(number);
        }
    }
    private static final class SourceAdapter implements JsonSerializer<CombatSource>, JsonDeserializer<CombatSource> {
        public JsonElement serialize(CombatSource value, java.lang.reflect.Type type, JsonSerializationContext context) {
            var result = new JsonObject(); result.addProperty("id", value.id().toString());
            value.owner().ifPresent(owner -> result.addProperty("owner", owner.toString())); return result;
        }
        public CombatSource deserialize(JsonElement value, java.lang.reflect.Type type, JsonDeserializationContext context) {
            var object = value.getAsJsonObject();
            return new CombatSource(Identifier.parse(object.get("id").getAsString()), object.has("owner")
                    ? Optional.of(UUID.fromString(object.get("owner").getAsString())) : Optional.empty(), Optional.empty());
        }
    }
    private static final class SourceInstanceAdapter implements JsonSerializer<io.github.naimjeg.combatnexus.combat.api.source.SourceInstance>, JsonDeserializer<io.github.naimjeg.combatnexus.combat.api.source.SourceInstance> {
        public JsonElement serialize(io.github.naimjeg.combatnexus.combat.api.source.SourceInstance value, java.lang.reflect.Type type, JsonSerializationContext context) {
            var json = new JsonObject(); json.addProperty("instance", value.instance().toString()); json.addProperty("provider", value.provider().toString()); json.addProperty("slot", value.slot());
            value.owner().ifPresent(owner -> json.addProperty("owner", owner.toString())); return json;
        }
        public io.github.naimjeg.combatnexus.combat.api.source.SourceInstance deserialize(JsonElement value, java.lang.reflect.Type type, JsonDeserializationContext context) {
            var json = value.getAsJsonObject(); return new io.github.naimjeg.combatnexus.combat.api.source.SourceInstance(UUID.fromString(json.get("instance").getAsString()), Identifier.parse(json.get("provider").getAsString()),
                json.has("owner") ? Optional.of(UUID.fromString(json.get("owner").getAsString())) : Optional.empty(), json.get("slot").getAsString());
        }
    }
    private static final class ComponentAdapter implements JsonSerializer<BehaviorComponent>, JsonDeserializer<BehaviorComponent> {
        public JsonElement serialize(BehaviorComponent value, java.lang.reflect.Type type, JsonSerializationContext context) {
            return io.github.naimjeg.combatnexus.combat.internal.registration.BehaviorTypes.encode(value);
        }
        public BehaviorComponent deserialize(JsonElement value, java.lang.reflect.Type type, JsonDeserializationContext context) {
            return io.github.naimjeg.combatnexus.combat.internal.registration.BehaviorTypes.decode(value.getAsJsonObject());
        }
    }
}
