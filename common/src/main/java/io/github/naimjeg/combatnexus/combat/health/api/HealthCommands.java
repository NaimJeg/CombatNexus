package io.github.naimjeg.combatnexus.combat.health.api;

import io.github.naimjeg.combatnexus.combat.api.CommandResult;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent;

public interface HealthCommands {
    CommandResult<Float> correct(HealthCorrection command);
    CommandResult<Float> correct(HealthCorrection command, CombatParent parent);
    CommandResult<LethalReceipt> execute(LethalAttempt command);
    CommandResult<LethalReceipt> execute(LethalAttempt command, CombatParent parent);
}
