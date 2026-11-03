package io.github.naimjeg.combatnexus.combat.api.platform;

/** Entity storage slot for an opaque adapter-owned ledger; it exposes no ledger mutation methods. */
public interface NativeAbsorptionStorage {
    interface State {}
    State combatnexus$absorptionState();
}
