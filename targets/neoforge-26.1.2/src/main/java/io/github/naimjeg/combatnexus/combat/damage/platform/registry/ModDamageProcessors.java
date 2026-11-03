package io.github.naimjeg.combatnexus.combat.damage.platform.registry;

import io.github.naimjeg.combatnexus.combat.damage.builtin.BuiltinDamageProcessors;

import io.github.naimjeg.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.builtin.bridge.*;
import io.github.naimjeg.combatnexus.combat.damage.builtin.processor.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModDamageProcessors {

    private static final ResourceKey<Registry<DamagePhaseProcessor>> PROCESSORS_KEY =
            ResourceKey.createRegistryKey(
                    DamageModuleIds.id("damage_phase_processors")
            );

    private static final DeferredRegister<DamagePhaseProcessor> PROCESSORS =
            DeferredRegister.create(PROCESSORS_KEY, DamageModule.MODID);

    private static final Registry<DamagePhaseProcessor> PROCESSOR_REGISTRY =
            PROCESSORS.makeRegistry(builder -> builder.sync(false));

    public static final DeferredHolder<DamagePhaseProcessor, RuleExecutionProcessor> RULE_BASE =
            PROCESSORS.register(
                    "rule_base",
                    () -> BuiltinDamageProcessors.RULE_BASE
            );

    public static final DeferredHolder<DamagePhaseProcessor, RuleExecutionProcessor> RULE_TYPE_SCALING =
            PROCESSORS.register(
                    "rule_type_scaling",
                    () -> BuiltinDamageProcessors.RULE_TYPE_SCALING
            );

    public static final DeferredHolder<DamagePhaseProcessor, RuleExecutionProcessor> RULE_MITIGATION =
            PROCESSORS.register(
                    "rule_mitigation",
                    () -> BuiltinDamageProcessors.RULE_MITIGATION
            );

    public static final DeferredHolder<DamagePhaseProcessor, RuleExecutionProcessor> RULE_CRITICAL =
            PROCESSORS.register(
                    "rule_critical",
                    () -> BuiltinDamageProcessors.RULE_CRITICAL
            );

    public static final DeferredHolder<DamagePhaseProcessor, RuleExecutionProcessor> RULE_CONDITIONAL =
            PROCESSORS.register(
                    "rule_conditional",
                    () -> BuiltinDamageProcessors.RULE_CONDITIONAL
            );

    public static final DeferredHolder<DamagePhaseProcessor, RuleExecutionProcessor> RULE_GLOBAL =
            PROCESSORS.register(
                    "rule_global",
                    () -> BuiltinDamageProcessors.RULE_GLOBAL
            );

    public static final DeferredHolder<DamagePhaseProcessor, RuleExecutionProcessor> RULE_FINAL_OVERRIDE =
            PROCESSORS.register(
                    "rule_final_override",
                    () -> BuiltinDamageProcessors.RULE_FINAL_OVERRIDE
            );

    public static final DeferredHolder<DamagePhaseProcessor, VanillaArmorEffectivenessProcessor> VANILLA_ARMOR_EFFECTIVENESS =
            PROCESSORS.register("vanilla_armor_effectiveness", () -> BuiltinDamageProcessors.VANILLA_ARMOR_EFFECTIVENESS);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaInitialBaseDamageProcessor> VANILLA_INITIAL_BASE_DAMAGE =
            PROCESSORS.register("vanilla_initial_base_damage", () -> BuiltinDamageProcessors.VANILLA_INITIAL_BASE_DAMAGE);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaThornsAttributeBridgeProcessor> VANILLA_THORNS_ATTRIBUTE =
            PROCESSORS.register("vanilla_thorns_attribute", () -> BuiltinDamageProcessors.VANILLA_THORNS_ATTRIBUTE);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaOffensiveEnchantmentProcessor> VANILLA_OFFENSIVE_ENCHANTMENT =
            PROCESSORS.register("vanilla_offensive_enchantment", () -> BuiltinDamageProcessors.VANILLA_OFFENSIVE_ENCHANTMENT);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaOffensiveMobEffectProcessor> VANILLA_OFFENSIVE_MOB_EFFECT =
            PROCESSORS.register("vanilla_offensive_mob_effect", () -> BuiltinDamageProcessors.VANILLA_OFFENSIVE_MOB_EFFECT);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaDamageProtectionProcessor> VANILLA_DAMAGE_PROTECTION =
            PROCESSORS.register("vanilla_damage_protection", () -> BuiltinDamageProcessors.VANILLA_DAMAGE_PROTECTION);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaResistanceEffectProcessor> VANILLA_RESISTANCE_EFFECT =
            PROCESSORS.register("vanilla_resistance_effect", () -> BuiltinDamageProcessors.VANILLA_RESISTANCE_EFFECT);

    public static final DeferredHolder<DamagePhaseProcessor, ArmorMitigationProcessor> ARMOR =
            PROCESSORS.register("armor", () -> BuiltinDamageProcessors.ARMOR);

    public static final DeferredHolder<DamagePhaseProcessor, ResistanceMitigationProcessor> RESISTANCE =
            PROCESSORS.register("resistance", () -> BuiltinDamageProcessors.RESISTANCE);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaDifficultyScalingProcessor> VANILLA_DIFFICULTY_SCALING =
            PROCESSORS.register("vanilla_difficulty_scaling", () -> BuiltinDamageProcessors.VANILLA_DIFFICULTY_SCALING);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaSpecialAttackScalingProcessor> VANILLA_SPECIAL_ATTACK_SCALING =
            PROCESSORS.register("vanilla_special_attack_scaling", () -> BuiltinDamageProcessors.VANILLA_SPECIAL_ATTACK_SCALING);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaSpearBonusProcessor> VANILLA_SPEAR_BONUS =
            PROCESSORS.register("vanilla_spear_bonus", () -> BuiltinDamageProcessors.VANILLA_SPEAR_BONUS);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaPlayerAttackScalingProcessor> VANILLA_PLAYER_ATTACK_SCALING =
            PROCESSORS.register("vanilla_player_attack_scaling", () -> BuiltinDamageProcessors.VANILLA_PLAYER_ATTACK_SCALING);

    public static final DeferredHolder<DamagePhaseProcessor, VanillaProjectileScalingProcessor> VANILLA_PROJECTILE_SCALING =
            PROCESSORS.register("vanilla_projectile_scaling", () -> BuiltinDamageProcessors.VANILLA_PROJECTILE_SCALING);

    public static void register(IEventBus modBus) {
        PROCESSORS.register(modBus);
    }

    public static List<DamagePhaseProcessor> builtInProcessors() {
        return BuiltinDamageProcessors.builtInProcessors();
    }

    public static Identifier builtInProcessorId(
            DamagePhaseProcessor processor
    ) {
        return processor == null
                ? null
                : PROCESSOR_REGISTRY.getKey(processor);
    }
}

