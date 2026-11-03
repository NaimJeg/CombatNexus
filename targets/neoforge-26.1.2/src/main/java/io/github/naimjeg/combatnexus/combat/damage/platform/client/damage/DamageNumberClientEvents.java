package io.github.naimjeg.combatnexus.combat.damage.platform.client.damage;

import io.github.naimjeg.combatnexus.combat.damage.client.damage.ClientDamageNumberManager;
import io.github.naimjeg.combatnexus.combat.damage.client.damage.DamageNumberRenderer;

import io.github.naimjeg.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(
        modid = DamageModule.MODID,
        value = Dist.CLIENT
)
final class DamageNumberClientEvents {

    private DamageNumberClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientDamageNumberManager.tick();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(
            RenderLevelStageEvent.AfterTranslucentParticles event
    ) {
        DamageNumberRenderer.render(event.getLevelRenderState().cameraRenderState, event.getPoseStack());
    }
}
