package io.github.naimjeg.combatnexus.presentation.api;

import io.github.naimjeg.combatnexus.combat.api.source.SourceProvenance;
import net.minecraft.network.chat.Component;

/** Generic rendering of confirmed source facts; contains no vanilla effect rules. */
public final class SourceProvenanceText {
    private SourceProvenanceText() {}
    public static Component describe(SourceProvenance origin) {
        var parts = new java.util.ArrayList<Component>();
        if (origin.entity() != null) parts.add(Component.literal(origin.entity().toString()));
        if (origin.item() != null) parts.add(Component.translatable("tooltip.combatnexus.source.item", origin.item().toString()));
        if (origin.slot() != null) parts.add(Component.translatable("tooltip.combatnexus.source.slot", origin.slot().getName()));
        if (origin.enchantment() != null) parts.add(Component.translatable("tooltip.combatnexus.source.enchantment", origin.enchantment().toString()));
        if (origin.block() != null) {
            var b = origin.block();
            parts.add(Component.translatable("tooltip.combatnexus.source.block", b.dimension().toString(), b.x(), b.y(), b.z()));
        }
        if (parts.isEmpty()) return Component.translatable("tooltip.combatnexus.effect.unknown_source");
        var result = Component.empty();
        for (int i = 0; i < parts.size(); i++) { if (i > 0) result.append(" / "); result.append(parts.get(i)); }
        return result;
    }
}
