package io.github.naimjeg.combatnexus.combat.api.source;

import java.util.*;
import net.minecraft.resources.Identifier;

/** A provider's concrete slot or cast, distinct from a definition and from installation authority. */
public record SourceInstance(UUID instance, Identifier provider, Optional<UUID> owner, String slot) {
    public SourceInstance {
        Objects.requireNonNull(instance); Objects.requireNonNull(provider); Objects.requireNonNull(owner); Objects.requireNonNull(slot);
        if (slot.length() > 256) throw new IllegalArgumentException("Source slot too long");
    }
}
