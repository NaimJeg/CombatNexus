package io.github.naimjeg.combatnexus.combat.api.transaction;

import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

/** Causal identity only, never permission to submit a child. The root owns its entire derivation budget. */
public final class CombatLineage {
    private static final AtomicLong NEXT_ID = new AtomicLong();
    private static final class Root {
        int derived;
        Object server;
    }
    private final long id;
    private final long rootId;
    private final OptionalLong parentId;
    private final int depth;
    private final CombatOperation operation;
    private final Root root;
    private Set<CombatOperation> permissions;
    /** Restricts descendants; this cannot add permissions or create admission authority. */
    public void restrictOperations(Set<CombatOperation> allowed) {
        var narrowed=EnumSet.noneOf(CombatOperation.class);narrowed.addAll(permissions);narrowed.retainAll(allowed);permissions=Set.copyOf(narrowed);
    }
    private String rejectedChildren;
    /** Monotonic causal restriction, never authority. Does not affect sibling targets. */
    public void rejectChildren(String reason) { rejectedChildren = Objects.requireNonNull(reason); }
    public void requireChildrenAllowed() {
        if (rejectedChildren != null) throw new IllegalStateException("Combat descendants rejected: " + rejectedChildren);
    }
    private Set<net.minecraft.resources.Identifier> suppressedTriggers = Set.of();
    /** Restrictions only tighten; IDs are semantic constraints, never submission authority. */
    public void suppressTriggers(Set<net.minecraft.resources.Identifier> additional) {
        var union = new java.util.HashSet<>(suppressedTriggers); union.addAll(additional);
        suppressedTriggers = Set.copyOf(union);
    }
    public Set<net.minecraft.resources.Identifier> suppressedTriggers() { return suppressedTriggers; }

    private CombatLineage(long rootId, OptionalLong parentId, int depth, CombatOperation operation, Root root,
                          Set<CombatOperation> permissions) {
        this.id = NEXT_ID.incrementAndGet();
        if (id <= 0) throw new IllegalStateException("Combat transaction identifiers exhausted");
        this.rootId = rootId == 0 ? id : rootId;
        this.parentId = parentId;
        this.depth = depth;
        this.operation = Objects.requireNonNull(operation, "operation");
        this.root = root;
        this.permissions = Set.copyOf(permissions);
    }
    public static CombatLineage root(CombatOperation operation) {
        return new CombatLineage(0, OptionalLong.empty(), 0, operation, new Root(), EnumSet.allOf(CombatOperation.class));
    }
    public CombatLineage child(CombatOperation operation) {
        return child(operation, permissions);
    }
    CombatLineage child(CombatOperation operation, Set<CombatOperation> restrictions) {
        requireChildrenAllowed();
        var allowed = EnumSet.noneOf(CombatOperation.class);
        allowed.addAll(permissions);
        allowed.retainAll(restrictions);
        var child = new CombatLineage(rootId, OptionalLong.of(id), Math.incrementExact(depth), operation, root, allowed);
        child.suppressedTriggers = suppressedTriggers;
        return child;
    }
    public Set<CombatOperation> permissions() { return permissions; }
    public long id() { return id; }
    public long rootId() { return rootId; }
    public OptionalLong parentId() { return parentId; }
    public int depth() { return depth; }
    public CombatOperation operation() { return operation; }
    public int derivedCount() { synchronized (root) { return root.derived; } }

    /** Admission SPI. A failed server-tick check must happen before reserving a root slot. */
    public boolean reserveDerived(int maximum) {
        if (parentId.isEmpty()) throw new IllegalStateException("Root cannot reserve a derived slot");
        if (maximum < 1) throw new IllegalArgumentException("Invalid derivation limit");
        synchronized (root) {
            if (root.derived >= maximum) return false;
            root.derived++;
            return true;
        }
    }
    public boolean bindServer(Object server) {
        Objects.requireNonNull(server, "server");
        synchronized (root) {
            if (root.server == null) root.server = server;
            return root.server == server;
        }
    }
    @Override public boolean equals(Object other) { return other instanceof CombatLineage lineage && id == lineage.id; }
    @Override public int hashCode() { return Long.hashCode(id); }
    @Override public String toString() {
        return "CombatLineage[id=" + id + ", rootId=" + rootId + ", parentId=" + parentId
                + ", depth=" + depth + ", operation=" + operation + "]";
    }
}
