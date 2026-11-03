package io.github.naimjeg.combatnexus.combat.internal.transaction;

import io.github.naimjeg.combatnexus.combat.api.transaction.CombatLineage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

/** Adapter SPI for mutable execution. Close order is checked across all combat subdomains. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class CombatExecutionScope implements AutoCloseable {
    public enum Phase { COMPUTE, SUBMISSION, APPLICATION }
    private static final ThreadLocal<Deque<CombatExecutionScope>> ACTIVE = new ThreadLocal<>();
    private final Thread owner = Thread.currentThread();
    private final Phase phase;
    private final Object server;
    private final CombatServerRuntime.ApplicationPermit permit;
    private final boolean ownsPermit;
    private final CombatLineage lineage;
    private boolean closed;
    private CombatExecutionScope(Phase phase, CombatServerRuntime.ApplicationPermit permit, boolean ownsPermit) {
        this.ownsPermit = ownsPermit;
        this.phase = Objects.requireNonNull(phase, "phase");
        this.permit = Objects.requireNonNull(permit);
        permit.requireActive();
        this.server = permit.serverIdentity();
        this.lineage = permit.lineage();
    }
    static CombatExecutionScope enter(Phase phase, CombatServerRuntime.ApplicationPermit permit) {
        return open(phase, permit, true);
    }
    private static CombatExecutionScope open(Phase phase, CombatServerRuntime.ApplicationPermit permit, boolean ownsPermit) {
        Deque<CombatExecutionScope> stack = ACTIVE.get();
        if (stack == null) { stack = new ArrayDeque<>(); ACTIVE.set(stack); }
        CombatExecutionScope scope = new CombatExecutionScope(phase, permit, ownsPermit);
        stack.addLast(scope);
        return scope;
    }
    public static CombatExecutionScope computation() {
        var stack = ACTIVE.get();
        if (stack == null || stack.isEmpty() || stack.peekLast().lineage == null)
            throw new IllegalStateException("Computation requires an authorized application");
        var current = stack.peekLast(); current.permit.requireActive();
        return open(Phase.COMPUTE, current.permit, false);
    }
    public static boolean isActive() { var stack = ACTIVE.get(); return stack != null && !stack.isEmpty(); }
    public static boolean isCurrentPhase(Phase phase) {
        var stack = ACTIVE.get();
        return stack != null && !stack.isEmpty() && stack.peekLast().phase == phase;
    }
    public static boolean isActive(Phase phase) {
        var stack = ACTIVE.get();
        return stack != null && stack.stream().anyMatch(scope -> scope.phase == phase);
    }
    /** Never skip an unbound/mismatched inner operation to borrow an ancestor's authority. */
    public static Optional<CombatLineage> currentLineage(Object server) {
        var stack = ACTIVE.get();
        if (stack == null || stack.isEmpty()) return Optional.empty();
        var scope = stack.peekLast();
        scope.permit.requireActive();
        if (scope.server != server) throw new IllegalStateException("Cross-server nested combat");
        return Optional.ofNullable(scope.lineage);
    }
    public static void requireSubmissionPhase() {
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatReadScope.requireWritable();
        if (isActive(Phase.COMPUTE) || isActive() && !isCurrentPhase(Phase.APPLICATION))
            throw new IllegalStateException("Combat submission requires idle or APPLICATION phase");
    }
    public static void requireNativeWritePhase() {
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatReadScope.requireWritable();
        CombatDispatchScope.requireChildrenAllowed();
        var stack = ACTIVE.get();
        if (stack != null && !stack.isEmpty()) stack.peekLast().lineage.requireChildrenAllowed();
        if (isActive(Phase.COMPUTE)) throw new IllegalStateException("World writes forbidden during COMPUTE");
    }
    @Override public void close() {
        if (closed) return;
        var stack = ACTIVE.get();
        if (Thread.currentThread() != owner || stack == null || stack.peekLast() != this) {
            throw new IllegalStateException("Combat execution scope closed out of order");
        }
        stack.removeLast();
        if (ownsPermit) permit.close();
        if (stack.isEmpty()) ACTIVE.remove();
        closed = true;
    }
}

