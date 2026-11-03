package io.github.naimjeg.combatnexus.combat.damage.api.damage;

import io.github.naimjeg.combatnexus.combat.api.transaction.*;
import java.util.OptionalLong;

/** Damage-facing view of the shared combat identity. Identity alone never grants child authority. */
public final class DamageLineage {
    private final CombatLineage transaction;
    private DamageLineage(CombatLineage transaction) { this.transaction = transaction; }
    public static DamageLineage newRoot() { return new DamageLineage(CombatLineage.root(CombatOperation.DAMAGE)); }
    public DamageLineage newChild() { return new DamageLineage(transaction.child(CombatOperation.DAMAGE)); }
    static DamageLineage fromCombatParent(CombatParent parent, Object server) {
        return new DamageLineage(parent.child(server, CombatOperation.DAMAGE));
    }
    @org.jetbrains.annotations.ApiStatus.Internal
    public static DamageLineage execution(CombatLineage transaction) {
        if (transaction.operation() != CombatOperation.DAMAGE) throw new IllegalArgumentException("Not damage");
        return new DamageLineage(transaction);
    }
    public CombatLineage transaction() { return transaction; }
    public long damageId() { return transaction.id(); }
    public long rootDamageId() { return transaction.rootId(); }
    /** With cross-domain ancestry these IDs may identify healing or shield operations. */
    public OptionalLong parentDamageId() { return transaction.parentId(); }
    public int recursionDepth() { return transaction.depth(); }
    public boolean hasParent() { return transaction.parentId().isPresent(); }
    public boolean reserveDerivedRequestInternal(int maximum) { return transaction.reserveDerived(maximum); }
    public int derivedRequestCountInternal() { return transaction.derivedCount(); }
    @Override public boolean equals(Object other) {
        return other instanceof DamageLineage lineage && transaction.equals(lineage.transaction);
    }
    @Override public int hashCode() { return transaction.hashCode(); }
    @Override public String toString() { return "DamageLineage[" + transaction + "]"; }
}
