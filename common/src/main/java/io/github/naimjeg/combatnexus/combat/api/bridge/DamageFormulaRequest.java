package io.github.naimjeg.combatnexus.combat.api.bridge;

import java.util.*;
import net.minecraft.resources.Identifier;

public record DamageFormulaRequest(UUID invocation, Identifier policy, UUID target, Optional<UUID> attacker,
        double amount, Optional<UUID> action, Optional<io.github.naimjeg.combatnexus.combat.projectile.api.ProjectileLaunch> launch) {
    public DamageFormulaRequest {
        Objects.requireNonNull(invocation); Objects.requireNonNull(policy); Objects.requireNonNull(target);
        Objects.requireNonNull(attacker); Objects.requireNonNull(action);
        Objects.requireNonNull(launch);
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid formula input");
    }
}
