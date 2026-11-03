package io.github.naimjeg.combatnexus.combat.api;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;

import java.util.*;
import net.minecraft.server.level.ServerLevel;

/** Read-only session lookup for domain ports. Session IDs are not mutation authority. */
public final class CombatWorldSessions {
    private CombatWorldSessions() {}
    public static java.util.List<io.github.naimjeg.combatnexus.effect.api.EffectInstance> managedEffects(ServerLevel level, UUID target) {
        return managedEffects(level, target, ignored -> {});
    }
    public static java.util.List<io.github.naimjeg.combatnexus.effect.api.EffectInstance> managedEffects(ServerLevel level, UUID target, java.util.function.IntConsumer budget) {
        return io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.existing(level.getServer())
            .flatMap(kernel -> kernel.world(level)).map(world -> world.effects().snapshot(target, budget)).orElse(java.util.List.of());
    }
    public static Optional<io.github.naimjeg.combatnexus.combat.api.source.SourceInstance> source(ServerLevel level, UUID target, net.minecraft.resources.Identifier source) {
        return io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.existing(level.getServer())
            .flatMap(kernel -> kernel.world(level)).flatMap(world -> world.sourceInstallations().identity(target, source));
    }
    public static Set<io.github.naimjeg.combatnexus.effect.api.EffectSnapshot.RestrictedAction> restrictions(ServerLevel level, UUID entity) {
        return io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.existing(level.getServer())
                .flatMap(kernel -> kernel.world(level)).map(world -> world.effects().restrictions(entity)).orElse(Set.of());
    }
    public static Optional<UUID> current(ServerLevel level) {
        return io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.existing(level.getServer())
                .flatMap(kernel -> kernel.world(level)).map(CombatRuntime::sessionId);
    }
}
