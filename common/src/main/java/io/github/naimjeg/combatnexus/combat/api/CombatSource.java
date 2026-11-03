package io.github.naimjeg.combatnexus.combat.api;

import net.minecraft.resources.Identifier;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Stable contribution identity. Unknown external owners remain absent, never inferred. */
public record CombatSource(Identifier id, Optional<UUID> owner, Optional<CombatOperationRef> parentOperation) {
    public CombatSource {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(parentOperation, "parentOperation");
    }
}
