package io.github.naimjeg.combatnexus.combat.internal.coordination;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.behavior.*;
import io.github.naimjeg.combatnexus.combat.api.reaction.ReactionResult;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.*;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingSettlement;
import io.github.naimjeg.combatnexus.combat.shield.api.*;
import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/** Gameplay-specific executors. Generic dispatch and persistence have no knowledge of these names. */
public final class BuiltinBehaviorReactions {
    private BuiltinBehaviorReactions() {}
    private static ReactionResult skipped(String reason) { return new ReactionResult(reason, Optional.empty()); }
    public static ReactionResult reflect(BehaviorComponent.Reflection definition, SettlementContext c) {
        var s = c.settlement().result(DamageSettlementSnapshot.class).orElseThrow();
        if (definition.ratio() <= 0 || s.requestKind() != DamageRequestKind.PRIMARY || s.status() != DamageSettlementStatus.APPLIED
                || s.cancelled() || !s.evidence().applicationCompleted() || !s.evidence().healthWriteObserved() || s.evidence().healthLoss() <= 0
                || !s.triggerPolicy().reflectionAllowed() || c.damageAuthority().get().isEmpty()
                || !(s.directEntity() instanceof LivingEntity attacker) || !attacker.isAlive() || attacker.level() != s.level() || attacker == s.target())
            return skipped("ineligible_reflection");
        var limited = c.claim().apply(definition.limit()); if (limited.isPresent()) return skipped(limited.get());
        float amount = (float)(Math.min(s.evidence().healthLoss(), Math.min(s.appliedDamage(), s.healthBefore())) * definition.ratio());
        var request = DamageRequest.builder(s.level(), attacker, DamageSourceDescriptor.of(net.minecraft.world.damagesource.DamageTypes.THORNS), amount)
            .kind(DamageRequestKind.REFLECTED).parent(c.damageAuthority().get().orElseThrow()).logicalAttacker(s.target()).directEntity(s.target())
            .triggerPolicy(DamageTriggerPolicy.NONE_ALLOWED).build();
        var result = c.world().damage(request);
        return new ReactionResult("child_" + result.status().name().toLowerCase(Locale.ROOT), Optional.of("damage/" + request.lineage().damageId()));
    }
    public static Identifier overflowPool(BehaviorSource source) { return Identifier.fromNamespaceAndPath("combatnexus", "overflow/" + source.installation()); }
    public static ReactionResult overflow(BehaviorComponent.OverflowShield definition, SettlementContext c) {
        var s = c.settlement().result(HealingSettlement.class).orElseThrow(); var source = c.source().orElseThrow();
        if (definition.ratio() <= 0 || s.overflow() <= 0 || !s.journal().applicationCompleted()
                || s.outcome() != HealingSettlement.Outcome.APPLIED && s.outcome() != HealingSettlement.Outcome.FULL_HEALTH || c.authority().get().isEmpty())
            return skipped("ineligible_overflow");
        var limited = c.claim().apply(definition.limit()); if (limited.isPresent()) return skipped(limited.get());
        var pool = overflowPool(source);
        double old = c.world().shields().snapshot(source.target()).stream().filter(p -> p.source().id().equals(pool)).mapToDouble(ShieldPoolSnapshot::balance).findFirst().orElse(0);
        double balance = Math.min(definition.capacity(), old + s.overflow() * definition.ratio());
        if (balance <= old) return skipped("shield_capacity");
        long now = c.world().facts(source.target()).orElseThrow().gameTick();
        var receipt = c.world().shields().grant(UUID.randomUUID(), source.target(), new ShieldGrant(
            new CombatSource(pool, source.owner(), Optional.of(new CombatOperationRef(c.settlement().identity().operation(), io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.HEALING))),
            new ShieldDefinition(definition.capacity(), source.plan().priority(), Set.of(), Set.of(), 0, 0, ShieldDefinition.Stacking.REPLACE_SAME_SOURCE), balance,
            OptionalLong.of(Math.addExact(now, definition.durationTicks()))), c.authority().get().orElseThrow());
        return new ReactionResult(receipt.applied() ? "executed" : "rejected", receipt.value().map(v -> "shield/" + v.operation()));
    }
}
