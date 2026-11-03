package io.github.naimjeg.combatnexus.combat.internal.transaction;

import java.util.Objects;
import java.util.WeakHashMap;

/** Single-use commands while their identity is retained; no unbounded history of expired requests. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class CombatSubmissionGate<K> {
    private final WeakHashMap<K, Boolean> submitted = new WeakHashMap<>();
    public synchronized boolean claim(K identity) {
        return submitted.put(Objects.requireNonNull(identity, "identity"), Boolean.TRUE) == null;
    }
    public synchronized void clear() { submitted.clear(); }
}

