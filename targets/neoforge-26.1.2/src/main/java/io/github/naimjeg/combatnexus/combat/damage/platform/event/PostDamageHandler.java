package io.github.naimjeg.combatnexus.combat.damage.platform.event;

import io.github.naimjeg.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import io.github.naimjeg.combatnexus.combat.damage.platform.settlement.NeoForgeDamageSettlement;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = DamageModule.MODID)
public final class PostDamageHandler {
    private PostDamageHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void captureFormalSettlement(LivingDamageEvent.Post event) {
        NeoForgeDamageSettlement.capturePost(event);
    }

}
