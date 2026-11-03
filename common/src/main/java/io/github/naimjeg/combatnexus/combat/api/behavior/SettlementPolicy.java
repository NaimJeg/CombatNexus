package io.github.naimjeg.combatnexus.combat.api.behavior;
import java.util.*;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation;
import io.github.naimjeg.combatnexus.combat.api.reaction.ReactionResult;
/** A registered world policy, without a fabricated source installation. */
public record SettlementPolicy(Identifier id,int priority,Set<CombatOperation> subscriptions,Set<CombatOperation> permissions,Function<SettlementContext,ReactionResult> handler) {
    public SettlementPolicy { Objects.requireNonNull(id);subscriptions=Set.copyOf(subscriptions);permissions=Set.copyOf(permissions);Objects.requireNonNull(handler); }
}
