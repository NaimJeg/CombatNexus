package io.github.naimjeg.combatnexus.combat.internal.transaction;
import io.github.naimjeg.combatnexus.combat.api.transaction.*;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.ApiStatus;

/** Trusted dispatcher SPI. Keep each dispatcher's owner identity private; ordinary listeners only receive CombatParent. */
@ApiStatus.Internal
public final class CombatDispatchScope implements AutoCloseable {
    private static final ThreadLocal<CombatDispatchScope> CURRENT = new ThreadLocal<>();
    private final Thread thread = Thread.currentThread();
    private final Object owner;
    private final Object delivery;
    private final Object domainAuthority;
    private final Object server;
    private final CombatParent parent;
    private Set<CombatOperation> effectivePermissions;
    private boolean closed;

    private CombatDispatchScope(Object owner, Object delivery, Object domainAuthority, Object server,
                                CombatLineage lineage, Set<CombatOperation> allowed) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.delivery = Objects.requireNonNull(delivery, "delivery");
        this.domainAuthority = domainAuthority;
        this.server = Objects.requireNonNull(server, "server");
        var permissions = EnumSet.noneOf(CombatOperation.class);
        permissions.addAll(allowed);
        if (lineage != null) permissions.retainAll(lineage.permissions());
        effectivePermissions = Set.copyOf(permissions);
        parent = lineage == null || permissions.isEmpty() ? null : new CombatParent(server, lineage, permissions);
    }
    public static CombatDispatchScope observation(Object owner, Object delivery, Object server) {
        return open(owner, delivery, null, server, null, Set.of());
    }
    public static CombatDispatchScope callback(Object owner, Object delivery, Object domainAuthority,
                                               Object server, CombatLineage lineage, Set<CombatOperation> allowed) {
        return open(owner, delivery, domainAuthority, server, lineage, allowed);
    }
    private static CombatDispatchScope open(Object owner, Object delivery, Object domainAuthority,
                                            Object server, CombatLineage lineage, Set<CombatOperation> allowed) {
        if (CURRENT.get() != null) throw new IllegalStateException("Recursive combat settlement delivery");
        var scope = new CombatDispatchScope(owner, delivery, domainAuthority, server, lineage, allowed);
        CURRENT.set(scope);
        return scope;
    }
    public static CombatLineage implicitChild(Object server, CombatOperation operation) {
        var scope = CURRENT.get();
        if (scope == null || scope.parent == null) throw new IllegalStateException("Observation has no child authority");
        return scope.parent.child(server, operation);
    }
    public static boolean delivering(Object delivery, Object server) {
        var scope = CURRENT.get(); return scope != null && scope.delivery == delivery && scope.server == server;
    }
    public static void requireChildrenAllowed() {
        var scope = CURRENT.get();
        if (scope != null) {
            if (scope.parent == null) throw new IllegalStateException("Observation has no mutation authority");
            scope.parent.lineage().requireChildrenAllowed();
        }
    }
    public static void requireOperation(CombatOperation operation) {
        var scope = CURRENT.get();
        if (scope == null) return;
        requireChildrenAllowed();
        if (!scope.parent.permits(scope.server, operation)) throw new IllegalStateException("Callback does not authorize " + operation);
    }
    public static boolean isActive() { return CURRENT.get() != null; }
    public static boolean permits(CombatOperation operation) {
        var current = CURRENT.get();
        return current != null && current.effectivePermissions.contains(operation);
    }
    public static boolean accepts(Object owner, Object authority, Object server) {
        var current = CURRENT.get();
        return current != null && authority != null && current.owner == owner
                && current.domainAuthority == authority && current.server == server;
    }
    public static boolean exposes(Object owner, Object delivery, Object authority, Object server) {
        var current = CURRENT.get();
        return accepts(owner, authority, server) && current.delivery == delivery;
    }
    public static boolean accepts(CombatParent parent, Object server) {
        var current = CURRENT.get();
        return current != null && current.parent == parent && parent != null && current.server == server;
    }
    public static boolean permits(CombatParent parent, Object server, CombatOperation operation) {
        return accepts(parent, server) && CURRENT.get().effectivePermissions.contains(operation);
    }
    public static Restriction restrict(Set<CombatOperation> permissions) {
        var scope = Objects.requireNonNull(CURRENT.get(), "Reaction requires dispatch");
        var before = scope.effectivePermissions;
        var narrowed = EnumSet.noneOf(CombatOperation.class); narrowed.addAll(before); narrowed.retainAll(permissions);
        scope.effectivePermissions = Set.copyOf(narrowed);
        return new Restriction(scope, before);
    }
    public static final class Restriction implements AutoCloseable {
        private final CombatDispatchScope scope; private final Set<CombatOperation> before;
        private Restriction(CombatDispatchScope scope, Set<CombatOperation> before) { this.scope = scope; this.before = before; }
        public void close() { if (CURRENT.get() != scope) throw new IllegalStateException("Reaction scope expired"); scope.effectivePermissions = before; }
    }
    public static Optional<CombatParent> parentFor(Object delivery, Object server) {
        var current = CURRENT.get();
        return current != null && current.delivery == delivery && current.server == server
                ? Optional.ofNullable(current.parent) : Optional.empty();
    }
    @Override public void close() {
        if (closed) return;
        if (Thread.currentThread() != thread || CURRENT.get() != this) throw new IllegalStateException("Wrong combat delivery scope");
        CURRENT.remove();
        closed = true;
    }
}
