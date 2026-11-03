package io.github.naimjeg.combatnexus.combat.state.api;
import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.api.CommandResult;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent;
public interface CooldownCommands {
    Optional<CooldownStamp> snapshot(UUID target, Identifier source);
    long remaining(UUID target, Identifier source, int duration);
    CommandResult<Boolean> submit(CooldownCommand command);
    CommandResult<Boolean> submit(CooldownCommand command, CombatParent parent);
}
