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

import java.util.Set;

public record AddGlobalMitigationOperation(
        float value
) implements DamageRuleOperation {

    public static final MapCodec<AddGlobalMitigationOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.FLOAT
                            .fieldOf("value")
                            .forGetter(AddGlobalMitigationOperation::value)
            ).apply(instance, AddGlobalMitigationOperation::new));

    @Override
    public Identifier type() {
        return DamageModuleOperationIds.ADD_GLOBAL_MITIGATION;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        return ctx.tryAddGlobalMitigation(
                value,
                RuleTraceIds.ADD_GLOBAL_MITIGATION
        );
    }

    @Override
    public Set<DamagePhase> supportedPhases() {
        return Set.of(DamagePhase.MITIGATION_SETUP);
    }

    @Override
    public float stackingValue() {
        return value;
    }
}
