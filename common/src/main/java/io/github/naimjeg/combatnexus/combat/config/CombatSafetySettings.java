package io.github.naimjeg.combatnexus.combat.config;

import io.github.naimjeg.combatnexus.combat.api.transaction.CombatLimits;

/** Immutable server-authoritative cross-domain combat admission limits. */
public record CombatSafetySettings(
        int maxRecursionDepth,
        int maxDerivedRequestsPerRoot,
        int maxManagedRequestsPerServerTick
) {
    public static final int DEFAULT_MAX_RECURSION_DEPTH = CombatLimits.DEFAULT.maximumDepth();
    public static final int DEFAULT_MAX_DERIVED_REQUESTS_PER_ROOT = CombatLimits.DEFAULT.maximumDerived();
    public static final int DEFAULT_MAX_MANAGED_REQUESTS_PER_SERVER_TICK = CombatLimits.DEFAULT.maximumPerTick();

    public static final int HARD_MAX_RECURSION_DEPTH = CombatLimits.HARD_MAX_DEPTH;
    public static final int HARD_MAX_DERIVED_REQUESTS_PER_ROOT = CombatLimits.HARD_MAX_DERIVED;
    public static final int HARD_MAX_MANAGED_REQUESTS_PER_SERVER_TICK =
            CombatLimits.HARD_MAX_PER_TICK;

    public CombatSafetySettings {
        requireRange(
                "maxRecursionDepth",
                maxRecursionDepth,
                HARD_MAX_RECURSION_DEPTH
        );
        requireRange(
                "maxDerivedRequestsPerRoot",
                maxDerivedRequestsPerRoot,
                HARD_MAX_DERIVED_REQUESTS_PER_ROOT
        );
        requireRange(
                "maxManagedRequestsPerServerTick",
                maxManagedRequestsPerServerTick,
                HARD_MAX_MANAGED_REQUESTS_PER_SERVER_TICK
        );
    }

    public io.github.naimjeg.combatnexus.combat.api.transaction.CombatLimits combatLimits() {
        return new io.github.naimjeg.combatnexus.combat.api.transaction.CombatLimits(
                maxRecursionDepth, maxDerivedRequestsPerRoot, maxManagedRequestsPerServerTick);
    }

    public static CombatSafetySettings defaults() {
        return new CombatSafetySettings(
                DEFAULT_MAX_RECURSION_DEPTH,
                DEFAULT_MAX_DERIVED_REQUESTS_PER_ROOT,
                DEFAULT_MAX_MANAGED_REQUESTS_PER_SERVER_TICK
        );
    }

    private static void requireRange(
            String name,
            int value,
            int maximum
    ) {
        if (value < 1 || value > maximum) {
            throw new IllegalArgumentException(
                    name + " must be in [1, " + maximum + "]"
            );
        }
    }
}
