package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.naimjeg.combatnexus.combat.damage.util.EntityConditionUtil;
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
    public boolean test(DamageContextView ctx) {
        return EntityConditionUtil.isEntityType(
                ctx.victim(),
                entityType
        );
    }
}
