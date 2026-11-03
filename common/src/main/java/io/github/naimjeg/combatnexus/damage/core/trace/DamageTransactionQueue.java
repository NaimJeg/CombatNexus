package io.github.naimjeg.combatnexus.damage.core.trace;

import java.util.ArrayDeque;
import java.util.Deque;

public final class DamageTransactionQueue {

    private final Deque<DamageModuleTransaction> entries =
            new ArrayDeque<>();

    public Deque<DamageModuleTransaction> entries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public void addLast(DamageModuleTransaction tx) {
        entries.addLast(tx);
    }

    public DamageModuleTransaction removeFirst() {
        return entries.removeFirst();
    }
}
