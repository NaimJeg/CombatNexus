package io.github.naimjeg.combatnexus.combat.api.behavior;
import java.util.*;
import net.minecraft.resources.Identifier;
/** Immutable source capture. Installation identity never grants transaction authority. */
public record BehaviorSource(UUID session, UUID target, Identifier effect, Identifier source,
        long installation, OptionalLong expiresAtTick, BehaviorPlan plan, Optional<UUID> owner) {
    public BehaviorSource(UUID session, UUID target, Identifier effect, Identifier source, long installation, OptionalLong expiresAtTick, BehaviorPlan plan) {
        this(session, target, effect, source, installation, expiresAtTick, plan, Optional.empty());
    }
    public BehaviorSource { Objects.requireNonNull(owner); Objects.requireNonNull(session); Objects.requireNonNull(target); Objects.requireNonNull(effect);
        Objects.requireNonNull(source); Objects.requireNonNull(expiresAtTick); Objects.requireNonNull(plan);
        if (source.toString().length() > 256 || effect.toString().length() > 256 || installation < 1) throw new IllegalArgumentException("Invalid behavior identity"); }
}
