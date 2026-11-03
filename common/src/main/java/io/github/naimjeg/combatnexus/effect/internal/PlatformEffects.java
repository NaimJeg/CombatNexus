package io.github.naimjeg.combatnexus.effect.internal;

import io.github.naimjeg.combatnexus.combat.api.platform.NativeEffectsAccess;
import io.github.naimjeg.combatnexus.combat.api.source.SourceProvenance;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import java.util.UUID;

public final class PlatformEffects implements NativeEffectsAccess {
    public static final NativeEffectsAccess INSTANCE = new PlatformEffects();
    private PlatformEffects() {}
    public void observedChange(net.minecraft.server.level.ServerLevel level, io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.Effect change) {
        if (change.managedChange().isPresent()) throw new IllegalArgumentException("Native observation cannot publish managed commits");
        io.github.naimjeg.combatnexus.combat.spi.CombatMomentAccess.publish(level, change);
    }
    public boolean projected(Identifier effect) { return EffectMappings.projected(effect); }
    public SourceProvenance origin(LivingEntity target, UUID entity) { return NativeEffectOrigins.current(target, entity); }
    public void sampleEnvironment(LivingEntity entity) { EnvironmentEffects.sample(entity); }
    public int remaining(LivingEntity entity, Identifier effect) { return EnvironmentEffects.remaining(entity, effect); }
    public void duration(LivingEntity entity, Identifier effect, int ticks) { EnvironmentEffects.duration(entity, effect, ticks); }
}
