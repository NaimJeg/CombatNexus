package io.github.naimjeg.combatnexus.combat.damage.core.lifecycle;

import io.github.naimjeg.combatnexus.combat.api.transaction.CombatRegistrationLease;

/** Damage-owned capability; only the framework lifecycle may construct it. */
public final class DamageModuleRegistrationAccess {
    private final CombatRegistrationLease lease = new CombatRegistrationLease();
    DamageModuleRegistrationAccess() {}
    public void requireActive(String action) { lease.requireActive(action); }
    void close() { lease.close(); }
    boolean isActive() { return lease.isActive(); }
}
