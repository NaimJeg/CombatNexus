package io.github.naimjeg.combatnexus.damage.core.settlement;

import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.api.event.DamageSettledEvent;
import io.github.naimjeg.combatnexus.damage.core.request.DamageRequestSubmissionTracker;
import io.github.naimjeg.combatnexus.damage.core.request.DamageTransactionActivity;
import io.github.naimjeg.combatnexus.damage.core.util.JvmFatalErrors;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DamageModuleDiagnosticState;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DiagnosticTextSanitizer;
import net.neoforged.neoforge.common.NeoForge;

/** Publishes a completion once, after all mutable transaction state is gone. */
public final class DamageSettlementEventPublisher {

    private DamageSettlementEventPublisher() {
    }

    static boolean publish(DamageSettlementCompletion completion) {
        if (DamageTransactionActivity.isActive()
                || DamageRequestSubmissionTracker.hasActiveSubmission()
                || DamageSettlementTracker.hasActiveHurt()) {
            throw new IllegalStateException(
                    "Damage settlement event publication attempted while a "
                            + "damage transaction is active"
            );
        }
        if (!completion.claimPublication()) {
            return false;
        }

        DamageSettledEvent event = new DamageSettledEvent(
                completion.snapshot());
        try (DamageSettlementDispatchScope.Scope ignored =
                     DamageSettlementDispatchScope.openObservation(
                             event,
                             completion.snapshot().level().getServer())) {
            dispatch(
                    completion.snapshot().lineage().damageId(),
                    () -> NeoForge.EVENT_BUS.post(event)
            );
        }
        DamageSettlementCallbacks.dispatch(completion.snapshot());
        return true;
    }

    static void dispatch(long damageId, Runnable dispatch) {
        try {
            dispatch.run();
        } catch (Throwable throwable) {
            JvmFatalErrors.rethrowIfFatal(throwable);

            String type = throwable.getClass().getName();
            if (DamageModuleDiagnosticState.shouldLog(
                    DamageModuleDiagnosticState.Domain.EVENT_DISPATCH,
                    type,
                    "damage_settled_event",
                    "listener_failure"
            )) {
                DamageModule.LOGGER.error(
                        "[DamageModule] DamageSettledEvent listener failed "
                                + "after damage {} was committed; the "
                                + "settlement remains valid. exception={}",
                        damageId,
                        DiagnosticTextSanitizer.sanitizeLine(type)
                );
            }
        }
    }
}
