package io.github.naimjeg.combatnexus.combat.damage.core.settlement;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatDispatchScope;

import io.github.naimjeg.combatnexus.combat.api.transaction.*;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageParentRef;
import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Private owner identity preserves damage callback authority while sharing delivery exclusion. */
public final class DamageSettlementDispatchScope {
    private static final Object OWNER = new Object();
    private DamageSettlementDispatchScope() {}
    static Scope openObservation(Object delivery, Object serverIdentity) {
        return new Scope(CombatDispatchScope.observation(OWNER, delivery, serverIdentity));
    }
    static Scope openCallback(Object delivery, @Nullable DamageParentRef authority, Object serverIdentity) {
        Set<CombatOperation> allowed = authority != null && authority.triggerPolicy().procAllowed()
                ? EnumSet.allOf(CombatOperation.class) : Set.of();
        return new Scope(CombatDispatchScope.callback(OWNER, delivery, authority, serverIdentity,
                authority == null ? null : authority.lineage().transaction(), allowed));
    }
    public static boolean isActive() { return CombatDispatchScope.isActive(); }
    public static boolean exposesAuthority(Object delivery, @Nullable DamageParentRef authority, Object serverIdentity) {
        return CombatDispatchScope.permits(CombatOperation.DAMAGE) && CombatDispatchScope.exposes(OWNER, delivery, authority, serverIdentity);
    }
    public static boolean accepts(@Nullable DamageParentRef authority, Object serverIdentity) {
        return CombatDispatchScope.permits(CombatOperation.DAMAGE) && CombatDispatchScope.accepts(OWNER, authority, serverIdentity);
    }
    public static int depthForTests() { return isActive() ? 1 : 0; }
    static final class Scope implements AutoCloseable {
        private final CombatDispatchScope delegate;
        private Scope(CombatDispatchScope delegate) { this.delegate = delegate; }
        @Override public void close() { delegate.close(); }
    }
}
