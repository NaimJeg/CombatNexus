package io.github.naimjeg.combatnexus.combat.api.transaction;

import java.util.concurrent.atomic.AtomicBoolean;

/** Thread and lifetime mechanics only. Each domain keeps its own non-public capability constructor. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class CombatRegistrationLease {
    private final Thread owner = Thread.currentThread();
    private final AtomicBoolean active = new AtomicBoolean(true);
    public void requireActive(String action) {
        if (!active.get() || Thread.currentThread() != owner) {
            throw new IllegalStateException("Registration lease is expired or on another thread: " + action);
        }
    }
    public boolean isActive() { return active.get(); }
    public void close() { active.set(false); }
}
