package io.github.naimjeg.combatnexus.combat.api.behavior;
/** Shared scheduler owns deadlines, cancellation and offline pause; behavior submits a domain command. */
public interface PeriodicBehavior extends BehaviorComponent {
    int intervalTicks();
    default int initialDelayTicks() { return intervalTicks(); }
    default PeriodicPhase phase() { return PeriodicPhase.RESTART; }
    void execute(PeriodicExecution execution);
}
