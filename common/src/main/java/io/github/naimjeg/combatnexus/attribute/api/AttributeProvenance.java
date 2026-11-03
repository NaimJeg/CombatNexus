package io.github.naimjeg.combatnexus.attribute.api;

import java.util.*;
import net.minecraft.resources.Identifier;

/** Immutable source blocks from the same capture as the value frame. */
public record AttributeProvenance(UUID entity, long gameTick, long revision,
        Map<Identifier, List<AttributeSnapshot.Contribution>> sources) {
    public AttributeProvenance {
        Objects.requireNonNull(entity);
        var copy = new LinkedHashMap<Identifier, List<AttributeSnapshot.Contribution>>();
        sources.forEach((id, block) -> copy.put(id, List.copyOf(block)));
        sources = Map.copyOf(copy);
    }
}
