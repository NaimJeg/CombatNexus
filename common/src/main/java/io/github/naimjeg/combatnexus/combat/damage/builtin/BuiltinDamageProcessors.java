package io.github.naimjeg.combatnexus.combat.damage.builtin;

import io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.builtin.bridge.*;
import io.github.naimjeg.combatnexus.combat.damage.builtin.processor.*;
import io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleIds;
import net.minecraft.resources.Identifier;
import java.util.List;

/** Shared processor instances and stable registration order. */
public final class BuiltinDamageProcessors {
    private BuiltinDamageProcessors() {}
    public static final RuleExecutionProcessor RULE_BASE = new RuleExecutionProcessor(DamagePhase.BASE_MODIFICATION);
    public static final RuleExecutionProcessor RULE_TYPE_SCALING = new RuleExecutionProcessor(DamagePhase.TYPE_SCALING);
    public static final RuleExecutionProcessor RULE_MITIGATION = new RuleExecutionProcessor(DamagePhase.MITIGATION_SETUP);
    public static final RuleExecutionProcessor RULE_CRITICAL = new RuleExecutionProcessor(DamagePhase.CRITICAL_HIT);
    public static final RuleExecutionProcessor RULE_CONDITIONAL = new RuleExecutionProcessor(DamagePhase.CONDITIONAL_MULTI);
    public static final RuleExecutionProcessor RULE_GLOBAL = new RuleExecutionProcessor(DamagePhase.GLOBAL_ADJUSTMENT);
    public static final RuleExecutionProcessor RULE_FINAL_OVERRIDE = new RuleExecutionProcessor(DamagePhase.FINAL_OVERRIDE);
    public static final VanillaArmorEffectivenessProcessor VANILLA_ARMOR_EFFECTIVENESS = new VanillaArmorEffectivenessProcessor();
    public static final VanillaInitialBaseDamageProcessor VANILLA_INITIAL_BASE_DAMAGE = new VanillaInitialBaseDamageProcessor();
    public static final VanillaThornsAttributeBridgeProcessor VANILLA_THORNS_ATTRIBUTE = new VanillaThornsAttributeBridgeProcessor();
    public static final VanillaOffensiveEnchantmentProcessor VANILLA_OFFENSIVE_ENCHANTMENT = new VanillaOffensiveEnchantmentProcessor();
    public static final VanillaOffensiveMobEffectProcessor VANILLA_OFFENSIVE_MOB_EFFECT = new VanillaOffensiveMobEffectProcessor();
    public static final VanillaDamageProtectionProcessor VANILLA_DAMAGE_PROTECTION = new VanillaDamageProtectionProcessor();
    public static final VanillaResistanceEffectProcessor VANILLA_RESISTANCE_EFFECT = new VanillaResistanceEffectProcessor();
    public static final ArmorMitigationProcessor ARMOR = new ArmorMitigationProcessor();
    public static final ResistanceMitigationProcessor RESISTANCE = new ResistanceMitigationProcessor();
    public static final VanillaDifficultyScalingProcessor VANILLA_DIFFICULTY_SCALING = new VanillaDifficultyScalingProcessor();
    public static final VanillaSpecialAttackScalingProcessor VANILLA_SPECIAL_ATTACK_SCALING = new VanillaSpecialAttackScalingProcessor();
    public static final VanillaSpearBonusProcessor VANILLA_SPEAR_BONUS = new VanillaSpearBonusProcessor();
    public static final VanillaPlayerAttackScalingProcessor VANILLA_PLAYER_ATTACK_SCALING = new VanillaPlayerAttackScalingProcessor();
    public static final VanillaProjectileScalingProcessor VANILLA_PROJECTILE_SCALING = new VanillaProjectileScalingProcessor();
    public static List<DamagePhaseProcessor> builtInProcessors() {
        return List.of(
                RULE_BASE,
                RULE_TYPE_SCALING,
                RULE_MITIGATION,
                RULE_CRITICAL,
                RULE_CONDITIONAL,
                RULE_GLOBAL,
                RULE_FINAL_OVERRIDE,
                VANILLA_ARMOR_EFFECTIVENESS,
                VANILLA_INITIAL_BASE_DAMAGE,
                VANILLA_THORNS_ATTRIBUTE,
                VANILLA_OFFENSIVE_ENCHANTMENT,
                VANILLA_OFFENSIVE_MOB_EFFECT,
                VANILLA_DAMAGE_PROTECTION,
                VANILLA_RESISTANCE_EFFECT,
                ARMOR,
                RESISTANCE,
                VANILLA_DIFFICULTY_SCALING,
                VANILLA_SPECIAL_ATTACK_SCALING,
                VANILLA_SPEAR_BONUS,
                VANILLA_PLAYER_ATTACK_SCALING,
                VANILLA_PROJECTILE_SCALING
        );
    }

    public static Identifier builtInProcessorId(DamagePhaseProcessor processor) {
        if (processor == RULE_BASE) return DamageModuleIds.id("rule_base");
        if (processor == RULE_TYPE_SCALING) return DamageModuleIds.id("rule_type_scaling");
        if (processor == RULE_MITIGATION) return DamageModuleIds.id("rule_mitigation");
        if (processor == RULE_CRITICAL) return DamageModuleIds.id("rule_critical");
        if (processor == RULE_CONDITIONAL) return DamageModuleIds.id("rule_conditional");
        if (processor == RULE_GLOBAL) return DamageModuleIds.id("rule_global");
        if (processor == RULE_FINAL_OVERRIDE) return DamageModuleIds.id("rule_final_override");
        if (processor == VANILLA_ARMOR_EFFECTIVENESS) return DamageModuleIds.id("vanilla_armor_effectiveness");
        if (processor == VANILLA_INITIAL_BASE_DAMAGE) return DamageModuleIds.id("vanilla_initial_base_damage");
        if (processor == VANILLA_THORNS_ATTRIBUTE) return DamageModuleIds.id("vanilla_thorns_attribute");
        if (processor == VANILLA_OFFENSIVE_ENCHANTMENT) return DamageModuleIds.id("vanilla_offensive_enchantment");
        if (processor == VANILLA_OFFENSIVE_MOB_EFFECT) return DamageModuleIds.id("vanilla_offensive_mob_effect");
        if (processor == VANILLA_DAMAGE_PROTECTION) return DamageModuleIds.id("vanilla_damage_protection");
        if (processor == VANILLA_RESISTANCE_EFFECT) return DamageModuleIds.id("vanilla_resistance_effect");
        if (processor == ARMOR) return DamageModuleIds.id("armor");
        if (processor == RESISTANCE) return DamageModuleIds.id("resistance");
        if (processor == VANILLA_DIFFICULTY_SCALING) return DamageModuleIds.id("vanilla_difficulty_scaling");
        if (processor == VANILLA_SPECIAL_ATTACK_SCALING) return DamageModuleIds.id("vanilla_special_attack_scaling");
        if (processor == VANILLA_SPEAR_BONUS) return DamageModuleIds.id("vanilla_spear_bonus");
        if (processor == VANILLA_PLAYER_ATTACK_SCALING) return DamageModuleIds.id("vanilla_player_attack_scaling");
        if (processor == VANILLA_PROJECTILE_SCALING) return DamageModuleIds.id("vanilla_projectile_scaling");
        return null;
    }
}
