package io.github.naimjeg.combatnexus.damage.event.neoforge;

import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.core.trace.DamageModuleTransactionTracker;
import io.github.naimjeg.combatnexus.damage.core.settlement.DamageSettlementTracker;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageSourcePolicy;
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
        DamageSettlementTracker.capturePre(event);
        if (!DamageSourcePolicy.shouldManage(event.getSource())) {
            return;
        }
        DamageModuleTransactionTracker.promoteIncomingCandidate(
                event.getContainer(),
                event
        );
    }
}
