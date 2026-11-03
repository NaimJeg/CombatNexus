package io.github.naimjeg.combatnexus.combat.damage.core.settlement;

import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot;
import io.github.naimjeg.combatnexus.combat.damage.core.request.DamageRequestSubmissionTracker;
import io.github.naimjeg.combatnexus.combat.damage.core.request.DamageTransactionActivity;
import io.github.naimjeg.combatnexus.combat.damage.core.util.JvmFatalErrors;
import java.util.Objects;
import java.util.function.Consumer;

/** Publishes a completion once, after all mutable transaction state is gone. */
public final class DamageSettlementEventPublisher {
    private static volatile Consumer<DamageSettlementSnapshot> observer;

    /** Installed during loader construction, before any world can submit damage. */
    public static synchronized void install(Consumer<DamageSettlementSnapshot> value) {
        if (observer != null) throw new IllegalStateException("Settlement observer already installed");
        observer = Objects.requireNonNull(value);
    }

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

        var dispatchObserver = Objects.requireNonNull(observer, "Settlement observer not installed");
        try (DamageSettlementDispatchScope.Scope ignored =
                     DamageSettlementDispatchScope.openObservation(
                             completion.snapshot(),
                             completion.snapshot().level().getServer())) {
            dispatch(
                    completion.snapshot().lineage().damageId(),
                    () -> dispatchObserver.accept(completion.snapshot())
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

        }
    }
}
