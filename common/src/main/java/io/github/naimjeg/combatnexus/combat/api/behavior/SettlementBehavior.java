package io.github.naimjeg.combatnexus.combat.api.behavior;
import java.util.Set;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation;
import io.github.naimjeg.combatnexus.combat.api.reaction.ReactionResult;
/** Source-owned reactions subscribe to domain settlements and declare child operations. */
public interface SettlementBehavior extends BehaviorComponent {
    Set<CombatOperation> subscriptions();
    Set<CombatOperation> permissions();
    ReactionResult settle(SettlementContext context);
}
