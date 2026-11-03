package io.github.naimjeg.combatnexus.combat.internal.transaction;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;

import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.*;
import java.util.*;
import java.util.function.Supplier;

/** Kernel admission for non-health state commands. No public service can issue application authority. */
public final class StateCommands {
    private static final ThreadLocal<CombatServerRuntime.AdmissionTicket> APPLYING = new ThreadLocal<>();
    private StateCommands() {}
    public static boolean isApplying() {
        var ticket = APPLYING.get();
        if (ticket == null || !CombatExecutionScope.isCurrentPhase(CombatExecutionScope.Phase.APPLICATION)
                || CombatExecutionScope.isActive(CombatExecutionScope.Phase.COMPUTE)) return false;
        ticket.requireActive();
        return CombatExecutionScope.currentLineage(ticket.serverIdentity()).orElse(null) == ticket.lineage();
    }
    public static <T> CommandResult<T> submit(CombatRuntime world, UUID request, UUID target,
            CombatOperation operation, CombatParent parent, Supplier<CommandResult<T>> action) {
        return submit(world, request, target, operation, Optional.empty(), parent, action);
    }
    public static <T> CommandResult<T> submit(CombatRuntime world, UUID request, UUID target,
            CombatOperation operation, Optional<CombatSource> source, CombatParent parent, Supplier<CommandResult<T>> action) {
        var context = new CommandContext(request, target, operation, source, Optional.empty());
        var result = execute(world, request, target, operation, source, parent, action);
        result=result.context().isPresent() ? result : result.withContext(context);
        io.github.naimjeg.combatnexus.combat.internal.coordination.BridgeExecutions.record(result);
        return result;
    }
    private static <T> CommandResult<T> execute(CombatRuntime world, UUID request, UUID target,
            CombatOperation operation, Optional<CombatSource> source, CombatParent parent, Supplier<CommandResult<T>> action) {
        var level = world.commandLevel();
        var kernel = CombatServerRuntime.existing(level.getServer());
        if (kernel.isEmpty() || world.commandSessionClosed()) return CommandResult.rejected(CommandRejection.Code.CLOSED_SESSION, "World session unavailable");
        var server = kernel.orElseThrow();
        if (!server.claim(operation, request)) return CommandResult.rejected(CommandRejection.Code.DUPLICATE, "Request already submitted");
        if (!level.getServer().isSameThread()) return CommandResult.rejected(CommandRejection.Code.WRONG_THREAD, "Owning server thread required");
        if (server.world(level).orElse(null) != world) return CommandResult.rejected(CommandRejection.Code.CLOSED_SESSION, "World session unavailable");
        if (!(level.getEntity(target) instanceof net.minecraft.world.entity.LivingEntity living) || !living.isAlive())
            return CommandResult.rejected(CommandRejection.Code.TARGET_UNAVAILABLE, "Live target required");
        CombatLineage lineage;
        try { lineage = server.requestLineage(operation, parent); }
        catch (IllegalStateException rejected) { return CommandResult.rejected(CommandRejection.Code.INVALID_PHASE, rejected.getMessage()); }
        var admission = server.admit(lineage);
        if (!admission.admitted()) return CommandResult.rejected(CommandRejection.Code.valueOf(admission.rejection().name()), "Admission limit");
        var previous = APPLYING.get();
        try (var completion = server.reserveCompletion(lineage)) {
            var ticket = server.ticket(lineage); ticket.bindSession(level, target);
            var captured = world.behaviors().capture(target);
            CommandResult<T> result;
            try (var scope = ticket.enter(CombatExecutionScope.Phase.APPLICATION)) {
                APPLYING.set(ticket);
                try { result = action.get(); }
                catch (RuntimeException failure) {
                    result = CommandResult.rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "State application incomplete; inspect owner receipt");
                }
            } finally { if (previous == null) APPLYING.remove(); else APPLYING.set(previous); }
            result = result.withContext(new CommandContext(request, target, operation, source,
                    Optional.of(new CommandContext.Identity(lineage.id(), lineage.rootId(), lineage.parentId(), lineage.depth()))));
            var delivered = result;
            completion.publish(() -> {
                var envelope=new io.github.naimjeg.combatnexus.combat.api.behavior.SettlementEnvelope(operation,
                    delivered.context().orElseThrow().identity().orElseThrow(),target,source,delivered.journal(),delivered);
                io.github.naimjeg.combatnexus.combat.spi.CombatBehaviorAccess.completed(world,lineage,envelope,captured);
                io.github.naimjeg.combatnexus.combat.internal.registration.CombatExtensions.stateCompleted(server.server(), lineage, delivered);
            });
            return result;
        } finally { server.finish(lineage); server.drain(); }
    }
}
