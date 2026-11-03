package io.github.naimjeg.combatnexus.combat.action.api;
import io.github.naimjeg.combatnexus.combat.api.CommandResult;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent;
public interface EntityCommands {
    CommandResult<Boolean> submit(EntityCommand command);
    CommandResult<Boolean> submit(EntityCommand command,CombatParent parent);
}
