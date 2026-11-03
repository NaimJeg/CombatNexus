package io.github.naimjeg.combatnexus.combat.health.api;

import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;

/** Explicit health state correction; does not emit healing or healing-derived benefits. */
public record HealthCorrection(UUID requestId, UUID target, CombatSource source, double health, Reason reason) {
    public enum Reason { MAXIMUM_CHANGED, INITIALIZATION, LOAD, SCRIPTED_STATE, LIFE_COST }
    public HealthCorrection {
        Objects.requireNonNull(requestId); Objects.requireNonNull(target); Objects.requireNonNull(source); Objects.requireNonNull(reason);
        if (!Double.isFinite(health) || health < 0) throw new IllegalArgumentException("Invalid corrected health");
    }
}
