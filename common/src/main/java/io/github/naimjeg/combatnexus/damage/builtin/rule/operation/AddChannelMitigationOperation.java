package io.github.naimjeg.combatnexus.damage.builtin.rule.operation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.enums.DamageChannel;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.damage.api.rule.ChannelReferencingOperation;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCodecs;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleOperation;
import io.github.naimjeg.combatnexus.damage.api.rule.RuleTraceIds;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleOperationIds;
import net.minecraft.resources.Identifier;

import java.util.List;

public record AddChannelMitigationOperation(
        Identifier channelId,
        float value
) implements DamageRuleOperation, ChannelReferencingOperation {

    public static final MapCodec<AddChannelMitigationOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("channel")
                            .forGetter(AddChannelMitigationOperation::channelId),

                    Codec.FLOAT
                            .fieldOf("value")
                            .forGetter(AddChannelMitigationOperation::value)
            ).apply(instance, AddChannelMitigationOperation::new));

    public AddChannelMitigationOperation(
            DamageChannel channel,
            float value
    ) {
        this(DamageOperationChannelIds.idOrUntyped(channel), value);
    }

    public AddChannelMitigationOperation {
        channelId = DamageOperationChannelIds.idOrUntyped(channelId);
    }

    @Override
    public Identifier type() {
        return DamageModuleOperationIds.ADD_CHANNEL_MITIGATION;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        return ctx.tryAddChannelMitigation(
                DamageOperationChannelIds.resolve(channelId),
                value,
                RuleTraceIds.ADD_CHANNEL_MITIGATION
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
