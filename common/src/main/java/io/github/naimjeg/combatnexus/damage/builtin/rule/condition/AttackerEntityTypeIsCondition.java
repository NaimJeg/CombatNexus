package io.github.naimjeg.combatnexus.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleConditionIds;
import io.github.naimjeg.combatnexus.damage.util.EntityConditionUtil;
import net.minecraft.resources.Identifier;

public record AttackerEntityTypeIsCondition(
        Identifier entityType
) implements DamageRuleCondition {

    public static final MapCodec<AttackerEntityTypeIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC
                            .fieldOf("entity_type")
                            .forGetter(AttackerEntityTypeIsCondition::entityType)
            ).apply(instance, AttackerEntityTypeIsCondition::new));

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.ATTACKER_ENTITY_TYPE_IS;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return EntityConditionUtil.isEntityType(
                ctx.logicalAttacker(),
                entityType
        );
    }
}
