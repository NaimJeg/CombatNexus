package io.github.naimjeg.combatnexus.combat.api.behavior;

import com.google.gson.JsonObject;
import io.github.naimjeg.combatnexus.combat.api.action.TriggerLimit;
import io.github.naimjeg.combatnexus.config.DefinitionJson;
import io.github.naimjeg.combatnexus.config.SettingsJson;
import java.util.Objects;

/** Typed, immutable contributions. Each executor owns its own parameters and trigger limit. */
public interface BehaviorComponent {
    default net.minecraft.resources.Identifier type() { return io.github.naimjeg.combatnexus.combat.internal.registration.BehaviorTypes.type(this).id(); }
    default java.util.Map<net.minecraft.resources.Identifier, net.minecraft.resources.Identifier> ownedShields(BehaviorSource source) { return java.util.Map.of(); }
    default void revoke(BehaviorSource source, BehaviorCleanup cleanup) { ownedShields(source).values().forEach(cleanup::removeShield); }
    record LethalProtection() implements ProtectionBehavior { public java.util.OptionalDouble protect(ProtectionContext c) { return c.before() > 0 && c.proposed() <= 0 ? java.util.OptionalDouble.of(Math.min(c.before(), 1.0f)) : java.util.OptionalDouble.empty(); } }
    record Reflection(double ratio, TriggerLimit limit) implements SettlementBehavior {
        public java.util.Set<io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation> permissions() { return java.util.Set.of(io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.DAMAGE); }
        public java.util.Set<io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation> subscriptions() { return java.util.Set.of(io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.DAMAGE); }
        public io.github.naimjeg.combatnexus.combat.api.reaction.ReactionResult settle(SettlementContext context) { return io.github.naimjeg.combatnexus.combat.internal.coordination.BuiltinBehaviorReactions.reflect(this, context); }
        public Reflection { DefinitionJson.range(ratio, 0, 1, "ratio"); Objects.requireNonNull(limit); }
    }
    record OverflowShield(double ratio, double capacity, int durationTicks, TriggerLimit limit) implements SettlementBehavior {
        public java.util.Set<io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation> permissions() { return java.util.Set.of(io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.SHIELD); }
        public java.util.Set<io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation> subscriptions() { return java.util.Set.of(io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.HEALING); }
        public io.github.naimjeg.combatnexus.combat.api.reaction.ReactionResult settle(SettlementContext context) { return io.github.naimjeg.combatnexus.combat.internal.coordination.BuiltinBehaviorReactions.overflow(this, context); }
        public java.util.Map<net.minecraft.resources.Identifier, net.minecraft.resources.Identifier> ownedShields(BehaviorSource source) { return java.util.Map.of(type(), io.github.naimjeg.combatnexus.combat.internal.coordination.BuiltinBehaviorReactions.overflowPool(source)); }
        public OverflowShield { DefinitionJson.range(ratio, 0, 1, "ratio"); DefinitionJson.range(capacity, 0, 1000000, "capacity");
            DefinitionJson.range(durationTicks, 1, 1200000, "duration_ticks"); Objects.requireNonNull(limit); }
    }
    record PeriodicHealing(double amount, int intervalTicks, int firstDelayTicks, boolean onlyWhenMissingHealth, PeriodicPhase phase) implements PeriodicBehavior {
        public PeriodicHealing(double amount, int intervalTicks, int firstDelayTicks, boolean onlyWhenMissingHealth) {
            this(amount, intervalTicks, firstDelayTicks, onlyWhenMissingHealth, PeriodicPhase.RESTART);
        }
        public PeriodicHealing(double amount, int intervalTicks) { this(amount, intervalTicks, -1, false); }
        public int initialDelayTicks() { return firstDelayTicks < 0 ? intervalTicks : firstDelayTicks; }
        public void execute(PeriodicExecution execution) {
            if (amount <= 0) return;
            if (onlyWhenMissingHealth) {
                var facts = execution.input().facts();
                if (facts.health() >= facts.maxHealth()) return;
            }
            execution.heal(amount);
        }
        public PeriodicHealing {
            Objects.requireNonNull(phase);
            DefinitionJson.range(amount, 0, 1000000, "amount"); DefinitionJson.range(intervalTicks, 1, 1200000, "interval_ticks");
            DefinitionJson.range(firstDelayTicks, -1, 1200000, "first_delay_ticks");
        }
    }
    record InstantHealing(double amount) implements InstantBehavior {
        public void execute(InstantExecution execution) {
            var s = execution.source();
            execution.submit(new io.github.naimjeg.combatnexus.combat.api.DomainCommand.Heal(
                new io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest(java.util.UUID.randomUUID(), s.target(),
                    new io.github.naimjeg.combatnexus.combat.api.CombatSource(s.source(), s.owner(), java.util.Optional.empty()),
                    io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest.Reason.EFFECT, amount)));
        }
        public InstantHealing { DefinitionJson.range(amount, 0, 1000000, "amount"); }
    }
    record InstantDamage(double amount, net.minecraft.resources.Identifier damageType, java.util.Optional<java.util.UUID> direct) implements InstantBehavior {
        public InstantDamage { DefinitionJson.range(amount, 0, 1000000, "amount"); Objects.requireNonNull(damageType); Objects.requireNonNull(direct); }
        public void execute(InstantExecution execution) { execution.damage(amount, damageType, direct); }
    }

