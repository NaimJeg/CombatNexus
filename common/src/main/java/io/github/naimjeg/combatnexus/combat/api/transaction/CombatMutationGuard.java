package io.github.naimjeg.combatnexus.combat.api.transaction;

/** Read-only world-write guard; it does not issue authority or open execution scopes. */
public final class CombatMutationGuard {
    private CombatMutationGuard() {}
    public static void requireWorldWritePhase() {
        CombatReadScope.requireWritable();
        if (io.github.naimjeg.combatnexus.combat.internal.transaction.StateCommands.isApplying()) return;
        io.github.naimjeg.combatnexus.combat.internal.transaction.WorldLifecycleScope.requireIdleOrLifecycle();
        io.github.naimjeg.combatnexus.combat.internal.transaction.CombatDispatchScope.requireChildrenAllowed();
    }
    public static void requireWorldWritePhase(CombatOperation operation) {
        requireWorldWritePhase();
        if (!io.github.naimjeg.combatnexus.combat.internal.transaction.StateCommands.isApplying())
            io.github.naimjeg.combatnexus.combat.internal.transaction.CombatDispatchScope.requireOperation(operation);
    }
    /** Checks a native resource write without granting or extending application authority. */
    public static void requireNativeWritePhase() {
        io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope.requireNativeWritePhase();
    }
    public static boolean isExecuting() {
        return io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope.isActive();
    }
}
