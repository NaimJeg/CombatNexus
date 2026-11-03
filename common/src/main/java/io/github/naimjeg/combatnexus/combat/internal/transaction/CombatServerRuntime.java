package io.github.naimjeg.combatnexus.combat.internal.transaction;
import io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;

import io.github.naimjeg.combatnexus.combat.api.transaction.*;
import io.github.naimjeg.combatnexus.combat.api.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** One execution kernel per MinecraftServer; world services share its quotas and completion FIFO. */
public final class CombatServerRuntime implements AutoCloseable {
    private static final Map<MinecraftServer, CombatServerRuntime> SERVERS = new IdentityHashMap<>();
    private static final Map<MinecraftServer, Boolean> STOPPED = new WeakHashMap<>();
    private volatile CombatLimits configuredLimits = CombatLimits.DEFAULT;
    private final MinecraftServer server;
    private final CombatAdmissionBudget budget = new CombatAdmissionBudget();
    private final CombatCompletionQueue completions = new CombatCompletionQueue();
    private final Map<ServerLevel, CombatRuntime> worlds = new IdentityHashMap<>();
    private final Map<CombatOperation, CombatSubmissionGate<Object>> submissions = new EnumMap<>(CombatOperation.class);
    private boolean closed;
    private DefinitionGeneration definitions;
    public Optional<DefinitionGeneration> definitions() { requireThread(); return Optional.ofNullable(definitions); }
    public void adoptDefinitions(DefinitionGeneration candidate) {
        requireThread(); Objects.requireNonNull(candidate);
        if (definitions != null && candidate.generation() <= definitions.generation()) throw new IllegalArgumentException("Generations must increase");
        configuredLimits = candidate.settings().safety().combatLimits();
        definitions = candidate;
        io.github.naimjeg.combatnexus.combat.internal.registration.CombatExtensions.definitions(server,new DefinitionView(candidate.generation(),candidate.domains().effects().keySet(),candidate.behaviors().keySet(),candidate.domains().shields().keySet(),candidate.domains().regeneration().keySet(),candidate.domains().healingRules().keySet()));
    }
    public void clearDefinitions() { requireThread(); definitions = null; }

    private CombatServerRuntime(MinecraftServer server) { this.server = server; }
    public static synchronized CombatServerRuntime of(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        if (STOPPED.containsKey(server)) throw new IllegalStateException("Server combat session stopped");
        return SERVERS.computeIfAbsent(server, CombatServerRuntime::new);
    }
    public static synchronized Optional<CombatServerRuntime> existing(MinecraftServer server) { return Optional.ofNullable(SERVERS.get(server)); }
    public static void stop(MinecraftServer server) {
        CombatServerRuntime runtime;
        synchronized (CombatServerRuntime.class) { runtime = SERVERS.get(server); }
        if (runtime != null) runtime.close();
    }
    public MinecraftServer server() { return server; }
    public CombatLimits limits() { return configuredLimits; }
    public void requireThread() {
        if (closed || !server.isSameThread()) throw new IllegalStateException("Combat requires a live owning server thread");
    }
    public CombatAdmissionBudget.Result admit(CombatLineage lineage) {
        requireThread();
        lineage.restrictOperations(io.github.naimjeg.combatnexus.combat.internal.coordination.BridgeExecutions.allowedOperations());
        if (!lineage.bindServer(server)) throw new IllegalArgumentException("Combat lineage belongs to another server");
        if (tickets.containsKey(lineage)) throw new IllegalStateException("Operation already admitted");
        var slots = new ArrayDeque<CombatCompletionQueue.Slot>();
        try {
            for (int index = 0; index < (lineage.operation() == CombatOperation.DAMAGE ? 2 : 1); index++)
                slots.addLast(completions.reserve());
        } catch (IllegalStateException exhausted) {
            slots.forEach(CombatCompletionQueue.Slot::close);
            return new CombatAdmissionBudget.Result(CombatAdmissionBudget.Rejection.COMPLETION_CAPACITY,
                    lineage.derivedCount(), budget.count(server.getTickCount()));
        }
        var result = budget.admit(lineage, server.getTickCount(), limits());
        if (result.admitted()) tickets.put(lineage, new AdmissionTicket(lineage, slots));
        else slots.forEach(CombatCompletionQueue.Slot::close);
        return result;
    }

