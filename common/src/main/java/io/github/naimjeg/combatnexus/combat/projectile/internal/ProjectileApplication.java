package io.github.naimjeg.combatnexus.combat.projectile.internal;

import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.projectile.api.*;
import io.github.naimjeg.combatnexus.combat.projectile.spi.NativeProjectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

public final class ProjectileApplication {
    private ProjectileApplication() {}
    public static CommandResult<Double> modify(ServerLevel level, ProjectileChange command) {
        var projectile = command.projectile();
        var launch = ((NativeProjectile)projectile).combatnexus$launch();
        if (projectile.level() != level || projectile.isRemoved() || !(projectile instanceof AbstractArrow arrow)
                || launch.isEmpty() || !launch.get().action().equals(command.action())
                || !launch.get().shooter().entity().equals(command.target())
                || launch.get().shooter().gameTick() != level.getGameTime())
            return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT, "Arrow launch identity/timing mismatch");
        double before = ((io.github.naimjeg.combatnexus.combat.projectile.spi.NativeArrow)arrow).combatnexus$baseDamage(), after = before * command.damageMultiplier();
        if (!Double.isFinite(after)) return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT, "Projectile damage overflow");
        try {
            arrow.setBaseDamage(after);
            return CommandResult.applied(((io.github.naimjeg.combatnexus.combat.projectile.spi.NativeArrow)arrow).combatnexus$baseDamage()).withJournal(new ApplicationJournal(List.of(
                    ApplicationJournal.Entry.write(ApplicationJournal.Resource.PROJECTILE, command.source().id(), before, ((io.github.naimjeg.combatnexus.combat.projectile.spi.NativeArrow)arrow).combatnexus$baseDamage(), Math.abs(((io.github.naimjeg.combatnexus.combat.projectile.spi.NativeArrow)arrow).combatnexus$baseDamage()-before), true)), true));
        } catch (RuntimeException failure) {
            return CommandResult.<Double>rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "Projectile modification incomplete; do not retry")
                    .withJournal(new ApplicationJournal(List.of(new ApplicationJournal.Entry(ApplicationJournal.Resource.PROJECTILE,
                            Optional.of(command.source().id()), OptionalDouble.of(before), OptionalDouble.of(((io.github.naimjeg.combatnexus.combat.projectile.spi.NativeArrow)arrow).combatnexus$baseDamage()), 0,
                            ApplicationJournal.Observation.UNKNOWN, false)), false));
        }
    }
}
