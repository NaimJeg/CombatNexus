package io.github.naimjeg.combatnexus.combat.api.behavior;

/** Executes once during an instant effect application, without installing a scheduler task. */
public interface InstantBehavior extends BehaviorComponent {
    void execute(InstantExecution execution);
}
