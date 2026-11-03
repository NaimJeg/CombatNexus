package io.github.naimjeg.combatnexus.combat.health.api;

import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;

/** A lethal intent, never ordinary damage. Native protection honors the damage type's bypass tags. */
public record LethalAttempt(UUID requestId, UUID target, CombatSource source, Identifier damageType,
        Optional<UUID> attacker, boolean bypassManagedProtection) {
    public LethalAttempt {
        Objects.requireNonNull(requestId); Objects.requireNonNull(target); Objects.requireNonNull(source);
        Objects.requireNonNull(damageType); Objects.requireNonNull(attacker);
    }
}