    record PeriodicDamage(double amount, net.minecraft.resources.Identifier damageType, int intervalTicks, float minimumHealth, int firstDelayTicks, PeriodicPhase phase) implements PeriodicBehavior {
        public PeriodicDamage(double amount, net.minecraft.resources.Identifier damageType, int intervalTicks, float minimumHealth, int firstDelayTicks) {
            this(amount, damageType, intervalTicks, minimumHealth, firstDelayTicks, PeriodicPhase.RESTART);
        }
        public PeriodicDamage(double amount, net.minecraft.resources.Identifier damageType, int intervalTicks) { this(amount, damageType, intervalTicks, 0, -1); }
        public PeriodicDamage(double amount, net.minecraft.resources.Identifier damageType, int intervalTicks, float minimumHealth) { this(amount, damageType, intervalTicks, minimumHealth, -1); }
        public int initialDelayTicks() { return firstDelayTicks < 0 ? intervalTicks : firstDelayTicks; }
        public void execute(PeriodicExecution execution) {
            if (amount > 0 && (minimumHealth == 0 || execution.input().facts().health() > minimumHealth))
                execution.damage(amount, damageType, minimumHealth);
        }
        public PeriodicDamage {
            Objects.requireNonNull(phase);
            Objects.requireNonNull(damageType); DefinitionJson.range(amount, 0, 1000000, "amount");
            DefinitionJson.range(intervalTicks, 1, 1200000, "interval_ticks");
            DefinitionJson.range(firstDelayTicks, -1, 1200000, "first_delay_ticks");
            if (!Float.isFinite(minimumHealth) || minimumHealth < 0) throw new IllegalArgumentException("Invalid minimum health");
        }
    }
    record HealingAdditive(double amount) implements NumericalBehavior {
        public void healing(io.github.naimjeg.combatnexus.combat.healing.api.HealingPhase phase, io.github.naimjeg.combatnexus.combat.healing.api.HealingCalculation calculation, BehaviorSource source) {
            if (phase == io.github.naimjeg.combatnexus.combat.healing.api.HealingPhase.CONTRIBUTIONS && source.target().equals(calculation.request().target())) calculation.additive(source.source(), amount);
        }
        public HealingAdditive { DefinitionJson.range(amount, -1000000, 1000000, "amount"); }
    }
    static BehaviorComponent decode(JsonObject json) { return io.github.naimjeg.combatnexus.combat.internal.registration.BehaviorTypes.decode(json); }
}
