package io.github.naimjeg.combatnexus.combat.damage.platform.event;

import io.github.naimjeg.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import io.github.naimjeg.combatnexus.combat.damage.platform.settlement.NeoForgeDamageSettlement;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = DamageModule.MODID)
public final class PreDamageTransactionHandler {
    private PreDamageTransactionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        NeoForgeDamageSettlement.capturePre(event);
    }
}
