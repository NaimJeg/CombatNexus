package io.github.naimjeg.combatnexus.combat.api.behavior;
import io.github.naimjeg.combatnexus.combat.healing.api.*;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
/** Computation only. Each domain retains its typed workspace and phase order. */
public interface NumericalBehavior extends BehaviorComponent {
    default void healing(HealingPhase phase, HealingCalculation calculation, BehaviorSource source) {}
    default void damage(DamagePhase phase, DamageRuleContext calculation, BehaviorSource source) {}
}
