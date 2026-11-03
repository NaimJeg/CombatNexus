package io.github.naimjeg.combatnexus.combat.projectile.spi;

import io.github.naimjeg.combatnexus.combat.projectile.api.ProjectileLaunch;
import java.util.Optional;

public interface NativeProjectile {
    Optional<ProjectileLaunch> combatnexus$launch();
    void combatnexus$captureLaunch(ProjectileLaunch launch);
}
