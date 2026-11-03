package io.github.naimjeg.combatnexus.combat.damage.platform.event;

import io.github.naimjeg.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla.VanillaDamageCapture;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageAttributionEntryPoint;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageAttributionQuery;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageOrigin;
import io.github.naimjeg.combatnexus.combat.damage.core.attribution.DamageAttributionResolvers;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageSourcePolicy;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageModuleContext;
import io.github.naimjeg.combatnexus.combat.damage.platform.pipeline.NeoForgeDamageContexts;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageApplicationPlan;
import io.github.naimjeg.combatnexus.combat.damage.core.request.DamageRequestSubmissionTracker;
import io.github.naimjeg.combatnexus.combat.damage.core.request.DamageAdmissionController;
import io.github.naimjeg.combatnexus.combat.damage.core.request.DamageAdmissionResult;
import io.github.naimjeg.combatnexus.combat.damage.core.request.DamageTransactionActivity;
import io.github.naimjeg.combatnexus.combat.damage.platform.settlement.NeoForgeDamageSettlement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = DamageModule.MODID)
public class IncomingDamageHandler {
    private static final ThreadLocal<Integer> EVENT_REENTRANCY_DEPTH =
            ThreadLocal.withInitial(() -> 0);

    /* Stack-safety fuse only; explicit lineage limits are enforced elsewhere. */
    private static final int MAX_EVENT_REENTRANCY_DEPTH = io.github.naimjeg.combatnexus.combat.api.transaction.CombatLimits.HARD_MAX_DEPTH + 1;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        int eventDepth = EVENT_REENTRANCY_DEPTH.get();

        try {
            EVENT_REENTRANCY_DEPTH.set(eventDepth + 1);

            if (!DamageSourcePolicy.shouldManage(event.getSource())
                    || event.getEntity().level().isClientSide()) {
                return;
            }

            DamageRequestSubmissionTracker.Claim claim =
                    DamageRequestSubmissionTracker.claimIncoming(event.getEntity(), event.getSource());
            var definitionHandle = claim == null
                    ? io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.of(((ServerLevel) event.getEntity().level()).getServer()).ticket(io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope.currentLineage(((ServerLevel) event.getEntity().level()).getServer()).orElseThrow()).definitions()
                    : claim.definitions();
            DamageOrigin origin;
            if (claim == null) {
                ServerLevel level = (ServerLevel) event.getEntity().level();
                try (var computing = io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope.computation()) {
                DamageOrigin base = NeoForgeDamageContexts.nativeOrigin(event);
                origin = DamageAttributionResolvers.resolve(
                        new DamageAttributionQuery(
                                level,
                                event.getEntity(),
                                base.source(),
                                java.util.Optional.of(event.getSource()),
                                base.requestKind(),
                                base.attribution(),
                                base.actionId(),
                                base.sourceTags(),
                                base.metadata(),
                                DamageAttributionEntryPoint.NATIVE
                        ),
                        base
                );
                }
            } else {
                origin = claim.origin();
            }
            MinecraftServer server = ((ServerLevel) event.getEntity().level())
                    .getServer();
            DamageAdmissionResult admission;
            boolean nativeDamage = claim == null;

            if (eventDepth >= MAX_EVENT_REENTRANCY_DEPTH) {
                admission = DamageAdmissionController.rejectEventReentrancy(
                        origin,
                        server
                );
            } else if (nativeDamage) {
                admission = DamageAdmissionController.admitNative(
                        origin,
                        server, definitionHandle
                );
            } else {
                admission = claim.admission();
            }

            if (!admission.admitted()) {
                if (!NeoForgeDamageSettlement.rejectAdmission(
                        event,
                        origin,
                        admission.reason()
                )) {
                    throw new IllegalStateException(
                            "Managed admission rejection had no hurt scope"
                    );
                }
                event.setCanceled(true);

                return;
            }

            boolean settlementTracked = NeoForgeDamageSettlement.beginIncoming(event, origin);
            try (DamageTransactionActivity.Scope ignored = DamageTransactionActivity.enter()) {
                DamageModuleContext ctx =
                        NeoForgeDamageContexts.tryCreate(event, origin, definitionHandle);

                if (ctx == null) {
                    return;
                }

                DamageApplicationPlan summary =
                        io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageApplicationCoordinator.calculateAndHandoff(ctx);
                if (settlementTracked) {
                    NeoForgeDamageSettlement.recordCalculated(event, summary);
                }
                DamageRequestSubmissionTracker.recordPipelineResult(
                        event.getEntity(), event.getSource(),
                        summary
                );
            }

        } finally {
            VanillaDamageCapture.clear();
            if (eventDepth == 0) {
                EVENT_REENTRANCY_DEPTH.remove();
            } else {
                EVENT_REENTRANCY_DEPTH.set(eventDepth);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onIncomingDispatchComplete(
            LivingIncomingDamageEvent event
    ) {
        NeoForgeDamageSettlement.enforceAdmissionRejection(event);
        NeoForgeDamageSettlement.markIncomingDispatchComplete(event);
    }
}

