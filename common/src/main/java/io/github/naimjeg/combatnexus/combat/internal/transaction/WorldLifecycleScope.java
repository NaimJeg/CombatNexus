package io.github.naimjeg.combatnexus.combat.internal.transaction;

/** Kernel-issued lifecycle execution, separate from gameplay admission and its quotas. */
public final class WorldLifecycleScope {
    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<>();
    private WorldLifecycleScope() {}
    static void run(Runnable action) {
        int before = DEPTH.get() == null ? 0 : DEPTH.get(); DEPTH.set(before + 1);
        try { action.run(); } finally { if (before == 0) DEPTH.remove(); else DEPTH.set(before); }
    }
    public static void requireIdleOrLifecycle() {
        if (CombatExecutionScope.isActive() && DEPTH.get() == null)
            throw new IllegalStateException("World lifecycle mutation requires kernel authority");
    }
}
