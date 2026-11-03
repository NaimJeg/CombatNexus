package io.github.naimjeg.combatnexus.attribute.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/** Display semantics only; movement uses vanilla units and is never mislabeled as blocks/second. */
public final class AttributeUnits {
    public enum Unit { HEALTH_POINTS, ARMOR_POINTS, RATING, PROBABILITY, ADDITIVE_RATIO, CONVERSION_RATIO, MULTIPLIER,
        ATTACKS_PER_SECOND, VANILLA_MOVEMENT }
    private static final Map<Identifier, Unit> UNITS = definitions();
    private AttributeUnits() {}
    public static Optional<Unit> unit(Identifier attribute) { return Optional.ofNullable(UNITS.get(attribute)); }

    private static Map<Identifier, Unit> definitions() {
        Map<Identifier, Unit> result = new LinkedHashMap<>();
        for (var definition : AttributeCatalog.definitions()) {
            result.put(definition.id(), definition.unit());
        }
        result.put(Identifier.withDefaultNamespace("attack_damage"), Unit.HEALTH_POINTS);
        result.put(Identifier.withDefaultNamespace("max_health"), Unit.HEALTH_POINTS);
        result.put(Identifier.withDefaultNamespace("max_absorption"), Unit.HEALTH_POINTS);
        result.put(Identifier.withDefaultNamespace("armor"), Unit.ARMOR_POINTS);
        result.put(Identifier.withDefaultNamespace("armor_toughness"), Unit.ARMOR_POINTS);
        result.put(Identifier.withDefaultNamespace("attack_speed"), Unit.ATTACKS_PER_SECOND);
        result.put(Identifier.withDefaultNamespace("movement_speed"), Unit.VANILLA_MOVEMENT);
        return Map.copyOf(result);
    }
}
