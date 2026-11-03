package io.github.naimjeg.combatnexus.combat.damage.api.damage;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.Objects;

/** Equipment damage is neither health loss nor shield/absorption consumption. */
public record EquipmentDamageReceipt(EquipmentSlot slot, Identifier item, float absorbedDamage,
        int durabilityConsumed, boolean broken) {
    public EquipmentDamageReceipt {
        Objects.requireNonNull(slot); Objects.requireNonNull(item);
        if (!Float.isFinite(absorbedDamage) || absorbedDamage < 0 || durabilityConsumed < 0)
            throw new IllegalArgumentException("Invalid equipment damage receipt");
    }
}
