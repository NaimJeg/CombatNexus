package io.github.naimjeg.combatnexus.combat.absorption.api;

import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.CommandResult;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent;

public interface AbsorptionCommands {
    List<AbsorptionSource> snapshot(UUID target);
    CommandResult<AbsorptionSource> submit(AbsorptionCommand command);
    CommandResult<AbsorptionSource> submit(AbsorptionCommand command, CombatParent parent);
}
