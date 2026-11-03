package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.operation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.ChannelReferencingOperation;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCodecs;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleOperation;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.RuleTraceIds;
import io.github.naimjeg.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleOperationIds;
import net.minecraft.resources.Identifier;

import java.util.List;

public record ConvertDamageOperation(
        Identifier fromChannel,
        Identifier toChannel,
        float ratio
) implements DamageRuleOperation, ChannelReferencingOperation {

    public static final MapCodec<ConvertDamageOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("from_channel")
                            .forGetter(ConvertDamageOperation::fromChannel),

                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("to_channel")
                            .forGetter(ConvertDamageOperation::toChannel),

                    DamageRuleCodecs.RATIO_0_TO_1
                            .fieldOf("ratio")
                            .forGetter(ConvertDamageOperation::ratio)
            ).apply(instance, ConvertDamageOperation::new));

    @Override
    public Identifier type() {
        return DamageModuleOperationIds.CONVERT_DAMAGE;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        return ctx.tryConvertDamage(
                ctx.resolveChannel(fromChannel),
                ctx.resolveChannel(toChannel),
                ratio,
                RuleTraceIds.CONVERT_DAMAGE
        );
    }

    @Override
    public java.util.Set<DamagePhase> supportedPhases() {
        return java.util.Set.of(DamagePhase.TYPE_SCALING);
    }

    @Override
    public List<Identifier> referencedChannels() {
        return List.of(fromChannel, toChannel);
    }

    @Override
    public float stackingValue() {
        return ratio;
    }
}
