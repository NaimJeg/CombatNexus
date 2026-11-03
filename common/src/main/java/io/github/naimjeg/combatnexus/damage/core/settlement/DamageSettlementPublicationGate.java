package io.github.naimjeg.combatnexus.damage.core.settlement;

import java.util.concurrent.atomic.AtomicBoolean;

/** One-shot gate rejecting duplicate settlement publication. */
final class DamageSettlementPublicationGate {

    private final AtomicBoolean claimed = new AtomicBoolean();

    boolean claim() {
        return claimed.compareAndSet(false, true);
    }

    boolean isClaimed() {
        return claimed.get();
    }
}
