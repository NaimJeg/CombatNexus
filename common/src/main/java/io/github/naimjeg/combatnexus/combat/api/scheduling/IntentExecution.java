package io.github.naimjeg.combatnexus.combat.api.scheduling;
import java.util.UUID;
import io.github.naimjeg.combatnexus.combat.api.*;
/** Expires after one invocation. The executor appends every receipt, including refused commands. */
public interface IntentExecution {
    UUID target();
    long gameTick();
    CombatSource source();
    CommandOutcome<?> submit(DomainCommand command);
    CommandOutcome<?> damage(double amount, net.minecraft.resources.Identifier type);
}
