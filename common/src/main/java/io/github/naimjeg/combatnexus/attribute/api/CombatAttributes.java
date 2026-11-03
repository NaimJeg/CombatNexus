package io.github.naimjeg.combatnexus.attribute.api;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Stable, read-only references to DamageModule attributes. */
public final class CombatAttributes {
    public static final ResourceKey<Attribute> CRIT_CHANCE = key("crit_chance");
    public static final ResourceKey<Attribute> CRIT_DAMAGE_ADDITIVE = key("crit_damage_additive");
    /** Target-side additive incoming damage, assembled from the authoritative attribute snapshot. */
    public static final ResourceKey<Attribute> VULNERABLE_DAMAGE_ADDITIVE = key("vulnerable_damage_additive");
    public static final ResourceKey<Attribute> FIRE_DAMAGE_ADDITIVE = key("fire_damage_additive");
    public static final ResourceKey<Attribute> COLD_DAMAGE_ADDITIVE = key("cold_damage_additive");
    public static final ResourceKey<Attribute> LIGHTNING_DAMAGE_ADDITIVE = key("lightning_damage_additive");
    public static final ResourceKey<Attribute> MAGIC_DAMAGE_ADDITIVE = key("magic_damage_additive");
    public static final ResourceKey<Attribute> WITHER_DAMAGE_ADDITIVE = key("wither_damage_additive");
    public static final ResourceKey<Attribute> POISON_DAMAGE_ADDITIVE = key("poison_damage_additive");
    public static final ResourceKey<Attribute> KINETIC_DAMAGE_ADDITIVE = key("kinetic_damage_additive");
    public static final ResourceKey<Attribute> MELEE_DAMAGE_ADDITIVE = key("melee_damage_additive");
    public static final ResourceKey<Attribute> PROJECTILE_DAMAGE_ADDITIVE = key("projectile_damage_additive");
    /** Probability of avoiding an eligible primary attack and its descendants. */
    public static final ResourceKey<Attribute> DODGE_CHANCE = key("dodge_chance");
    public static final ResourceKey<Attribute> RESISTANCE_PHYSICAL = key("resistance_physical");
    public static final ResourceKey<Attribute> RESISTANCE_FIRE = key("resistance_fire");
    public static final ResourceKey<Attribute> RESISTANCE_COLD = key("resistance_cold");
    public static final ResourceKey<Attribute> RESISTANCE_LIGHTNING = key("resistance_lightning");
    public static final ResourceKey<Attribute> RESISTANCE_MAGIC = key("resistance_magic");
    public static final ResourceKey<Attribute> RESISTANCE_WITHER = key("resistance_wither");
    public static final ResourceKey<Attribute> RESISTANCE_POISON = key("resistance_poison");
    public static final ResourceKey<Attribute> RESISTANCE_KINETIC = key("resistance_kinetic");
    public static final ResourceKey<Attribute> RESISTANCE_MELEE = key("resistance_melee");
    public static final ResourceKey<Attribute> RESISTANCE_PROJECTILE = key("resistance_projectile");
    public static final ResourceKey<Attribute> ARMOR_PENETRATION_RATIO = key("armor_penetration_ratio");
    public static final ResourceKey<Attribute> ARMOR_PENETRATION_FLAT = key("armor_penetration_flat");
    public static final ResourceKey<Attribute> THORNS = key("thorns");
    /** Incoming healing multiplier, captured once by the independent healing pipeline. */
    public static final ResourceKey<Attribute> HEALING_POWER = key("healing_power");
    public static Holder<Attribute> healingPower() { return holder(HEALING_POWER); }
    public static final ResourceKey<Attribute> HEALING_RECEIVED = key("healing_received");

    private CombatAttributes() {}

