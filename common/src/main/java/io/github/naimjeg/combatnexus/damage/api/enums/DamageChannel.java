package io.github.naimjeg.combatnexus.damage.api.enums;

import io.github.naimjeg.combatnexus.damage.api.DamageModuleIds;
import net.minecraft.resources.Identifier;

/**
 * Damage channel value. Built-in {@code *_ID} fields are stable persistent IDs;
 * {@link #index()} is assigned at runtime and must never be serialized.
 */
public record DamageChannel(Identifier id, int index) {

    public static final Identifier UNTYPED_ID =
            DamageModuleIds.id("untyped");

    public static final Identifier PHYSICAL_ID =
            DamageModuleIds.id("physical");

    public static final Identifier FIRE_ID =
            DamageModuleIds.id("fire");

    public static final Identifier COLD_ID =
            DamageModuleIds.id("cold");

    public static final Identifier LIGHTNING_ID =
            DamageModuleIds.id("lightning");

    public static final Identifier MAGIC_ID =
            DamageModuleIds.id("magic");

    public static final Identifier WITHER_ID =
            DamageModuleIds.id("wither");

    public static final Identifier KINETIC_ID =
            DamageModuleIds.id("kinetic");

    public static final Identifier POISON_ID =
            DamageModuleIds.id("poison");

    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DamageChannel other)) return false;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
