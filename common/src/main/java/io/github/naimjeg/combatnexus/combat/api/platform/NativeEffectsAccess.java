package io.github.naimjeg.combatnexus.combat.api.platform;

import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import io.github.naimjeg.combatnexus.combat.api.source.SourceProvenance;

/** Native environment inputs and source snapshots; never creates a world or advances its clock. */
public interface NativeEffectsAccess {
    void observedChange(net.minecraft.server.level.ServerLevel level, io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.Effect change);
    boolean projected(Identifier effect);
    SourceProvenance origin(LivingEntity target, UUID entity);
    void sampleEnvironment(LivingEntity entity);
    int remaining(LivingEntity entity, Identifier effect);
    void duration(LivingEntity entity, Identifier effect, int ticks);
}
