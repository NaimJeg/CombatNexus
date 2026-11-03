package io.github.naimjeg.combatnexus.combat.api.behavior;

import java.util.OptionalLong;

/** Scheduling policy is independent from the behavior's domain operation. */
public enum PeriodicPhase {
    /** Refresh starts a new interval using the configured initial delay. */
    RESTART,
    /** Refresh preserves the next pending trigger for the same behavior type. */
    PRESERVE,
    /** Trigger when remaining duration is divisible by the interval, as vanilla effects do. */
    REMAINING_DURATION;

    public long firstDelay(long now, OptionalLong expiry, int interval, int initialDelay) {
        if (interval < 1 || initialDelay < 0) throw new IllegalArgumentException("Invalid periodic timing");
        if (this != REMAINING_DURATION) return initialDelay;
        // Infinite vanilla effects use the entity tick phase; callers must supply that
        // phase as their initial delay rather than treating infinity as a deadline.
        return expiry.isPresent() ? Math.max(0, expiry.getAsLong() - now) % interval : initialDelay;
    }
}
