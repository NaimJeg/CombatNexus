package io.github.naimjeg.combatnexus.combat.internal.coordination;

import io.github.naimjeg.combatnexus.combat.api.CombatSource;
import io.github.naimjeg.combatnexus.combat.healing.api.*;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageRequestKind;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageSettlementStatus;
import io.github.naimjeg.combatnexus.formula.Amounts;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;

/** Request planning only; the settlement consumer owns once-only dispatch and the HealingService. */
public final class LifestealRequests {
    private LifestealRequests() {}
    public static io.github.naimjeg.combatnexus.combat.api.behavior.SettlementPolicy policy() {
        var id=Identifier.fromNamespaceAndPath("combatnexus","lifesteal");
        return new io.github.naimjeg.combatnexus.combat.api.behavior.SettlementPolicy(id,0,
            java.util.Set.of(io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.DAMAGE),
            java.util.Set.of(io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.HEALING),c->{
                var s=c.settlement().result(DamageSettlementSnapshot.class).orElseThrow();
                var attacker=s.logicalAttacker();
                var input=attacker==null?null:s.evidence().reactionInputs().participants().get(attacker.getUUID());
                double ratio=input==null?0:input.attributes().valueOrZero(io.github.naimjeg.combatnexus.attribute.api.CombatAttributes.lifesteal().unwrapKey().orElseThrow().identifier());
                var request=c.authority().get().isEmpty()?Optional.<HealingRequest>empty():fromPrimaryHealthLoss(s,id,ratio);
                if(request.isEmpty())return new io.github.naimjeg.combatnexus.combat.api.reaction.ReactionResult("ineligible_lifesteal",Optional.empty());
                var result=c.world().healing().submit(request.orElseThrow(),c.authority().get().orElseThrow());
                return new io.github.naimjeg.combatnexus.combat.api.reaction.ReactionResult("child_"+result.outcome().name().toLowerCase(java.util.Locale.ROOT),Optional.of("healing/"+result.request().requestId()));
            });
    }

    /** Called only from a registered damage callback; preserves the shared root and its remaining budget. */
    public static Optional<HealingSettlement> submitFrom(
            io.github.naimjeg.combatnexus.combat.damage.api.event.DamageSettlementCallback callback,
            Identifier source, double ratio, HealingService healing) {
        java.util.Objects.requireNonNull(callback, "callback");
        java.util.Objects.requireNonNull(healing, "healing");
        var parent = callback.combatAuthority();
        if (parent.isEmpty()) return Optional.empty();
        return fromPrimaryHealthLoss(callback.snapshot(), source, ratio)
                .map(request -> healing.submit(request, parent.get()));
    }

    /** Default policy excludes derived/reflected damage, absorption and custom shields. */
    public static Optional<HealingRequest> fromPrimaryHealthLoss(DamageSettlementSnapshot settlement,
                                                                Identifier source, double ratio) {
        java.util.Objects.requireNonNull(settlement, "settlement");
        java.util.Objects.requireNonNull(source, "source");
        Amounts.nonNegative(ratio, "ratio");
        if (ratio > 1) throw new IllegalArgumentException("Lifesteal ratio exceeds one");
        var attacker = settlement.logicalAttacker();
        if (settlement.status() != DamageSettlementStatus.APPLIED || settlement.cancelled()
                || settlement.requestKind() != DamageRequestKind.PRIMARY || settlement.parentDamageId().isPresent()
                || !settlement.triggerPolicy().procAllowed() || attacker == null || !attacker.isAlive()
                || attacker.level() != settlement.level()) return Optional.empty();
        // Observed health delta can include platform side effects; cap it to this applied amount and prior health.
        if (!settlement.evidence().applicationCompleted() || !settlement.evidence().healthWriteObserved()) return Optional.empty();
        double effectiveLoss = Math.min(settlement.evidence().healthLoss(),
                Math.min(settlement.appliedDamage(), settlement.healthBefore()));
        double amount = effectiveLoss * ratio;
        if (amount <= 0) return Optional.empty();
        CombatSource origin = new CombatSource(source, Optional.of(attacker.getUUID()), Optional.of(io.github.naimjeg.combatnexus.combat.api.CombatOperationRef.of(settlement.lineage().transaction())));
        return Optional.of(new HealingRequest(UUID.randomUUID(), attacker.getUUID(), origin,
                HealingRequest.Reason.LIFESTEAL, amount));
    }
}
