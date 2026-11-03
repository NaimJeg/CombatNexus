package io.github.naimjeg.combatnexus.damage.event.neoforge;

import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.core.config.DamageModuleSettings;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageSourcePolicy;
import io.github.naimjeg.combatnexus.damage.core.settlement.DamageSettlementTracker;
import io.github.naimjeg.combatnexus.damage.core.trace.DamageModuleTransaction;
import io.github.naimjeg.combatnexus.damage.core.trace.DamageModuleTransactionTracker;
import io.github.naimjeg.combatnexus.damage.core.trace.PostSettlementClassifier;
import io.github.naimjeg.combatnexus.damage.core.trace.PostSettlementObservation;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.PostDamageDiagnosticsLog;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = DamageModule.MODID)
public final class PostDamageHandler {

    private PostDamageHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void captureFormalSettlement(LivingDamageEvent.Post event) {
        DamageSettlementTracker.capturePost(event);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!DamageModuleTransactionTracker.enabled()
                || !DamageSourcePolicy.shouldManage(event.getSource())) {
            return;
        }

        LivingEntity victim = event.getEntity();
        DamageModuleTransaction tx =
                DamageModuleTransactionTracker.pollMatchingPostTrackable(
                        victim,
                        event.getSource(),
                        event.getInflictedDamage()
                );

        if (tx == null) {
            return;
        }

        PostSettlementObservation observation = new PostSettlementObservation(
                event.getInflictedDamage(),
                event.getHealthDamage(),
                event.getReduction(DamageContainer.Reduction.ABSORPTION),
                victim.getHealth(),
                victim.getAbsorptionAmount(),
                victim.invulnerableTime
        );
        PostSettlementClassifier.Result result =
                PostSettlementClassifier.classify(tx, observation);

        if (DamageModuleSettings.summaryTraceEnabled()
                && result.severity()
                == PostSettlementClassifier.Severity.CONSISTENT) {
            PostDamageDiagnosticsLog.observed(tx, observation, result);
        }

        if (result.adjusted()) {
            PostDamageDiagnosticsLog.adjusted(tx, observation, result);
        } else if (result.mismatch()) {
            PostDamageDiagnosticsLog.mismatch(tx, observation, result);
        }
    }
}
