package io.github.naimjeg.combatnexus.damage.registry.rule;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleConditionIds;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.damage.builtin.rule.condition.*;
import io.github.naimjeg.combatnexus.damage.core.lifecycle.DamageModuleLifecycle;
import io.github.naimjeg.combatnexus.damage.core.lifecycle.DamageModuleRegistrationAccess;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Runtime codec registry for damage-rule conditions. */
public final class DamageRuleConditionTypes {
    private static final Map<Identifier, MapCodec<? extends DamageRuleCondition>> CODECS =
            new HashMap<>();

    static {
        registerBuiltin(DamageModuleConditionIds.ALWAYS, AlwaysCondition.CODEC);
        registerBuiltin(DamageModuleConditionIds.ALL_OF, AllOfCondition.CODEC);
        registerBuiltin(DamageModuleConditionIds.ANY_OF, AnyOfCondition.CODEC);
        registerBuiltin(DamageModuleConditionIds.NOT, NotCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.IS_CRITICAL,
                IsCriticalCondition.CODEC);

        registerBuiltin(
                DamageModuleConditionIds.TARGET_ON_FIRE,
                TargetOnFireCondition.CODEC);

        registerBuiltin(
                DamageModuleConditionIds.DAMAGE_TYPE_TAG,
                DamageTypeTagCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.DAMAGE_TYPE_IS,
                DamageTypeIsCondition.CODEC);

        registerBuiltin(
                DamageModuleConditionIds.ATTACKER_HEALTH_BELOW,
                AttackerHealthBelowCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.ATTACKER_HEALTH_ABOVE,
                AttackerHealthAboveCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.TARGET_HEALTH_BELOW,
                TargetHealthBelowCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.TARGET_HEALTH_ABOVE,
                TargetHealthAboveCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.ATTACKER_HAS_EFFECT,
                AttackerHasEffectCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.TARGET_HAS_EFFECT,
                TargetHasEffectCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.ATTACKER_EFFECT_TAG,
                AttackerEffectTagCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.TARGET_EFFECT_TAG,
                TargetEffectTagCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.SOURCE_ACTION_IS,
                SourceActionIsCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.SOURCE_TAG,
                SourceTagCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.REQUEST_KIND_IS,
                RequestKindIsCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.IS_PRIMARY_DAMAGE,
                IsPrimaryDamageCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.IS_PROC_DAMAGE,
                IsProcDamageCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.HAS_PARENT_DAMAGE,
                HasParentDamageCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.PROC_ALLOWED,
                ProcAllowedCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.DAMAGE_CHANNEL_IS,
                DamageChannelIsCondition.CODEC);

        registerBuiltin(
                DamageModuleConditionIds.TARGET_ENTITY_TYPE_IS,
                TargetEntityTypeIsCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.ATTACKER_ENTITY_TYPE_IS,
                AttackerEntityTypeIsCondition.CODEC);

        registerBuiltin(
                DamageModuleConditionIds.TARGET_MOB_CATEGORY_IS,
                TargetMobCategoryIsCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.ATTACKER_MOB_CATEGORY_IS,
                AttackerMobCategoryIsCondition.CODEC);

        registerBuiltin(
                DamageModuleConditionIds.TARGET_IS_BOSS,
                TargetIsBossCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.ATTACKER_IS_BOSS,
                AttackerIsBossCondition.CODEC);

        registerBuiltin(
                DamageModuleConditionIds.TARGET_ENTITY_TYPE_TAG,
                TargetEntityTypeTagCondition.CODEC);
        registerBuiltin(
                DamageModuleConditionIds.ATTACKER_ENTITY_TYPE_TAG,
                AttackerEntityTypeTagCondition.CODEC);
    }

    private DamageRuleConditionTypes() {
    }

    public static synchronized void register(
            DamageModuleRegistrationAccess access,
            Identifier id,
            MapCodec<? extends DamageRuleCondition> codec
    ) {
        DamageModuleLifecycle.requireRegistering(
                access,
                "DamageRuleConditionTypes.register"
        );
        registerChecked(id, codec);
    }

    private static void registerBuiltin(
            Identifier id,
            MapCodec<? extends DamageRuleCondition> codec
    ) {
        registerChecked(id, codec);
    }

    private static void registerChecked(
            Identifier id,
            MapCodec<? extends DamageRuleCondition> codec
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(codec, "codec");

        if (CODECS.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Duplicate DamageModule rule condition type: " + id
            );
        }

        CODECS.put(id, codec);
    }

    public static synchronized MapCodec<? extends DamageRuleCondition> codec(Identifier id) {
        MapCodec<? extends DamageRuleCondition> codec = CODECS.get(id);

        if (codec == null) {
            throw new IllegalArgumentException(
                    "Unknown DamageModule rule condition type: " + id
            );
        }

        return codec;
    }

    public static synchronized Set<Identifier> registeredTypes() {
        return Set.copyOf(CODECS.keySet());
    }
}
