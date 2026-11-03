package io.github.naimjeg.combatnexus.attribute.api;
import net.minecraft.resources.Identifier;
import java.util.List;
import java.util.Optional;
import static io.github.naimjeg.combatnexus.attribute.api.AttributeUnits.Unit.*;

/** Shared definitions. Reserved attributes retain their persisted IDs without claiming a consumer. */
public final class AttributeCatalog {
    public static final double RESISTANCE_RATING_LIMIT = 65535.0D;
    public static final double RESISTANCE_RATING_MIN = -RESISTANCE_RATING_LIMIT;
    public static final double RESISTANCE_RATING_MAX = RESISTANCE_RATING_LIMIT;
    private AttributeCatalog() {}
    public record Definition(Identifier id, double defaultValue, double minimum, double maximum, boolean syncable, boolean reserved, AttributeUnits.Unit unit) {
        public Definition {
            java.util.Objects.requireNonNull(id, "id");
            java.util.Objects.requireNonNull(unit, "unit");
            if (!Double.isFinite(defaultValue) || !Double.isFinite(minimum) || !Double.isFinite(maximum)
                    || minimum > maximum || defaultValue < minimum || defaultValue > maximum)
                throw new IllegalArgumentException("Invalid attribute bounds: " + id);
        }
    }
    public static Optional<Definition> definition(Identifier id) {
        return definitions().stream().filter(value -> value.id().equals(id)).findFirst();
    }
    public static final Definition CRIT_CHANCE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "crit_chance"), 0.0D, 0.0D, 1.0D, true, false, PROBABILITY);
    public static final Definition CRIT_DAMAGE_ADDITIVE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "crit_damage_additive"), 0.0D, 0.0D, 100.0D, true, false, ADDITIVE_RATIO);
    public static final Definition VULNERABLE_DAMAGE_ADDITIVE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "vulnerable_damage_additive"), 0.0D, 0.0D, 100.0D, true, false, ADDITIVE_RATIO);
    public static final Definition FIRE_DAMAGE_ADDITIVE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "fire_damage_additive"), 0.0D, 0.0D, 100.0D, true, false, ADDITIVE_RATIO);
    public static final Definition COLD_DAMAGE_ADDITIVE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "cold_damage_additive"), 0.0D, 0.0D, 100.0D, true, false, ADDITIVE_RATIO);
    public static final Definition LIGHTNING_DAMAGE_ADDITIVE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "lightning_damage_additive"), 0.0D, 0.0D, 100.0D, true, false, ADDITIVE_RATIO);
    public static final Definition MAGIC_DAMAGE_ADDITIVE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "magic_damage_additive"), 0.0D, 0.0D, 100.0D, true, false, ADDITIVE_RATIO);
    public static final Definition WITHER_DAMAGE_ADDITIVE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "wither_damage_additive"), 0.0D, 0.0D, 100.0D, true, false, ADDITIVE_RATIO);
    public static final Definition POISON_DAMAGE_ADDITIVE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "poison_damage_additive"), 0.0D, 0.0D, 100.0D, true, false, ADDITIVE_RATIO);
    public static final Definition MELEE_DAMAGE_ADDITIVE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "melee_damage_additive"), 0.0D, 0.0D, 100.0D, true, false, ADDITIVE_RATIO);
    public static final Definition PROJECTILE_DAMAGE_ADDITIVE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "projectile_damage_additive"), 0.0D, 0.0D, 100.0D, true, false, ADDITIVE_RATIO);
    public static final Definition KINETIC_DAMAGE_ADDITIVE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "kinetic_damage_additive"), 0.0D, 0.0D, 100.0D, true, false, ADDITIVE_RATIO);
    public static final Definition DODGE_CHANCE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "dodge_chance"), 0.0D, 0.0D, 1.0D, true, false, PROBABILITY);
    public static final Definition RESISTANCE_PHYSICAL = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "resistance_physical"), 0.0D, RESISTANCE_RATING_MIN, RESISTANCE_RATING_MAX, true, false, RATING);
    public static final Definition RESISTANCE_FIRE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "resistance_fire"), 0.0D, RESISTANCE_RATING_MIN, RESISTANCE_RATING_MAX, true, false, RATING);
    public static final Definition RESISTANCE_COLD = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "resistance_cold"), 0.0D, RESISTANCE_RATING_MIN, RESISTANCE_RATING_MAX, true, false, RATING);
    public static final Definition RESISTANCE_LIGHTNING = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "resistance_lightning"), 0.0D, RESISTANCE_RATING_MIN, RESISTANCE_RATING_MAX, true, false, RATING);
    public static final Definition RESISTANCE_MAGIC = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "resistance_magic"), 0.0D, RESISTANCE_RATING_MIN, RESISTANCE_RATING_MAX, true, false, RATING);
    public static final Definition RESISTANCE_WITHER = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "resistance_wither"), 0.0D, RESISTANCE_RATING_MIN, RESISTANCE_RATING_MAX, true, false, RATING);
    public static final Definition RESISTANCE_POISON = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "resistance_poison"), 0.0D, RESISTANCE_RATING_MIN, RESISTANCE_RATING_MAX, true, false, RATING);
    public static final Definition RESISTANCE_MELEE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "resistance_melee"), 0.0D, RESISTANCE_RATING_MIN, RESISTANCE_RATING_MAX, true, false, RATING);
    public static final Definition RESISTANCE_PROJECTILE = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "resistance_projectile"), 0.0D, RESISTANCE_RATING_MIN, RESISTANCE_RATING_MAX, true, false, RATING);
    public static final Definition RESISTANCE_KINETIC = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "resistance_kinetic"), 0.0D, RESISTANCE_RATING_MIN, RESISTANCE_RATING_MAX, true, false, RATING);
    public static final Definition ARMOR_PENETRATION_RATIO = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "armor_penetration_ratio"), 0.0D, 0.0D, 1.0D, true, false, ADDITIVE_RATIO);
    public static final Definition ARMOR_PENETRATION_FLAT = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "armor_penetration_flat"), 0.0D, 0.0D, 1000000.0D, true, false, RATING);
    public static final Definition THORNS = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "thorns"), 0.0D, 0.0D, 2048.0D, true, false, HEALTH_POINTS);
    public static final Definition HEALING_RECEIVED = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "healing_received"), 1.0D, 0.0D, 10.0D, true, false, MULTIPLIER);
    public static final Definition HEALING_POWER = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "healing_power"), 1.0D, 0.0D, 10.0D, true, false, MULTIPLIER);
    public static final Definition LIFESTEAL = new Definition(Identifier.fromNamespaceAndPath("combatnexus", "lifesteal"), 0.0D, 0.0D, 1.0D, true, false, CONVERSION_RATIO);
    public static List<Definition> definitions() { return List.of(CRIT_CHANCE, CRIT_DAMAGE_ADDITIVE, VULNERABLE_DAMAGE_ADDITIVE, FIRE_DAMAGE_ADDITIVE, COLD_DAMAGE_ADDITIVE, LIGHTNING_DAMAGE_ADDITIVE, MAGIC_DAMAGE_ADDITIVE, WITHER_DAMAGE_ADDITIVE, POISON_DAMAGE_ADDITIVE, MELEE_DAMAGE_ADDITIVE, PROJECTILE_DAMAGE_ADDITIVE, KINETIC_DAMAGE_ADDITIVE, DODGE_CHANCE, RESISTANCE_PHYSICAL, RESISTANCE_FIRE, RESISTANCE_COLD, RESISTANCE_LIGHTNING, RESISTANCE_MAGIC, RESISTANCE_WITHER, RESISTANCE_POISON, RESISTANCE_MELEE, RESISTANCE_PROJECTILE, RESISTANCE_KINETIC, ARMOR_PENETRATION_RATIO, ARMOR_PENETRATION_FLAT, THORNS, HEALING_POWER, HEALING_RECEIVED, LIFESTEAL); }
}
