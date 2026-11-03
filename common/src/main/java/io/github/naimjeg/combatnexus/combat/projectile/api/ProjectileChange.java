package io.github.naimjeg.combatnexus.combat.projectile.api;

import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;
import net.minecraft.world.entity.projectile.Projectile;

/** Short-lived submission. Only the launch tick permits base-damage modification. */
public record ProjectileChange(UUID requestId, UUID target, CombatSource source, Projectile projectile,
        UUID action, double damageMultiplier) {
    public ProjectileChange {
        Objects.requireNonNull(requestId); Objects.requireNonNull(target); Objects.requireNonNull(source);
        Objects.requireNonNull(projectile); Objects.requireNonNull(action);
        if (!Double.isFinite(damageMultiplier) || damageMultiplier < 0) throw new IllegalArgumentException("Invalid projectile multiplier");
    }
}
