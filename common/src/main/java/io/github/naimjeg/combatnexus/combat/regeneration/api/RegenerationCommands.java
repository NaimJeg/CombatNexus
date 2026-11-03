package io.github.naimjeg.combatnexus.combat.regeneration.api;
import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.api.CommandResult;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent;
public interface RegenerationCommands {
    List<RegenerationPlan> snapshot(UUID target);
    CommandResult<Boolean> replace(UUID request, RegenerationPlan plan);
    CommandResult<Boolean> replace(UUID request, RegenerationPlan plan, CombatParent parent);
    CommandResult<Boolean> remove(UUID request, UUID target, Identifier source);
}
