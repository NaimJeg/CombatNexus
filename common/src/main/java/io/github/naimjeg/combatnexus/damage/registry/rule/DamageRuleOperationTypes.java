package io.github.naimjeg.combatnexus.damage.registry.rule;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleOperationIds;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleOperation;
import io.github.naimjeg.combatnexus.damage.builtin.rule.operation.*;
import io.github.naimjeg.combatnexus.damage.core.lifecycle.DamageModuleLifecycle;
import io.github.naimjeg.combatnexus.damage.core.lifecycle.DamageModuleRegistrationAccess;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Runtime codec registry for damage-rule operations. */
public final class DamageRuleOperationTypes {
    private static final Map<Identifier, MapCodec<? extends DamageRuleOperation>> CODECS =
            new HashMap<>();

    static {
        registerBuiltin(
                DamageModuleOperationIds.ADD_BASE_DAMAGE,
                AddBaseDamageOperation.CODEC);
        registerBuiltin(
                DamageModuleOperationIds.ADD_CHANNEL_PRE_MULTIPLIER,
                AddChannelPreMultiplierOperation.CODEC);
        registerBuiltin(
                DamageModuleOperationIds.ADD_CHANNEL_POST_MULTIPLIER,
                AddChannelPostMultiplierOperation.CODEC);
        registerBuiltin(
                DamageModuleOperationIds.ADD_GLOBAL_POST_MULTIPLIER,
                AddGlobalPostMultiplierOperation.CODEC);
        registerBuiltin(
                DamageModuleOperationIds.OVERRIDE_FINAL_DAMAGE,
                OverrideFinalDamageOperation.CODEC);
        registerBuiltin(
                DamageModuleOperationIds.CANCEL_DAMAGE,
                CancelDamageOperation.CODEC);
        registerBuiltin(
                DamageModuleOperationIds.ADD_TEMPORARY_RESISTANCE,
                AddTemporaryResistanceOperation.CODEC);
        registerBuiltin(
                DamageModuleOperationIds.ADD_GLOBAL_PRE_MULTIPLIER,
                AddGlobalPreMultiplierOperation.CODEC);
        registerBuiltin(
                DamageModuleOperationIds.CONVERT_DAMAGE,
                ConvertDamageOperation.CODEC);
        registerBuiltin(
                DamageModuleOperationIds.GAIN_EXTRA_DAMAGE,
                GainExtraDamageOperation.CODEC);
        registerBuiltin(
                DamageModuleOperationIds.ADD_CHANNEL_MITIGATION,
                AddChannelMitigationOperation.CODEC);
        registerBuiltin(
                DamageModuleOperationIds.ADD_TRUE_DAMAGE,
                AddTrueDamageOperation.CODEC);
        registerBuiltin(
                DamageModuleOperationIds.ADD_GLOBAL_MITIGATION,
                AddGlobalMitigationOperation.CODEC);
        registerBuiltin(
                DamageModuleOperationIds.MULTIPLY_ARMOR_EFFECTIVENESS,
                MultiplyArmorEffectivenessOperation.CODEC);
    }

    private DamageRuleOperationTypes() {
    }

    public static synchronized void register(
            DamageModuleRegistrationAccess access,
            Identifier id,
            MapCodec<? extends DamageRuleOperation> codec
    ) {
        DamageModuleLifecycle.requireRegistering(
                access,
                "DamageRuleOperationTypes.register"
        );
        registerChecked(id, codec);
    }

    private static void registerBuiltin(
            Identifier id,
            MapCodec<? extends DamageRuleOperation> codec
    ) {
        registerChecked(id, codec);
    }

    private static void registerChecked(
            Identifier id,
            MapCodec<? extends DamageRuleOperation> codec
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(codec, "codec");

        if (CODECS.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Duplicate DamageModule rule operation type: " + id
            );
        }

        CODECS.put(id, codec);
    }

    public static synchronized MapCodec<? extends DamageRuleOperation> codec(Identifier id) {
        MapCodec<? extends DamageRuleOperation> codec = CODECS.get(id);

        if (codec == null) {
            throw new IllegalArgumentException(
                    "Unknown DamageModule rule operation type: " + id
            );
        }

        return codec;
    }

    public static synchronized Set<Identifier> registeredTypes() {
        return Set.copyOf(CODECS.keySet());
    }
}
