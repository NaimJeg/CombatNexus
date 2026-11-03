package io.github.naimjeg.combatnexus.combat.internal.coordination;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.behavior.*;
import io.github.naimjeg.combatnexus.combat.api.reaction.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.*;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.*;
import io.github.naimjeg.combatnexus.combat.damage.api.event.DamageSettlementCallback;
import io.github.naimjeg.combatnexus.combat.healing.api.*;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Domain policies over frozen facts. Delivery and short-lived authority remain owned by the kernel. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class CombatReactions implements CombatReactionService {
    private static Identifier id(String value) { return Identifier.fromNamespaceAndPath("combatnexus", value); }
    private final CombatRuntime world;
    private final io.github.naimjeg.combatnexus.combat.api.action.TriggerLedger triggers = new io.github.naimjeg.combatnexus.combat.api.action.TriggerLedger(4096);
    private final ArrayDeque<ReactionFact> facts = new ArrayDeque<>();
    public CombatReactions(CombatRuntime world) { this.world = world; }
    private final Map<UUID, ArrayDeque<ReactionFact>> targetFacts = new HashMap<>();
    private void record(ReactionFact fact) {
        if (facts.size() == 256) {
            var old = facts.removeFirst(); var group = targetFacts.get(old.target()); group.removeFirst();
            if (group.isEmpty()) targetFacts.remove(old.target());
        }
        facts.addLast(fact); targetFacts.computeIfAbsent(fact.target(), ignored -> new ArrayDeque<>()).addLast(fact);
    }
    public List<ReactionFact> facts(UUID target, java.util.function.IntConsumer budget) {
        world.level(); var result = new ArrayList<ReactionFact>(); var group = targetFacts.get(target);
        if (group == null) return List.of();
        int skip = Math.max(0, group.size() - 16);
        for (var fact : group) { if (skip-- > 0) continue; budget.accept(1); result.add(fact); }
        return List.copyOf(result);
    }
    public List<ReactionFact> facts() { world.level(); return List.copyOf(facts); }
    public CombatReactionService readOnly() { return this::facts; }
    private void lifecycleWrite() { world.level(); CombatMutationGuard.requireWorldWritePhase(); }
    public void clear() {
        lifecycleWrite();
        if (io.github.naimjeg.combatnexus.combat.internal.transaction.CombatDispatchScope.isActive())
            throw new IllegalStateException("Cannot reset reactions during settlement dispatch");
        facts.clear(); targetFacts.clear(); triggers.clear(); delivered.clear();
    }
    io.github.naimjeg.combatnexus.combat.api.action.TriggerLedger.SourceCheckpoint checkpoint(long installation) { lifecycleWrite(); return triggers.checkpoint(installation); }
    void restore(io.github.naimjeg.combatnexus.combat.api.action.TriggerLedger.SourceCheckpoint checkpoint) { lifecycleWrite(); triggers.restore(checkpoint); }
    Map<String, Integer> persistentCounts(long source) { lifecycleWrite(); return triggers.persistentCounts(source); }
    void restoreCounts(long source, Map<String, Integer> counts) { lifecycleWrite(); triggers.restoreCounts(source, counts); }
    void removeSource(long installation) { lifecycleWrite(); triggers.remove(installation); }
    private Optional<String> claim(BehaviorSource source, String reaction, io.github.naimjeg.combatnexus.combat.api.action.TriggerLimit limit, long operation, long root, UUID target,
            Optional<io.github.naimjeg.combatnexus.combat.api.action.CombatAction> action) {
        if (!io.github.naimjeg.combatnexus.combat.internal.transaction.CombatDispatchScope.isActive())
            throw new IllegalStateException("Trigger consumption requires settlement dispatch");
        if (!world.behaviors().valid(source)) return Optional.of("source_removed");
        return triggers.claim(world.level().getGameTime(), source.installation(), reaction, limit, operation, root, target, action);
    }
    private static CommandContext.Identity identity(CombatLineage l) { return new CommandContext.Identity(l.id(),l.rootId(),l.parentId(),l.depth()); }
    public void damage(DamageSettlementCallback callback) {
        if(!io.github.naimjeg.combatnexus.combat.internal.transaction.CombatDispatchScope.delivering(callback,world.level().getServer()))throw new IllegalStateException("Expired damage callback");
        var s=callback.snapshot();
        if(!claimDelivery(s.lineage().transaction(),CombatOperation.DAMAGE))return;
        io.github.naimjeg.combatnexus.combat.spi.CombatBehaviorAccess.recordRecoveryDamage(world,callback);
        if(s.successfulHit())
            record(new ReactionFact(s.damageId(),s.target().getUUID(),id("recovery_delay"),Optional.empty(),"executed",Optional.empty()));
        dispatch(s.lineage().transaction(),new SettlementEnvelope(CombatOperation.DAMAGE,identity(s.lineage().transaction()),s.target().getUUID(),Optional.empty(),s.evidence().journal(),s),
            s.evidence().behaviors(),s.evidence().action(),callback::combatAuthority,callback::childAuthority);
    }
    public void healing(CombatCallback<HealingSettlement> callback) {
        if(!io.github.naimjeg.combatnexus.combat.internal.transaction.CombatDispatchScope.delivering(callback,world.level().getServer()))throw new IllegalStateException("Expired healing callback");
        var s=callback.result();
        if(!claimDelivery(callback.lineage(),CombatOperation.HEALING))return;
        dispatch(callback.lineage(),new SettlementEnvelope(CombatOperation.HEALING,identity(callback.lineage()),s.request().target(),Optional.of(s.request().source()),s.journal(),s),
            s.behaviors(),Optional.empty(),callback::childAuthority,Optional::empty);
    }
    public void completed(CombatCallback<SettlementEnvelope> callback,List<BehaviorSource> captured) {
        if(!io.github.naimjeg.combatnexus.combat.internal.transaction.CombatDispatchScope.delivering(callback,world.level().getServer()))throw new IllegalStateException("Expired settlement callback");
        if(!claimDelivery(callback.lineage(),callback.result().operation()))return;
        dispatch(callback.lineage(),callback.result(),captured,Optional.empty(),callback::childAuthority,Optional::empty);
    }
    private final Map<CombatLineage,Set<CombatOperation>> delivered = new WeakHashMap<>();
    private boolean claimDelivery(CombatLineage lineage,CombatOperation operation) {
        return delivered.computeIfAbsent(lineage,ignored->EnumSet.noneOf(CombatOperation.class)).add(operation);
    }
    private void dispatch(CombatLineage lineage,SettlementEnvelope envelope,List<BehaviorSource> captured,
            Optional<io.github.naimjeg.combatnexus.combat.api.action.CombatAction> action,
            java.util.function.Supplier<Optional<CombatParent>> authority,
            java.util.function.Supplier<Optional<DamageParentRef>> damageAuthority) {
        var services=new io.github.naimjeg.combatnexus.combat.internal.WorldServices(world);
        for(var policy:io.github.naimjeg.combatnexus.combat.internal.registration.CombatExtensions.settlementPolicies())
            if(policy.subscriptions().contains(envelope.operation())) {
                var context=new SettlementContext(services,Optional.empty(),envelope,limit->Optional.of("source_required"),authority,damageAuthority);
                invoke(policy.id(),policy.permissions(),Optional.empty(),envelope,()->policy.handler().apply(context));
            }
        for(var source:captured) if(world.behaviors().valid(source))
            for(var component:source.plan().components()) if(component instanceof SettlementBehavior behavior && behavior.subscriptions().contains(envelope.operation())) {
                var context=new SettlementContext(services,Optional.of(source),envelope,
                    limit->claim(source,component.type().toString(),limit,envelope.identity().operation(),envelope.identity().root(),source.target(),action),authority,damageAuthority);
                invoke(component.type(),behavior.permissions(),Optional.of(source.source()),envelope,()->behavior.settle(context));
            }
    }
    private void invoke(Identifier id,Set<CombatOperation> permissions,Optional<Identifier> source,SettlementEnvelope envelope,java.util.function.Supplier<ReactionResult> handler) {
        try(var restriction=io.github.naimjeg.combatnexus.combat.internal.transaction.CombatDispatchScope.restrict(permissions)) {
            var result=handler.get();record(new ReactionFact(envelope.identity().operation(),envelope.target(),id,source,result.outcome(),result.childRequest()));
        } catch(Throwable failure) {
            CombatFailures.rethrowFatal(failure);
        }
    }
}
