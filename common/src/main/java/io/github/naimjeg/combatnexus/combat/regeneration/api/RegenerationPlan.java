package io.github.naimjeg.combatnexus.combat.regeneration.api;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;
import java.util.*;
/** Installed recovery command with copied policy and an optional absolute deadline. */
public record RegenerationPlan(UUID target, CombatSource source, RegenerationDefinition definition, OptionalLong expiresAtTick) {
    public RegenerationPlan {
        Objects.requireNonNull(target); Objects.requireNonNull(source); Objects.requireNonNull(definition); Objects.requireNonNull(expiresAtTick);
        if (expiresAtTick.isPresent() && expiresAtTick.getAsLong() < 0) throw new IllegalArgumentException("Negative expiry");
    }
}
