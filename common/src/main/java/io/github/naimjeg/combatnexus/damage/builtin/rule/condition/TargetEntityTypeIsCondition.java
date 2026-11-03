package io.github.naimjeg.combatnexus.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleConditionIds;
import io.github.naimjeg.combatnexus.damage.util.EntityConditionUtil;
import net.minecraft.resources.Identifier;

public record TargetEntityTypeIsCondition(
        Identifier entityType
) implements DamageRuleCondition {

    public static final MapCodec<TargetEntityTypeIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC
                            .fieldOf("entity_type")
                            .forGetter(TargetEntityTypeIsCondition::entityType)
            ).apply(instance, TargetEntityTypeIsCondition::new));

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.TARGET_ENTITY_TYPE_IS;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return EntityConditionUtil.isEntityType(
                ctx.victim(),
                entityType
        );
    }
}
