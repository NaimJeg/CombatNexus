package io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla;

/** All captures are isolated before the native call can invoke another attack. */
public final class NativeDamageCaptures implements AutoCloseable {
    private final java.util.List<Runnable> restore;
    private boolean closed;
    private NativeDamageCaptures() {
        restore = java.util.List.of(VanillaDamageCapture.OFFENSIVE_ENCHANT.open(), VanillaCriticalCapture.PENDING_CRIT.open(),
                MaceDamageCapture.CURRENT.open(), SpearDamageCapture.CURRENT.open(), ProjectileDamageCapture.CURRENT.open());
    }
    public static NativeDamageCaptures open() { return new NativeDamageCaptures(); }
    @Override public void close() {
        if (closed) return;
        for (int i = restore.size() - 1; i >= 0; i--) restore.get(i).run();
        closed = true;
    }
}
