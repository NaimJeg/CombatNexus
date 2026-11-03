package io.github.naimjeg.combatnexus.combat.damage.core.pipeline;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import java.util.Objects;

/** Immutable capture of the incoming platform event. */
public record DamageInput(LivingEntity victim, DamageSource source, float originalAmount) {
    public DamageInput {
        Objects.requireNonNull(victim, "victim");
        Objects.requireNonNull(source, "source");
    }
}
