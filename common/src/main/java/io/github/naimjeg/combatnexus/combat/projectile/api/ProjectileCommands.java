package io.github.naimjeg.combatnexus.combat.projectile.api;

import java.util.*;
import net.minecraft.world.entity.projectile.Projectile;
import io.github.naimjeg.combatnexus.combat.api.CommandResult;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent;

public interface ProjectileCommands {
    Optional<ProjectileLaunch> snapshot(Projectile projectile);
    CommandResult<ProjectileLaunch> capture(UUID request, UUID shooter, Projectile projectile);
    CommandResult<io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.Result> execute(UUID request, UUID shooter, Projectile projectile, UUID action, io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan plan);
    CommandResult<Double> modify(ProjectileChange command);
    CommandResult<Double> modify(ProjectileChange command, CombatParent parent);
}
