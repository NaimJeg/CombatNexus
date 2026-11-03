package io.github.naimjeg.combatnexus.combat.api.transaction;

/** A restriction only: input capture cannot acquire command authority from its surrounding application. */
public final class CombatReadScope implements AutoCloseable {
    private static final ThreadLocal<CombatReadScope> CURRENT = new ThreadLocal<>();
    private final CombatReadScope previous;
    private final Thread thread = Thread.currentThread();
    private boolean closed;
    private CombatReadScope() { previous = CURRENT.get(); CURRENT.set(this); }
    public static CombatReadScope open() { return new CombatReadScope(); }
    public static void requireWritable() {
        if (CURRENT.get() != null) throw new IllegalStateException("Input capture is read-only");
    }
    @Override public void close() {
        if (closed) return;
        if (Thread.currentThread() != thread || CURRENT.get() != this) throw new IllegalStateException("Read scope closed out of order");
        closed = true;
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    }
}
