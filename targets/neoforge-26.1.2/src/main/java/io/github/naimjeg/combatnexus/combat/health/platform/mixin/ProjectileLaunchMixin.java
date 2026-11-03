package io.github.naimjeg.combatnexus.combat.health.platform.mixin;

import java.util.Optional;
import io.github.naimjeg.combatnexus.combat.projectile.api.ProjectileLaunch;
import io.github.naimjeg.combatnexus.combat.projectile.spi.NativeProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.*;

@Mixin(Projectile.class)
public abstract class ProjectileLaunchMixin implements NativeProjectile {
    @Unique private ProjectileLaunch combatnexus$launch;
    public Optional<ProjectileLaunch> combatnexus$launch() { return Optional.ofNullable(combatnexus$launch); }
    public void combatnexus$captureLaunch(ProjectileLaunch launch) {
        if (combatnexus$launch != null) throw new IllegalStateException("Projectile launch already captured");
        combatnexus$launch = launch;
    }
}
