package io.github.naimjeg.combatnexus.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.enums.DamageChannel;
import io.github.naimjeg.combatnexus.damage.api.rule.ChannelReferencingCondition;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCodecs;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.damage.core.registry.DamageChannelRegistry;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;

import java.util.List;

public record DamageChannelIsCondition(
        Identifier channelId
) implements DamageRuleCondition, ChannelReferencingCondition {

    public static final MapCodec<DamageChannelIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("channel")
                            .forGetter(DamageChannelIsCondition::channelId)
            ).apply(instance, DamageChannelIsCondition::new));

    public DamageChannelIsCondition(DamageChannel channel) {
        this(channel.id());
    }

    public DamageChannel channel() {
        return DamageChannelRegistry.getChannelOrUntyped(channelId);
    }

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.DAMAGE_CHANNEL_IS;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return ctx.hasActiveDamageInChannel(channel());
    }

    @Override
    public List<Identifier> referencedChannels() {
        return List.of(channelId);
    }
}
