package io.github.naimjeg.combatnexus.combat.api.platform;

import io.github.naimjeg.combatnexus.combat.api.source.SourceProvenance;
import net.minecraft.world.entity.LivingEntity;
import java.util.function.Supplier;

/** Startup-issued native origin wrapper. Descriptive facts grant no application authority. */
public interface EffectOriginAccess {
    void enchantment(net.minecraft.resources.Identifier enchantment, Runnable nativeCall);
    <T> T invoke(LivingEntity target, SourceProvenance provenance, Supplier<T> nativeCall);
}
