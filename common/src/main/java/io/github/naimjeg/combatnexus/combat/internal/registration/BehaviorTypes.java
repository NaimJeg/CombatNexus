package io.github.naimjeg.combatnexus.combat.internal.registration;
import io.github.naimjeg.combatnexus.combat.api.behavior.*;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.util.*;
import net.minecraft.resources.Identifier;

/** Startup-owned codec table. No Java class name is written to disk. */
public final class BehaviorTypes {
    private static final Map<Identifier, BehaviorType<?>> TYPES = new LinkedHashMap<>();
    private static final Map<Class<?>, BehaviorType<?>> CLASSES = new HashMap<>();
    private static boolean frozen;
    static { BuiltinBehaviorTypes.register(); }
    private BehaviorTypes() {}
    public static synchronized <T extends BehaviorComponent> void register(BehaviorType<T> type) {
        if (frozen) throw new IllegalStateException("Behavior types frozen");
        if (TYPES.containsKey(type.id()) || CLASSES.containsKey(type.implementation())) throw new IllegalArgumentException("Duplicate behavior type: " + type.id());
        TYPES.put(type.id(), type); CLASSES.put(type.implementation(), type);
    }
    public static BehaviorType<?> registered(Identifier id) { return Objects.requireNonNull(TYPES.get(id), "Unknown behavior type: " + id); }
    public static void freeze() { frozen = true; }
    public static BehaviorType<?> type(BehaviorComponent component) {
        return Objects.requireNonNull(CLASSES.get(component.getClass()), "Unregistered behavior implementation");
    }
    public static BehaviorComponent decode(JsonObject json) {
        var id = Identifier.parse(json.get("type").getAsString());
        var type = Objects.requireNonNull(TYPES.get(id), "Unknown behavior type: " + id);
        var parameters = json.deepCopy(); parameters.remove("type");
        var fields = type.codec().keys(JsonOps.INSTANCE).map(JsonElement::getAsString).collect(java.util.stream.Collectors.toSet());
        if (!fields.containsAll(parameters.keySet())) throw new IllegalArgumentException("Unknown behavior parameters: " + id);
        return type.codec().codec().parse(JsonOps.INSTANCE, parameters).getOrThrow();
    }
    public static JsonObject encode(BehaviorComponent component) { return encode(type(component), component); }
    private static <T extends BehaviorComponent> JsonObject encode(BehaviorType<T> type, BehaviorComponent component) {
        var json = type.codec().codec().encodeStart(JsonOps.INSTANCE, type.implementation().cast(component)).getOrThrow().getAsJsonObject();
        json.addProperty("type", type.id().toString()); return json;
    }
}
