package io.github.naimjeg.combatnexus.attribute.platform.registry;

import io.github.naimjeg.combatnexus.attribute.api.AttributeCatalog;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * DamageModule attribute registry.
 *
 * <p>Phase 8 consumes the critical, seven channel-damage, melee/projectile
 * damage, eight channel-resistance, melee/projectile resistance, and thorns
 * attributes. Channel resistance is selected indirectly by the active
 * {@code DamageChannelRegistry.ChannelDefinition}. There is deliberately no
 * {@code physical_damage_additive}; rules may still write the existing
 * {@code physical_damage} pre-multiplier bucket.</p>
 *
 * <p>{@link #VULNERABLE_DAMAGE_ADDITIVE} and {@link #DODGE_CHANCE}
 * retain their IDs, ranges, defaults, and stored
 * modifiers, but are reserved and currently have no framework consumer.</p>
 */
public class ModAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, "combatnexus");

    /** Shared declared bound for DamageModule resistance rating attributes. */
    public static final double RESISTANCE_RATING_LIMIT = AttributeCatalog.RESISTANCE_RATING_LIMIT;
    public static final double RESISTANCE_RATING_MIN =
            -RESISTANCE_RATING_LIMIT;
    public static final double RESISTANCE_RATING_MAX =
            RESISTANCE_RATING_LIMIT;

    public static final DeferredHolder<Attribute, Attribute> CRIT_CHANCE = register(AttributeCatalog.CRIT_CHANCE);

    public static final DeferredHolder<Attribute, Attribute> CRIT_DAMAGE_ADDITIVE = register(AttributeCatalog.CRIT_DAMAGE_ADDITIVE);

    /** Target-side additive incoming damage. */
    public static final DeferredHolder<Attribute, Attribute> VULNERABLE_DAMAGE_ADDITIVE = register(AttributeCatalog.VULNERABLE_DAMAGE_ADDITIVE);

    public static final DeferredHolder<Attribute, Attribute> FIRE_DAMAGE_ADDITIVE = register(AttributeCatalog.FIRE_DAMAGE_ADDITIVE);

    public static final DeferredHolder<Attribute, Attribute> COLD_DAMAGE_ADDITIVE = register(AttributeCatalog.COLD_DAMAGE_ADDITIVE);

    public static final DeferredHolder<Attribute, Attribute> LIGHTNING_DAMAGE_ADDITIVE = register(AttributeCatalog.LIGHTNING_DAMAGE_ADDITIVE);

    public static final DeferredHolder<Attribute, Attribute> MAGIC_DAMAGE_ADDITIVE = register(AttributeCatalog.MAGIC_DAMAGE_ADDITIVE);

    public static final DeferredHolder<Attribute, Attribute> WITHER_DAMAGE_ADDITIVE = register(AttributeCatalog.WITHER_DAMAGE_ADDITIVE);

    public static final DeferredHolder<Attribute, Attribute> POISON_DAMAGE_ADDITIVE = register(AttributeCatalog.POISON_DAMAGE_ADDITIVE);

    public static final DeferredHolder<Attribute, Attribute> MELEE_DAMAGE_ADDITIVE = register(AttributeCatalog.MELEE_DAMAGE_ADDITIVE);

    public static final DeferredHolder<Attribute, Attribute> PROJECTILE_DAMAGE_ADDITIVE = register(AttributeCatalog.PROJECTILE_DAMAGE_ADDITIVE);

    public static final DeferredHolder<Attribute, Attribute> KINETIC_DAMAGE_ADDITIVE = register(AttributeCatalog.KINETIC_DAMAGE_ADDITIVE);

    /** Reserved: damage avoidance requires its own decision lifecycle. */
    public static final DeferredHolder<Attribute, Attribute> DODGE_CHANCE = register(AttributeCatalog.DODGE_CHANCE);

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_PHYSICAL = register(AttributeCatalog.RESISTANCE_PHYSICAL);

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_FIRE = register(AttributeCatalog.RESISTANCE_FIRE);

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_COLD = register(AttributeCatalog.RESISTANCE_COLD);

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_LIGHTNING = register(AttributeCatalog.RESISTANCE_LIGHTNING);

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_MAGIC = register(AttributeCatalog.RESISTANCE_MAGIC);

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_WITHER = register(AttributeCatalog.RESISTANCE_WITHER);

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_POISON = register(AttributeCatalog.RESISTANCE_POISON);

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_MELEE = register(AttributeCatalog.RESISTANCE_MELEE);

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_PROJECTILE = register(AttributeCatalog.RESISTANCE_PROJECTILE);

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_KINETIC = register(AttributeCatalog.RESISTANCE_KINETIC);

    public static final DeferredHolder<Attribute, Attribute> ARMOR_PENETRATION_RATIO = register(AttributeCatalog.ARMOR_PENETRATION_RATIO);

    public static final DeferredHolder<Attribute, Attribute> ARMOR_PENETRATION_FLAT = register(AttributeCatalog.ARMOR_PENETRATION_FLAT);

    public static final DeferredHolder<Attribute, Attribute> THORNS = register(AttributeCatalog.THORNS);

    /** Consumed once by independent framework healing requests. */
    public static final DeferredHolder<Attribute, Attribute> HEALING_POWER = register(AttributeCatalog.HEALING_POWER);
    public static final DeferredHolder<Attribute, Attribute> HEALING_RECEIVED = register(AttributeCatalog.HEALING_RECEIVED);

    public static final DeferredHolder<Attribute, Attribute> LIFESTEAL = register(AttributeCatalog.LIFESTEAL);

    private static DeferredHolder<Attribute, Attribute> register(AttributeCatalog.Definition definition) {
        return ATTRIBUTES.register(definition.id().getPath(), () -> new RangedAttribute(
                "attribute.name." + definition.id().getNamespace() + "." + definition.id().getPath(),
                definition.defaultValue(), definition.minimum(), definition.maximum()).setSyncable(definition.syncable()));
    }

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }
}
