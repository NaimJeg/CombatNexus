package io.github.naimjeg.combatnexus.combat.internal.definition;
import io.github.naimjeg.combatnexus.combat.api.DomainDefinitions;
import io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorDefinition;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingRuleDefinition;
import io.github.naimjeg.combatnexus.combat.regeneration.api.RegenerationDefinition;
import io.github.naimjeg.combatnexus.combat.shield.api.ShieldDefinition;
import io.github.naimjeg.combatnexus.effect.api.EffectDefinition;
import io.github.naimjeg.combatnexus.config.SettingsJson;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.*;
import java.util.function.Function;
/** Strict domain readers share the aggregate reload's failure/publication boundary. */
final class DomainDefinitionResources {
    private DomainDefinitionResources() {}
    static DomainDefinitions read(ResourceManager manager, Map<Identifier, BehaviorDefinition> behaviors) {
        var shields = read(manager, "combatnexus_shields", 256, ShieldDefinition::decode);
        for (var shield : shields.values()) {
            shield.damageTypes().forEach(id -> requireResource(manager, id, "damage_type/"));
            shield.damageTypeTags().forEach(id -> requireResource(manager, id, "tags/damage_type/"));
        }
        return new DomainDefinitions(read(manager, "combatnexus_healing_rules", 64, HealingRuleDefinition::decode),
                read(manager, "combatnexus_regeneration", 256, RegenerationDefinition::decode), shields,
                read(manager, "combatnexus_effects", 256, json -> EffectDefinition.decode(json, behaviors)));
    }
    private static void requireResource(ResourceManager manager, Identifier id, String prefix) {
        if (manager.getResource(Identifier.fromNamespaceAndPath(id.getNamespace(), prefix + id.getPath() + ".json")).isEmpty())
            throw new IllegalArgumentException("Unknown shield filter resource: " + prefix + id);
    }
    private static <T> Map<Identifier, T> read(ResourceManager manager, String directory, int maximum,
            Function<com.google.gson.JsonObject, T> decode) {
        var converter = FileToIdConverter.json(directory);
        var files = converter.listMatchingResources(manager);
        if (files.size() > maximum) throw new IllegalArgumentException("Resource budget exceeded: " + directory);
        var result = new LinkedHashMap<Identifier, T>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try (var reader = entry.getValue().openAsReader()) {
                var id = converter.fileToId(entry.getKey());
                if (id.toString().length() > 256) throw new IllegalArgumentException("Definition ID too long");
                result.put(id, decode.apply(SettingsJson.read(reader)));
            } catch (java.io.IOException | RuntimeException failure) {
                throw new IllegalArgumentException("Invalid domain definition: " + entry.getKey(), failure);
            }
        });
        return Map.copyOf(result);
    }
}
