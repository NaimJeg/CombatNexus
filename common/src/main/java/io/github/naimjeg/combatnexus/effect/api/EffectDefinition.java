package io.github.naimjeg.combatnexus.effect.api;
import com.google.gson.JsonObject;
import io.github.naimjeg.combatnexus.attribute.api.AttributeSnapshot;
import io.github.naimjeg.combatnexus.combat.api.behavior.*;
import io.github.naimjeg.combatnexus.config.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.util.*;
/** Complete managed definition compiled before adoption; no live repository retained by instances. */
public record EffectDefinition(int level, EffectLifetime lifetime, Identifier stacking, EffectPolicy policy,
        List<EffectInstance.AttributeContribution> attributes, BehaviorPlan behavior, Set<EffectSnapshot.RestrictedAction> restrictions, EffectDisplay display) {
    public EffectDefinition(int level, EffectLifetime lifetime, Identifier stacking, EffectPolicy policy,
            List<EffectInstance.AttributeContribution> attributes, BehaviorPlan behavior, Set<EffectSnapshot.RestrictedAction> restrictions) {
        this(level, lifetime, stacking, policy, attributes, behavior, restrictions, EffectDisplay.NONE);
    }
    public EffectDefinition {
        DefinitionJson.range(level, 0, 255, "level"); Objects.requireNonNull(lifetime);
        Objects.requireNonNull(display);
        Objects.requireNonNull(stacking); Objects.requireNonNull(policy); Objects.requireNonNull(behavior);
        io.github.naimjeg.combatnexus.effect.internal.EffectStackingStrategies.get(stacking);
        restrictions = Set.copyOf(restrictions);
        attributes = List.copyOf(attributes);
        if (lifetime.kind() == EffectLifetime.Kind.INSTANT) {
            if (!attributes.isEmpty() || !restrictions.isEmpty()
                    || behavior.components().stream().anyMatch(c -> !(c instanceof InstantBehavior)))
                throw new IllegalArgumentException("Instant definitions can only contain instant behaviors");
        } else if (behavior.components().stream().anyMatch(c -> c instanceof InstantBehavior)) {
            throw new IllegalArgumentException("Instant behaviors require an instant definition");
        }
        if (attributes.size() > 32) throw new IllegalArgumentException("Effect attribute budget exceeded");
        var identities = new HashSet<String>();
        for (var value : attributes) {
            if (BuiltInRegistries.ATTRIBUTE.get(value.attribute()).isEmpty()) throw new IllegalArgumentException("Unknown attribute: " + value.attribute());
            if (!identities.add(value.attribute() + "/" + value.modifier().source())) throw new IllegalArgumentException("Duplicate effect modifier");
        }
    }
    public EffectDefinition(int level, int durationTicks, EffectService.Stacking stacking, EffectPolicy policy,
            List<EffectInstance.AttributeContribution> attributes, BehaviorPlan behavior, Set<EffectSnapshot.RestrictedAction> restrictions) {
        this(level, EffectLifetime.timed(durationTicks), stacking, policy, attributes, behavior, restrictions);
    }
    public EffectDefinition(int level, EffectLifetime lifetime, EffectService.Stacking stacking, EffectPolicy policy,
            List<EffectInstance.AttributeContribution> attributes, BehaviorPlan behavior, Set<EffectSnapshot.RestrictedAction> restrictions) {
        this(level, lifetime, stacking.id(), policy, attributes, behavior, restrictions);
    }
    public EffectInstance instantiate(UUID target, Identifier effect, Identifier source, long now) {
        return new EffectInstance(new EffectSnapshot(target, effect, source, level, lifetime.deadline(now), restrictions, lifetime.kind()),
                attributes, EffectInstance.Ownership.MANAGED, behavior, policy, List.of(),
                display.nativeId() == null ? io.github.naimjeg.combatnexus.effect.internal.EffectMappings.display(effect).withProvenance(display.provenance()) : display, stacking);
    }
    public static EffectDefinition decode(JsonObject json, Map<Identifier, BehaviorDefinition> behaviors) {
        json = json.deepCopy();
        if (!json.has("tags")) json.add("tags", new com.google.gson.JsonArray());
        if (!json.has("display")) json.add("display", com.google.gson.JsonNull.INSTANCE);
        SettingsJson.fields(json, "level", "lifetime", "stacking", "category", "dispellable", "attributes", "behaviors", "priority", "restrictions", "tags", "display");
        var display = EffectDisplay.NONE;
        if (!json.get("display").isJsonNull()) {
            var value = SettingsJson.fields(json.getAsJsonObject("display"), "native_id", "ambient", "particles", "icon", "aggregate_sources", "persistent", "environment");
            display = new EffectDisplay(DefinitionJson.id(value, "native_id"), SettingsJson.bool(value, "ambient"), SettingsJson.bool(value, "particles"),
                SettingsJson.bool(value, "icon"), null, SettingsJson.bool(value, "aggregate_sources"), SettingsJson.bool(value, "persistent"), SettingsJson.bool(value, "environment"));
        }
        var restrictions = EnumSet.noneOf(EffectSnapshot.RestrictedAction.class);
        for (var value : DefinitionJson.array(json, "restrictions", 8)) {
            var action = EffectSnapshot.RestrictedAction.valueOf(value.getAsString());
            if (!restrictions.add(action)) throw new IllegalArgumentException("Duplicate action restriction");
        }
        List<EffectInstance.AttributeContribution> attributes = new ArrayList<>();
        for (var item : DefinitionJson.array(json, "attributes", 32)) {
            var value = SettingsJson.fields(item.getAsJsonObject(), "attribute", "modifier", "amount", "operation");
            attributes.add(new EffectInstance.AttributeContribution(DefinitionJson.id(value, "attribute"),
                    new AttributeSnapshot.Contribution(DefinitionJson.id(value, "modifier"), DefinitionJson.number(value, "amount", -1000000, 1000000),
                            SettingsJson.enumeration(value, "operation", AttributeModifier.Operation.class))));
        }
        List<BehaviorComponent> components = new ArrayList<>();
        for (var id : DefinitionJson.orderedIds(json, "behaviors")) {
            var definition = behaviors.get(id);
            if (definition == null) throw new IllegalArgumentException("Unknown behavior reference: " + id);
            components.addAll(definition.plan().components());
        }
        return new EffectDefinition(SettingsJson.integer(json, "level"), EffectLifetime.decode(json.getAsJsonObject("lifetime")),
                DefinitionJson.id(json, "stacking"),
                new EffectPolicy(SettingsJson.enumeration(json, "category", EffectPolicy.Category.class), SettingsJson.bool(json, "dispellable"),
                        json.has("tags") ? DefinitionJson.ids(json, "tags") : Set.of()),
                attributes, new BehaviorPlan(SettingsJson.integer(json, "priority"), components), restrictions, display);
    }
}
