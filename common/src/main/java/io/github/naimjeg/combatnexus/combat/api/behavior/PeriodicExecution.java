package io.github.naimjeg.combatnexus.combat.api.behavior;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingSettlement;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageResult;
import net.minecraft.resources.Identifier;
/** Short-lived periodic command capability; implementations validate source liveness on every call. */
public interface PeriodicExecution {
    void environmentDamage(io.github.naimjeg.combatnexus.effect.api.EnvironmentDamage parameters);
    void effectCapability(Identifier capability);
    BehaviorSource source();
    io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame input();
    io.github.naimjeg.combatnexus.combat.api.CommandOutcome<?> execute(io.github.naimjeg.combatnexus.combat.api.scheduling.ScheduledIntent intent);
    HealingSettlement heal(double amount);
    DamageResult damage(double amount, Identifier damageType);
    DamageResult damage(double amount, Identifier damageType, float minimumHealth);
}
