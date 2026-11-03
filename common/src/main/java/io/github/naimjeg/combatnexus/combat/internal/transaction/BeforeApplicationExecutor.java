package io.github.naimjeg.combatnexus.combat.internal.transaction;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
/** One executor for immediate and scheduled composition, using ordinary child admission. */
public final class BeforeApplicationExecutor {
    private BeforeApplicationExecutor() {}
    public static BeforeApplicationPlan.Result execute(CombatRuntime world, BeforeApplicationPlan plan) {
        if (!CombatExecutionScope.isCurrentPhase(CombatExecutionScope.Phase.APPLICATION))
            throw new IllegalStateException("Before-application plan requires parent application");
        var results = new ArrayList<BeforeApplicationPlan.StepEvidence>();
        for (var step : plan.steps()) {
            var result = DomainCommandExecutor.execute(world, step.command());
            results.add(new BeforeApplicationPlan.StepEvidence(results.size(), step.command().type(), result));
            if (!result.completed()) return new BeforeApplicationPlan.Result(results, false, false);
            if (!result.applied() && step.rejectionPolicy() == BeforeApplicationPlan.RejectionPolicy.ABORT_PARENT)
                return new BeforeApplicationPlan.Result(results, false, true);
        }
        return new BeforeApplicationPlan.Result(results, true, true);
    }
}
