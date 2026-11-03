package io.github.naimjeg.combatnexus.combat.damage.platform.client;

import io.github.naimjeg.combatnexus.combat.damage.client.DamageDummyModel;
import io.github.naimjeg.combatnexus.combat.damage.client.DamageDummyRenderer;
import io.github.naimjeg.combatnexus.combat.damage.platform.registry.ModEntityTypes;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
/** Loaded exclusively by the physical-client entry point. */
public final class DamageDummyRenderRegistration {
    private DamageDummyRenderRegistration() {}
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DamageDummyModel.LAYER_LOCATION, DamageDummyModel::createBodyLayer);
    }
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.DAMAGE_DUMMY.get(), DamageDummyRenderer::new);
    }
}
