package io.github.naimjeg.combatnexus.combat.action.api;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;
import net.minecraft.world.phys.Vec3;
public sealed interface EntityCommand {
    UUID requestId(); UUID target(); CombatSource source();
    record Ignite(UUID requestId,UUID target,CombatSource source,int ticks) implements EntityCommand {
        public Ignite { Objects.requireNonNull(requestId);Objects.requireNonNull(target);Objects.requireNonNull(source);if(ticks<0)throw new IllegalArgumentException("Negative fire duration"); }
    }
    record Displace(UUID requestId,UUID target,CombatSource source,Vec3 delta,Mode mode) implements EntityCommand {
        public enum Mode { POSITION, VELOCITY }
        public Displace { Objects.requireNonNull(requestId);Objects.requireNonNull(target);Objects.requireNonNull(source);Objects.requireNonNull(delta);Objects.requireNonNull(mode);if(!Double.isFinite(delta.x)||!Double.isFinite(delta.y)||!Double.isFinite(delta.z))throw new IllegalArgumentException("Nonfinite displacement"); }
    }
}
