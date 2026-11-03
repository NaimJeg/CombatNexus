package io.github.naimjeg.combatnexus.combat.damage.core.settlement;

import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot;

import java.util.Objects;

/** Internal one-shot publication capability for a completed settlement. */
public final class DamageSettlementCompletion {

    private final DamageSettlementSnapshot snapshot;
    private final DamageSettlementState state;
    private final DamageSettlementPublicationGate publicationGate =
            new DamageSettlementPublicationGate();

    DamageSettlementCompletion(
            DamageSettlementSnapshot snapshot,
            DamageSettlementState state
    ) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.state = Objects.requireNonNull(state, "state");
    }

    public DamageSettlementSnapshot snapshot() {
        return snapshot;
    }

    void enqueue(Runnable delivery) { state.enqueue(delivery); }

    boolean claimPublication() {
        if (!publicationGate.claim()) {
            return false;
        }
        state.markPublished();
        return true;
    }

    boolean publicationClaimedForTests() {
        return publicationGate.isClaimed();
    }
}
