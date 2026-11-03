package io.github.naimjeg.combatnexus.combat.internal.transaction;
import io.github.naimjeg.combatnexus.combat.api.transaction.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

/** Per-server FIFO; thread-local coordination prevents recursive delivery across server/world sessions. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class CombatCompletionQueue {
    private static final ThreadLocal<Set<CombatCompletionQueue>> READY = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> DRAINING = new ThreadLocal<>();
    private final Deque<Runnable> pending = new ArrayDeque<>();
    private int reserved;
    private long epoch;
    private final int capacity;
    public CombatCompletionQueue() { this(CombatLimits.HARD_MAX_PER_TICK); }
    public CombatCompletionQueue(int capacity) {
        if (capacity < 1 || capacity > CombatLimits.HARD_MAX_PER_TICK) throw new IllegalArgumentException("Invalid completion capacity");
        this.capacity = capacity;
    }

    public Slot reserve() {
        if ((long) pending.size() + reserved >= capacity)
            throw new IllegalStateException("Combat completion capacity exceeded before application");
        reserved++;
        return new Slot();
    }
    public final class Slot implements AutoCloseable {
        private boolean open = true;
        private final long generation = epoch;
        public void publish(Runnable publication) {
            java.util.Objects.requireNonNull(publication);
            if (!open || generation != epoch) throw new IllegalStateException("Completion slot already consumed");
            close();
            append(publication);
        }
        @Override public void close() { if (open) { open = false; if (generation == epoch) reserved--; } }
    }

    private void append(Runnable publication) {
        pending.addLast(java.util.Objects.requireNonNull(publication, "publication"));
        var ready = READY.get();
        if (ready == null) { ready = new LinkedHashSet<>(); READY.set(ready); }
        ready.add(this);
    }
    public static void drainIfSafe() {
        if (CombatExecutionScope.isActive() || CombatDispatchScope.isActive() || draining()) return;
        var ready = READY.get();
        if (ready == null) return;
        DRAINING.set(true);
        try {
            while (!ready.isEmpty()) {
                CombatCompletionQueue queue = ready.iterator().next();
                while (!queue.pending.isEmpty()) {
                    try { queue.pending.removeFirst().run(); }
                    catch (RuntimeException failure) {
                    }
                }
                ready.remove(queue);
            }
        } finally {
            DRAINING.remove();
            if (ready.isEmpty()) READY.remove();
        }
    }
    public static boolean draining() { return Boolean.TRUE.equals(DRAINING.get()); }
    public static int pendingCount() {
        var ready = READY.get();
        return ready == null ? 0 : ready.stream().mapToInt(queue -> queue.pending.size()).sum();
    }
    public void clear() {
        pending.clear();
        reserved = 0; epoch++;
        var ready = READY.get();
        if (ready != null) { ready.remove(this); if (ready.isEmpty()) READY.remove(); }
    }
}

