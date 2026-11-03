package io.github.naimjeg.combatnexus.damage.event.neoforge;

import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.bridge.vanilla.VanillaDamageCapture;
import io.github.naimjeg.combatnexus.damage.api.damage.DamageAttributionEntryPoint;
import io.github.naimjeg.combatnexus.damage.api.damage.DamageAttributionQuery;
import io.github.naimjeg.combatnexus.damage.api.damage.DamageOrigin;
import io.github.naimjeg.combatnexus.damage.core.attribution.DamageAttributionResolvers;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageSourcePolicy;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageModuleContext;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageModuleContextFactory;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageModulePipeline;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageExecutionSummary;
import io.github.naimjeg.combatnexus.damage.core.request.DamageRequestSubmissionTracker;
import io.github.naimjeg.combatnexus.damage.core.request.DamageAdmissionController;
import io.github.naimjeg.combatnexus.damage.core.request.DamageAdmissionResult;
import io.github.naimjeg.combatnexus.damage.core.request.DamageTransactionActivity;
import io.github.naimjeg.combatnexus.damage.core.settlement.DamageSettlementTracker;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DamageAdmissionDiagnosticsLog;
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
    private static final int MAX_EVENT_REENTRANCY_DEPTH = 32;

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
                    DamageRequestSubmissionTracker.claimIncoming(event);
            DamageOrigin origin;
            if (claim == null) {
                ServerLevel level = (ServerLevel) event.getEntity().level();
                DamageOrigin base = DamageModuleContextFactory.nativeOrigin(event);
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
                        server
                );
            } else {
                admission = claim.admission();
            }

            if (!admission.admitted()) {
                if (!DamageSettlementTracker.rejectAdmission(
                        event,
                        origin,
                        admission.reason()
                )) {
                    throw new IllegalStateException(
                            "Managed admission rejection had no hurt scope"
                    );
                }
                event.setCanceled(true);
                if (nativeDamage) {
                    DamageAdmissionDiagnosticsLog.nativeRejected(
                            origin,
                            admission
                    );
                }
                return;
            }

            if (nativeDamage) {
                DamageAdmissionDiagnosticsLog.accepted(origin, admission);
            }

            try (DamageTransactionActivity.Scope ignored =
                         DamageTransactionActivity.enter()) {
                DamageModuleContext ctx =
                        DamageModuleContextFactory.tryCreate(event, origin);

                if (ctx == null) {
                    return;
                }

                boolean settlementTracked =
                        DamageSettlementTracker.beginIncoming(
                        event,
                        ctx.origin()
                );
                DamageExecutionSummary summary =
                        DamageModulePipeline.execute(ctx);
                if (settlementTracked) {
                    DamageSettlementTracker.recordCalculated(event, summary);
                }
                DamageRequestSubmissionTracker.recordPipelineResult(
                        event,
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
        DamageSettlementTracker.enforceAdmissionRejection(event);
        DamageSettlementTracker.markIncomingDispatchComplete(event);
    }
}

