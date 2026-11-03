package io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla;

/** A producer writes the next invocation; an active consumer sees only its own frame. */
final class CaptureSlot<T> {
    private static final class Frame<T> { T value, next; Frame<T> parent; }
    private final ThreadLocal<Frame<T>> current = new ThreadLocal<>();
    private final ThreadLocal<T> pending = new ThreadLocal<>();
    void set(T value) { var frame = current.get(); if (frame == null) pending.set(value); else frame.next = value; }
    T get() { var frame = current.get(); return frame == null ? pending.get() : frame.value; }
    void remove() { var frame = current.get(); if (frame == null) pending.remove(); else frame.value = null; }
    void clearPending() { var frame = current.get(); if (frame == null) pending.remove(); else frame.next = null; }
    Runnable open() {
        var parent = current.get(); var frame = new Frame<T>(); frame.parent = parent;
        frame.value = parent == null ? pending.get() : parent.next;
        clearPending(); current.set(frame);
        var thread = Thread.currentThread();
        return () -> {
            if (Thread.currentThread() != thread || current.get() != frame) throw new IllegalStateException("Capture scope order");
            if (parent == null) current.remove(); else current.set(parent);
        };
    }
}
