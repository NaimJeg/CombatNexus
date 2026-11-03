package io.github.naimjeg.combatnexus.combat.api.bridge;

/** Restricts command authority; it cannot create a transaction or extend its lifetime. */
public interface BridgeCall extends AutoCloseable {
    void nativeContribution(BridgeInvocation.Contribution contribution);
    BridgeInvocation snapshot();
    void returnedNormally();
    @Override void close();
}
