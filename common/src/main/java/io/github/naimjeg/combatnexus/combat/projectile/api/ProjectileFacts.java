package io.github.naimjeg.combatnexus.combat.projectile.api;
import java.util.Optional;
import net.minecraft.world.entity.Entity;
/** Read-only access remains valid after the shooter leaves the world. */
public final class ProjectileFacts {
    private ProjectileFacts() {}
    public static Optional<ProjectileLaunch> launch(Entity projectile) {
        return projectile instanceof io.github.naimjeg.combatnexus.combat.projectile.spi.NativeProjectile captured?captured.combatnexus$launch():Optional.empty();
    }
}
