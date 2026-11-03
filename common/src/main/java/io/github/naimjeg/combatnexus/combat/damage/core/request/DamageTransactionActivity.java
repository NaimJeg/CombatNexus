package io.github.naimjeg.combatnexus.combat.damage.core.request;

import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope;

/** Damage computation facade over the shared combat execution stack. */
public final class DamageTransactionActivity {
    private DamageTransactionActivity() {}
    public static Scope enter() { return new Scope(CombatExecutionScope.computation()); }
    public static boolean isActive() { return CombatExecutionScope.isActive(CombatExecutionScope.Phase.COMPUTE); }
    public static final class Scope implements AutoCloseable {
        private final CombatExecutionScope delegate;
        private Scope(CombatExecutionScope delegate) { this.delegate = delegate; }
        @Override public void close() { delegate.close(); }
    }
}
