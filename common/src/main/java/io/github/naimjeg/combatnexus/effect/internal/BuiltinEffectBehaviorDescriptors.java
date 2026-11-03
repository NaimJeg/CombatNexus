package io.github.naimjeg.combatnexus.effect.internal;
import io.github.naimjeg.combatnexus.effect.api.*;
import io.github.naimjeg.combatnexus.combat.damage.api.display.DamageText;
import io.github.naimjeg.combatnexus.combat.api.behavior.*;
import io.github.naimjeg.combatnexus.combat.api.action.TriggerLimit;
import io.github.naimjeg.combatnexus.presentation.api.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.*;
final class BuiltinEffectBehaviorDescriptors {
    static void register() {
        EffectBehaviorDescriptors.register(Identifier.parse("combatnexus:scheduled_environment_damage"), ScheduledEnvironmentDamage.class, (scheduled, level) -> {
            var summary = new ArrayList<Component>();
            var details = new ArrayList<Component>();
            var damage = scheduled.parameters();
                    summary.add(text("periodic_base_damage", TooltipNumbers.number(damage.intervalTicks() / 20.0), DamageText.amount(damage.amount(), DamageText.type(damage.damageType()))));
                    details.add(text("independent_cooldown"));
                    if (damage.minimumHealth() > 0) details.add(text("minimum_health", TooltipNumbers.number(damage.minimumHealth())));
            return new TooltipBlock(Component.empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(Identifier.parse("combatnexus:environment_damage"), EnvironmentDamage.class, (damage, level) -> {
            var summary = new ArrayList<Component>();
            var details = new ArrayList<Component>();
            summary.add(damage.intervalTicks() > 0 ? text("periodic_damage", TooltipNumbers.number(damage.intervalTicks() / 20.0), DamageText.amount(damage.amount(), DamageText.type(damage.damageType())))
                        : text("environment_damage", DamageText.amount(damage.amount(), DamageText.type(damage.damageType()))));
                    if (damage.minimumHealth() > 0) details.add(text("minimum_health", TooltipNumbers.number(damage.minimumHealth())));
            return new TooltipBlock(Component.empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(Identifier.parse("combatnexus:lethal_protection"), BehaviorComponent.LethalProtection.class, (ignored, level) -> {
            var summary = new ArrayList<Component>();
            var details = new ArrayList<Component>();
            summary.add(text("lethal"));
            return new TooltipBlock(Component.empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(Identifier.parse("combatnexus:periodic_healing"), BehaviorComponent.PeriodicHealing.class, (healing, level) -> {
            var summary = new ArrayList<Component>();
            var details = new ArrayList<Component>();
            summary.add(text("periodic", TooltipNumbers.number(healing.intervalTicks() / 20.0), TooltipNumbers.number(healing.amount())));
                    if (healing.onlyWhenMissingHealth()) summary.add(text("missing_health"));
            return new TooltipBlock(Component.empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(Identifier.parse("combatnexus:periodic_damage"), BehaviorComponent.PeriodicDamage.class, (damage, level) -> {
            var summary = new ArrayList<Component>();
            var details = new ArrayList<Component>();
            summary.add(text("periodic_damage", TooltipNumbers.number(damage.intervalTicks() / 20.0), DamageText.amount(damage.amount(), DamageText.type(damage.damageType()))));
                    if (damage.minimumHealth() > 0) details.add(text("minimum_health", TooltipNumbers.number(damage.minimumHealth())));
            return new TooltipBlock(Component.empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(Identifier.parse("combatnexus:instant_healing"), BehaviorComponent.InstantHealing.class, (healing, level) -> {
            var summary = new ArrayList<Component>();
            var details = new ArrayList<Component>();
            summary.add(text("instant_healing", TooltipNumbers.number(healing.amount())));
            return new TooltipBlock(Component.empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(Identifier.parse("combatnexus:instant_damage"), BehaviorComponent.InstantDamage.class, (damage, level) -> {
            var summary = new ArrayList<Component>();
            var details = new ArrayList<Component>();
            summary.add(text("instant_damage", DamageText.amount(damage.amount(), DamageText.type(damage.damageType()))));
            return new TooltipBlock(Component.empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(Identifier.parse("combatnexus:effect_capability"), EffectCapabilityBehavior.class, (capability, level) -> {
            var summary = new ArrayList<Component>();
            var details = new ArrayList<Component>();
            summary.addAll(io.github.naimjeg.combatnexus.effect.internal.EffectCapabilities.describe(capability.capability(), level));
            return new TooltipBlock(Component.empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(Identifier.parse("combatnexus:healing_additive"), BehaviorComponent.HealingAdditive.class, (healing, level) -> {
            var summary = new ArrayList<Component>();
            var details = new ArrayList<Component>();
            summary.add(text("healing", TooltipNumbers.number(healing.amount())));
            return new TooltipBlock(Component.empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(Identifier.parse("combatnexus:reflection"), BehaviorComponent.Reflection.class, (reflection, level) -> {
            var summary = new ArrayList<Component>();
            var details = new ArrayList<Component>();
            summary.add(text("reflection", TooltipNumbers.percent(reflection.ratio())));
                    details.add(text("reflection.detail"));
                    details.add(limit(reflection.limit()));
            return new TooltipBlock(Component.empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(Identifier.parse("combatnexus:overflow_shield"), BehaviorComponent.OverflowShield.class, (shield, level) -> {
            var summary = new ArrayList<Component>();
            var details = new ArrayList<Component>();
            summary.add(text("overflow", TooltipNumbers.percent(shield.ratio()), TooltipNumbers.number(shield.capacity()), TooltipNumbers.number(shield.durationTicks() / 20.0)));
                    details.add(text("overflow.detail"));
                    details.add(limit(shield.limit()));
            return new TooltipBlock(Component.empty(), summary, details);
        });
    }
    private static Component limit(TriggerLimit limit) {
        return text("limit", text("scope." + limit.scope().name().toLowerCase(Locale.ROOT)), limit.maximum());
    }
    private static Component text(String key, Object... args) { return Component.translatable("tooltip.combatnexus.effect." + key, args); }
}
