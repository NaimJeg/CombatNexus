package io.github.naimjeg.combatnexus.combat.absorption.api;

import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;

/** Native absorption, independent of CombatNexus shield pools. Existing unowned absorption is consumed first. */
public record AbsorptionCommand(UUID requestId, UUID target, CombatSource source, Operation operation, double amount, double capacity) {
    public enum Operation { GRANT, RESIZE, REMOVE }
    public AbsorptionCommand {
        Objects.requireNonNull(requestId); Objects.requireNonNull(target); Objects.requireNonNull(source); Objects.requireNonNull(operation);
        if (!Double.isFinite(amount) || amount < 0 || !Double.isFinite(capacity) || capacity < 0)
            throw new IllegalArgumentException("Invalid native absorption grant");
    }
}
