package io.github.naimjeg.combatnexus.combat.internal.coordination;
import io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorDefinition;
import io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent;
import io.github.naimjeg.combatnexus.effect.api.EnvironmentDamage;
import io.github.naimjeg.combatnexus.effect.api.ScheduledEnvironmentDamage;
import io.github.naimjeg.combatnexus.config.SettingsJson;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.*;
/** Prepared with the existing aggregate combat reload; never publishes independently. */
public final class BehaviorDefinitionResources {
    private BehaviorDefinitionResources() {}
    public static Map<Identifier, BehaviorDefinition> read(ResourceManager manager) {
        var converter = FileToIdConverter.json("combatnexus_behaviors");
        var resources = converter.listMatchingResources(manager);
        if (resources.size() > 256) throw new IllegalArgumentException("Behavior definition capacity exceeded");
        Map<Identifier, BehaviorDefinition> definitions = new LinkedHashMap<>();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try (var reader = entry.getValue().openAsReader()) {
                var id = converter.fileToId(entry.getKey());
                if (id.toString().length() > 256) throw new IllegalArgumentException("Behavior definition ID too long");
                var definition = BehaviorDefinition.decode(SettingsJson.read(reader));
                for (int index = 0; index < definition.plan().components().size(); index++) {
                    Identifier damageType = switch (definition.plan().components().get(index)) {
                        case BehaviorComponent.InstantDamage damage -> damage.damageType();
                        case BehaviorComponent.PeriodicDamage damage -> damage.damageType();
                        case EnvironmentDamage damage -> damage.damageType();
                        case ScheduledEnvironmentDamage damage -> damage.parameters().damageType();
                        default -> null;
                    };
                    if (damageType != null && manager.getResource(Identifier.fromNamespaceAndPath(
                            damageType.getNamespace(), "damage_type/" + damageType.getPath() + ".json")).isEmpty())
                        throw new IllegalArgumentException("Invalid behavior " + entry.getKey() + " components[" + index
                                + "].damage_type: unknown resource " + damageType);
                }
                if (definitions.putIfAbsent(id, definition) != null)
                    throw new IllegalArgumentException("Duplicate behavior " + id);
            } catch (java.io.IOException | RuntimeException error) { throw new IllegalArgumentException("Invalid behavior " + entry.getKey(), error); }
        });
        return Map.copyOf(definitions);
    }
}
