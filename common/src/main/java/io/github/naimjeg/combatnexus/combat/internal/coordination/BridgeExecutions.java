package io.github.naimjeg.combatnexus.combat.internal.coordination;

import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.bridge.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
import io.github.naimjeg.combatnexus.combat.internal.transaction.*;
import net.minecraft.resources.Identifier;

/** Invocation evidence and restrictive scopes only. Admission/lifecycle remain in the existing kernel. */
public final class BridgeExecutions implements BridgeCalls {
    private static final ThreadLocal<ArrayDeque<Call>> ACTIVE=ThreadLocal.withInitial(ArrayDeque::new);
    private final CombatRuntime world;
    private final ArrayDeque<BridgeInvocation> history=new ArrayDeque<>();
    public BridgeExecutions(CombatRuntime world) { this.world=world; }
    public List<BridgeInvocation> history() { world.level(); return List.copyOf(history); }
    public BridgeCall open(UUID invocation, UUID action, UUID target, Identifier capability, Identifier position, Identifier executor) {
        world.level(); CombatExecutionScope.requireSubmissionPhase();
        var declaration=CombatAccess.bridgeCoverage().stream().filter(c->c.id().equals(capability)&&c.position().equals(position)&&c.executor().equals(executor)).findFirst().orElseThrow(()->new IllegalArgumentException("Unregistered bridge owner/position"));
        var kernel=CombatServerRuntime.existing(world.level().getServer()).orElseThrow();
        if(!kernel.claim(CombatOperation.STATE,invocation)) throw new IllegalStateException("Bridge invocation already consumed");
        var call=new Call(invocation,action,target,declaration); ACTIVE.get().push(call); return call;
    }
    public static void require(CombatOperation operation) {
        for(var call:ACTIVE.get()) if(!call.declaration.permissions().contains(operation)) throw new IllegalStateException("Operation restricted by bridge: "+call.declaration.id());
    }
    public static Set<CombatOperation> allowedOperations(){var allowed=EnumSet.allOf(CombatOperation.class);for(var call:ACTIVE.get())allowed.retainAll(call.declaration.permissions());return allowed;}
    public static void record(CommandResult<?> result) { for(var call:ACTIVE.get()) call.commands.add(new BridgeInvocation.CommandEvidence(result.context(),result.journal(),result.rejection())); }
    public static void record(DamageFormulaResult result) { for(var call:ACTIVE.get()) call.decisions.add(new BridgeInvocation.DecisionEvidence(result.request().invocation(),result.request().policy(),result.request().amount(),result.output().amount(),result.samples(),result.output().multipliers(),result.definitionGeneration(),result.completed())); }
    public static DamageFormulaResult decision(DamageFormulaResult result) { record(result); return result; }
    public static void evidence(BridgeInvocation.CommandEvidence evidence) { for(var call:ACTIVE.get()) call.commands.add(evidence); }
    private final class Call implements BridgeCall {
        private final UUID invocation,action,target;
        private final BridgeCapability declaration;
        private final List<BridgeInvocation.CommandEvidence> commands=new ArrayList<>();
        private final List<BridgeInvocation.DecisionEvidence> decisions=new ArrayList<>();
        private BridgeInvocation.Contribution nativeContribution=BridgeInvocation.Contribution.PENDING;
        private boolean closed;
        private boolean returnedNormally;
        public void returnedNormally() { active(); returnedNormally=true; }
        Call(UUID invocation,UUID action,UUID target,BridgeCapability declaration) {
            this.invocation=Objects.requireNonNull(invocation);this.action=Objects.requireNonNull(action);this.target=Objects.requireNonNull(target);this.declaration=declaration;
        }
        private void active() { world.level(); if(closed || ACTIVE.get().peek()!=this) throw new IllegalStateException("Bridge scope expired or out of order"); }
        public void nativeContribution(BridgeInvocation.Contribution contribution) {
            active(); Objects.requireNonNull(contribution);
            if(nativeContribution!=BridgeInvocation.Contribution.PENDING || contribution==BridgeInvocation.Contribution.PENDING) throw new IllegalStateException("Native contribution already resolved");
            nativeContribution=contribution;
        }
        public BridgeInvocation snapshot() { world.level(); return new BridgeInvocation(invocation,action,target,declaration.id(),declaration.executor(),nativeContribution,commands,decisions,closed,returnedNormally); }
        public void close() { active(); ACTIVE.get().pop(); if(ACTIVE.get().isEmpty()) ACTIVE.remove(); closed=true;history.addLast(snapshot());while(history.size()>256)history.removeFirst(); }
    }
}