    // Reuse vanilla holders; never register synonymous attributes.
    public static Holder<Attribute> attackSpeed() { return Attributes.ATTACK_SPEED; }
    public static Holder<Attribute> movementSpeed() { return Attributes.MOVEMENT_SPEED; }
    public static Holder<Attribute> armor() { return Attributes.ARMOR; }
    public static Holder<Attribute> armorToughness() { return Attributes.ARMOR_TOUGHNESS; }
    public static Holder<Attribute> maxHealth() { return Attributes.MAX_HEALTH; }

    /** Holder accessors are valid after NeoForge has bound the attribute registry. */
    public static Holder<Attribute> critChance() { return holder(CRIT_CHANCE); }
    public static Holder<Attribute> critDamageAdditive() { return holder(CRIT_DAMAGE_ADDITIVE); }
    public static Holder<Attribute> vulnerableDamageAdditive() { return holder(VULNERABLE_DAMAGE_ADDITIVE); }
    public static Holder<Attribute> fireDamageAdditive() { return holder(FIRE_DAMAGE_ADDITIVE); }
    public static Holder<Attribute> coldDamageAdditive() { return holder(COLD_DAMAGE_ADDITIVE); }
    public static Holder<Attribute> lightningDamageAdditive() { return holder(LIGHTNING_DAMAGE_ADDITIVE); }
    public static Holder<Attribute> magicDamageAdditive() { return holder(MAGIC_DAMAGE_ADDITIVE); }
    public static Holder<Attribute> witherDamageAdditive() { return holder(WITHER_DAMAGE_ADDITIVE); }
    public static Holder<Attribute> poisonDamageAdditive() { return holder(POISON_DAMAGE_ADDITIVE); }
    public static Holder<Attribute> kineticDamageAdditive() { return holder(KINETIC_DAMAGE_ADDITIVE); }
    public static Holder<Attribute> meleeDamageAdditive() { return holder(MELEE_DAMAGE_ADDITIVE); }
    public static Holder<Attribute> projectileDamageAdditive() { return holder(PROJECTILE_DAMAGE_ADDITIVE); }
    public static Holder<Attribute> armorPenetrationRatio() { return holder(ARMOR_PENETRATION_RATIO); }
    public static Holder<Attribute> armorPenetrationFlat() { return holder(ARMOR_PENETRATION_FLAT); }
    public static Holder<Attribute> dodgeChance() { return holder(DODGE_CHANCE); }
    public static Holder<Attribute> resistancePhysical() { return holder(RESISTANCE_PHYSICAL); }
    public static Holder<Attribute> resistanceFire() { return holder(RESISTANCE_FIRE); }
    public static Holder<Attribute> resistanceCold() { return holder(RESISTANCE_COLD); }
    public static Holder<Attribute> resistanceLightning() { return holder(RESISTANCE_LIGHTNING); }
    public static Holder<Attribute> resistanceMagic() { return holder(RESISTANCE_MAGIC); }
    public static Holder<Attribute> resistanceWither() { return holder(RESISTANCE_WITHER); }
    public static Holder<Attribute> resistancePoison() { return holder(RESISTANCE_POISON); }
    public static Holder<Attribute> resistanceKinetic() { return holder(RESISTANCE_KINETIC); }
    public static Holder<Attribute> resistanceMelee() { return holder(RESISTANCE_MELEE); }
    public static Holder<Attribute> resistanceProjectile() { return holder(RESISTANCE_PROJECTILE); }
    public static Holder<Attribute> thorns() { return holder(THORNS); }
    public static final ResourceKey<Attribute> LIFESTEAL = key("lifesteal");
    public static Holder<Attribute> lifesteal() { return holder(LIFESTEAL); }
    public static Holder<Attribute> healingReceived() { return holder(HEALING_RECEIVED); }

    private static Holder<Attribute> holder(ResourceKey<Attribute> key) {
        return BuiltInRegistries.ATTRIBUTE.getOrThrow(key);
    }

    private static ResourceKey<Attribute> key(String path) {
        Identifier id = Identifier.fromNamespaceAndPath("combatnexus", path);
        return ResourceKey.create(Registries.ATTRIBUTE, id);
    }
}
