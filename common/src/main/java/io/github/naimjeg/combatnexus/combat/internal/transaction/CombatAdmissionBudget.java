package io.github.naimjeg.combatnexus.combat.internal.transaction;
import io.github.naimjeg.combatnexus.combat.api.transaction.*;

/** One instance per server. Uses server ticks, not a level's time or wall clock. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class CombatAdmissionBudget {
    public enum Rejection { NONE, DEPTH, ROOT_BUDGET, TICK_BUDGET, COMPLETION_CAPACITY }
    public record Result(Rejection rejection, int derivedCount, int tickCount) {
        public boolean admitted() { return rejection == Rejection.NONE; }
    }
    private int tick;
    private boolean initialized;
    private int count;

    public synchronized Result admit(CombatLineage lineage, int currentTick, CombatLimits limits) {
        advance(currentTick);
        Rejection rejection = Rejection.NONE;
        if (lineage.depth() > limits.maximumDepth()) rejection = Rejection.DEPTH;
        else if (count >= limits.maximumPerTick()) rejection = Rejection.TICK_BUDGET;
        else if (lineage.parentId().isPresent() && !lineage.reserveDerived(limits.maximumDerived())) {
            rejection = Rejection.ROOT_BUDGET;
        }
        if (rejection == Rejection.NONE) count++;
        return new Result(rejection, lineage.derivedCount(), count);
    }
    public synchronized int count(int currentTick) { advance(currentTick); return count; }
    private void advance(int currentTick) {
        if (!initialized || tick != currentTick) { tick = currentTick; count = 0; initialized = true; }
    }
}

