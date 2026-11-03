package io.github.naimjeg.combatnexus.damage.builtin.rule.operation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleOperation;
import io.github.naimjeg.combatnexus.damage.api.rule.RuleTraceIds;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleOperationIds;
import net.minecraft.resources.Identifier;

public record AddGlobalPostMultiplierOperation(
        float value
) implements DamageRuleOperation {

    public static final MapCodec<AddGlobalPostMultiplierOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.FLOAT
                            .fieldOf("value")
                            .forGetter(AddGlobalPostMultiplierOperation::value)
            ).apply(instance, AddGlobalPostMultiplierOperation::new));

    @Override
    public Identifier type() {
        return DamageModuleOperationIds.ADD_GLOBAL_POST_MULTIPLIER;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        return ctx.tryAddGlobalPostMultiplier(
                value,
                RuleTraceIds.ADD_GLOBAL_POST_MULTIPLIER
        );
    }

    @Override
    public java.util.Set<DamagePhase> supportedPhases() {
        return java.util.Set.of(
                DamagePhase.CONDITIONAL_MULTI,
                DamagePhase.GLOBAL_ADJUSTMENT
        );
    }

    @Override
    public float stackingValue() {
        return value;
    }
}
