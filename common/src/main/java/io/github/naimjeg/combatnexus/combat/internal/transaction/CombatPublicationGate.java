package io.github.naimjeg.combatnexus.combat.internal.transaction;

import java.util.concurrent.atomic.AtomicBoolean;

/** Internal completion capability. Publishing a result never rolls back an applied operation. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class CombatPublicationGate {
    private final AtomicBoolean claimed = new AtomicBoolean();
    public boolean claim() { return claimed.compareAndSet(false, true); }
    public boolean isClaimed() { return claimed.get(); }
}

