package io.github.naimjeg.combatnexus.effect.api;

import io.github.naimjeg.combatnexus.combat.api.behavior.*;

/** Condition-bound damage with its own persisted cadence, independent of shared hurt cooldown. */
public record ScheduledEnvironmentDamage(EnvironmentDamage parameters, int initialDelayTicks) implements PeriodicBehavior {
    public ScheduledEnvironmentDamage(EnvironmentDamage parameters) { this(parameters, 0); }
    public ScheduledEnvironmentDamage {
        if (initialDelayTicks < 0) throw new IllegalArgumentException("Negative initial environment delay");
        java.util.Objects.requireNonNull(parameters);
        if (parameters.intervalTicks() < 1) throw new IllegalArgumentException("Scheduled environment requires a positive interval");
    }
    public int intervalTicks() { return parameters.intervalTicks(); }
    public PeriodicPhase phase() { return PeriodicPhase.PRESERVE; }
    public void execute(PeriodicExecution execution) { execution.environmentDamage(parameters); }
}
