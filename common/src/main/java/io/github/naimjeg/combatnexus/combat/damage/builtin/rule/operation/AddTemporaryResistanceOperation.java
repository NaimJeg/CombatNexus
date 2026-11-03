package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.operation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageChannel;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.ChannelReferencingOperation;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCodecs;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleOperation;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.RuleTraceIds;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleOperationIds;
import net.minecraft.resources.Identifier;

import java.util.List;

public record AddTemporaryResistanceOperation(
        Identifier channelId,
        float value
) implements DamageRuleOperation, ChannelReferencingOperation {

    public static final MapCodec<AddTemporaryResistanceOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("channel")
                            .forGetter(AddTemporaryResistanceOperation::channelId),

                    Codec.FLOAT
                            .fieldOf("value")
                            .forGetter(AddTemporaryResistanceOperation::value)
            ).apply(instance, AddTemporaryResistanceOperation::new));

    public AddTemporaryResistanceOperation(
            DamageChannel channel,
            float value
    ) {
        this(DamageOperationChannelIds.idOrUntyped(channel), value);
    }

    public AddTemporaryResistanceOperation {
        channelId = DamageOperationChannelIds.idOrUntyped(channelId);
    }

    public DamageChannel channel() {
        return DamageOperationChannelIds.resolve(channelId);
    }

    @Override
    public Identifier type() {
        return DamageModuleOperationIds.ADD_TEMPORARY_RESISTANCE;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        return ctx.tryAddTemporaryResistance(
                ctx.resolveChannel(channelId),
                value,
                RuleTraceIds.ADD_TEMPORARY_RESISTANCE
        );
    }

    @Override
    public java.util.Set<DamagePhase> supportedPhases() {
        return java.util.Set.of(DamagePhase.MITIGATION_SETUP);
    }

    @Override
    public List<Identifier> referencedChannels() {
        return List.of(channelId);
    }

    @Override
    public float stackingValue() {
        return value;
    }
}
