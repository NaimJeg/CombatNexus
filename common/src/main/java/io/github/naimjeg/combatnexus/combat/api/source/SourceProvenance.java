package io.github.naimjeg.combatnexus.combat.api.source;

import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;

/** Observed origin facts, independent of installation/stacking identity. Null means unknown. */
public record SourceProvenance(UUID entity, Identifier item, EquipmentSlot slot, Identifier enchantment, Block block) {
    public static final SourceProvenance UNKNOWN = new SourceProvenance(null, null, null, null, null);
    public static SourceProvenance entity(UUID entity) { return new SourceProvenance(entity, null, null, null, null); }
    public record Block(Identifier dimension, int x, int y, int z) {
        public Block { java.util.Objects.requireNonNull(dimension); }
    }
}