    private final Map<CombatLineage, AdmissionTicket> tickets = new IdentityHashMap<>();
    public boolean hasTicket(CombatLineage lineage) { requireThread(); return tickets.containsKey(lineage); }
    public AdmissionTicket ticket(CombatLineage lineage) {
        requireThread();
        return Objects.requireNonNull(tickets.get(lineage), "No live admission ticket");
    }
    public final class AdmissionTicket implements AutoCloseable {
        public Object serverIdentity() { return server; }
        private final CombatLineage lineage;
        private final DefinitionGeneration definitions = CombatServerRuntime.this.definitions;
        public DefinitionGeneration definitions() { requireActive(); return Objects.requireNonNull(definitions, "No adopted definitions"); }
        private boolean active = true;
        private final Deque<CombatCompletionQueue.Slot> slots;
        private ServerLevel level;
        private UUID target;
        private CombatRuntime world;
        public void bindSession(ServerLevel level, UUID target) {
            requireActive();
            if (level.getServer() != server || this.level != null && (this.level != level || !this.target.equals(target)))
                throw new IllegalArgumentException("Ticket belongs to a different world or target");
            this.level = level; this.target = Objects.requireNonNull(target);
            this.world = Objects.requireNonNull(worlds.get(level), "World combat session unavailable");
        }
        private AdmissionTicket(CombatLineage lineage, Deque<CombatCompletionQueue.Slot> slots) { this.lineage = lineage; this.slots = slots; }
        public void requireActive() {
            requireThread();
            if (!active || tickets.get(lineage) != this || level != null && worlds.get(level) != world) throw new IllegalStateException("Admission expired");
        }
        public CombatExecutionScope enter(CombatExecutionScope.Phase phase) {
            requireActive(); return CombatExecutionScope.enter(phase, new ApplicationPermit(this));
        }
        public CombatLineage lineage() { requireActive(); return lineage; }
        public void close() { requireThread(); if (active) { active = false; slots.forEach(CombatCompletionQueue.Slot::close); slots.clear(); tickets.remove(lineage, this); } }
    }
    public final class ApplicationPermit {
        private final AdmissionTicket ticket;
        private boolean active = true;
        private ApplicationPermit(AdmissionTicket ticket) { this.ticket = Objects.requireNonNull(ticket); }
        public Object serverIdentity() { return server; }
        public CombatLineage lineage() { return ticket.lineage; }
        public void requireActive() {
            if (!active) throw new IllegalStateException("Application session expired");
            ticket.requireActive();
        }
        public void close() { active = false; }
    }
    /** A native method starts only after its independent ticket is admitted and bound. */
    public CombatExecutionScope nativeApplication(ServerLevel level, net.minecraft.world.entity.LivingEntity target, CombatLineage lineage) {
        var ticket = ticket(lineage);
        ticket.bindSession(level, target.getUUID());
        return ticket.enter(CombatExecutionScope.Phase.APPLICATION);
    }
    public void finish(CombatLineage lineage) {
        requireThread(); var ticket = tickets.get(lineage); if (ticket != null) ticket.close();
    }

    public int currentTickCount() { requireThread(); return budget.count(server.getTickCount()); }
    public synchronized boolean claim(CombatOperation operation, Object requestIdentity) {
        if (closed) return false;
        return submissions.computeIfAbsent(operation, ignored -> new CombatSubmissionGate<>()).claim(requestIdentity);
    }
    /** Public services call this before admission. Mutable cross-domain recursion must not invent new roots. */
    public CombatLineage requestLineage(CombatOperation operation, CombatParent parent) {
        requireThread();
        CombatExecutionScope.requireSubmissionPhase();
        io.github.naimjeg.combatnexus.combat.internal.coordination.BridgeExecutions.require(operation);
        if (CombatExecutionScope.isActive()) {
            if (parent != null) throw new IllegalStateException("Cannot replace the active application parent");
            var active = CombatExecutionScope.currentLineage(server).orElseThrow();
            ticket(active).requireActive();
            if (!active.permissions().contains(operation)) throw new IllegalStateException("Operation restricted by parent");
            return active.child(operation);
        }
        if (parent != null) return parent.child(server, operation);
        if (CombatDispatchScope.isActive()) return CombatDispatchScope.implicitChild(server, operation);
        return CombatLineage.root(operation);
    }
    /** Shield consumption enlisted in a live application shares the already admitted application authority; it is not another request. */
    public CombatLineage enlistedLineage(CombatOperation operation, long applicationId) {
        requireThread();
        var current = CombatExecutionScope.currentLineage(server);
        if (current.isEmpty() || current.get().id() != applicationId
                || operation != CombatOperation.SHIELD || current.get().operation() != CombatOperation.DAMAGE
                || !CombatExecutionScope.isCurrentPhase(CombatExecutionScope.Phase.APPLICATION)) {
            throw new IllegalStateException("Shield enlistment requires the exact active application");
        }
        return current.get();
    }
    public CombatCompletionQueue.Slot reserveCompletion(CombatLineage lineage) {
        var ticket = ticket(lineage); ticket.requireActive();
        if (ticket.slots.isEmpty()) throw new IllegalStateException("Admitted completion envelope exhausted");
        return ticket.slots.removeFirst();
    }
    public Optional<CombatCompletionQueue.Slot> rejectionCompletion() {
        requireThread();
        try { return Optional.of(completions.reserve()); } catch (IllegalStateException full) { return Optional.empty(); }
    }
    public void drain() { requireThread(); CombatCompletionQueue.drainIfSafe(); }
    public void lifecycle(CombatRuntime world, Runnable action) {
        requireThread();
        if (!worlds.containsValue(world)) throw new IllegalStateException("World session unavailable");
        WorldLifecycleScope.run(action);
    }
    public Optional<CombatRuntime> world(ServerLevel level) { requireThread(); return Optional.ofNullable(worlds.get(level)); }
    public void attach(ServerLevel level, CombatRuntime world) {
        requireThread();
        if (level.getServer() != server || worlds.putIfAbsent(level, world) != null) {
            throw new IllegalStateException("World already attached or belongs to another server");
        }
    }
    public void detach(ServerLevel level, CombatRuntime world) { requireThread(); worlds.remove(level, world); }
    @Override public void close() {
        if (closed) return;
        requireThread();
        if (CombatExecutionScope.isActive() || CombatDispatchScope.isActive()) {
            throw new IllegalStateException("Cannot close combat server during a transaction or delivery");
        }
        drain();
        for (CombatRuntime world : List.copyOf(worlds.values())) io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess.close(world);
        worlds.clear();
        completions.clear();
        definitions = null;
        tickets.values().forEach(value -> value.active = false);
        tickets.clear();
        synchronized (this) {
            submissions.values().forEach(CombatSubmissionGate::clear);
            submissions.clear();
            closed = true;
        }
        synchronized (CombatServerRuntime.class) { SERVERS.remove(server, this); STOPPED.put(server, Boolean.TRUE); }
    }
}

