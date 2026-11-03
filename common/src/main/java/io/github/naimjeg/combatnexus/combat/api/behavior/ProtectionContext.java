package io.github.naimjeg.combatnexus.combat.api.behavior;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation;
/** Frozen inputs; unknown origin remains absent for non-damage lethal requests. */
public record ProtectionContext(UUID target, float before, float proposed, CombatOperation operation,
        Optional<CombatSource> origin, ReactionInputs inputs, BehaviorSource behavior) {}
