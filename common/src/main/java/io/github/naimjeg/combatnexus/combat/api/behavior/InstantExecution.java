package io.github.naimjeg.combatnexus.combat.api.behavior;

import io.github.naimjeg.combatnexus.combat.api.CommandOutcome;
import io.github.naimjeg.combatnexus.combat.api.DomainCommand;

/** Short-lived execution capability; every submitted command is included in the parent effect receipt. */
public interface InstantExecution {
    BehaviorSource source();
    CommandOutcome<?> submit(DomainCommand command);
    CommandOutcome<?> damage(double amount, net.minecraft.resources.Identifier type, java.util.Optional<java.util.UUID> direct);
}
