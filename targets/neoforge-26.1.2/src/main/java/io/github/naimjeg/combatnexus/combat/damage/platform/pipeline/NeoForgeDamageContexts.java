package io.github.naimjeg.combatnexus.combat.damage.platform.pipeline;
import io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration;

import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageInput;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageModuleContext;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageModuleContextFactory;

import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageOrigin;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** The only event-to-context adapter. */
public final class NeoForgeDamageContexts {
    private NeoForgeDamageContexts() {}
    private static DamageInput input(LivingIncomingDamageEvent event) {
        return new DamageInput(event.getEntity(), event.getSource(), event.getOriginalAmount());
    }
    public static DamageModuleContext tryCreate(LivingIncomingDamageEvent event, DamageOrigin origin, io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration definitions) {
        return event == null ? null : DamageModuleContextFactory.tryCreate(input(event), new DamageEventWriter(event), origin, definitions);
    }
    public static DamageOrigin nativeOrigin(LivingIncomingDamageEvent event) {
        return DamageModuleContextFactory.nativeOrigin(input(event));
    }
}
