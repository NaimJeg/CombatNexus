package io.github.naimjeg.combatnexus.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleConditionIds;
import io.github.naimjeg.combatnexus.damage.util.EntityConditionUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public record AttackerEntityTypeTagCondition(
        TagKey<EntityType<?>> tag
) implements DamageRuleCondition {

    public static final MapCodec<AttackerEntityTypeTagCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    TagKey.codec(Registries.ENTITY_TYPE)
                            .fieldOf("tag")
                            .forGetter(AttackerEntityTypeTagCondition::tag)
            ).apply(instance, AttackerEntityTypeTagCondition::new));

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.ATTACKER_ENTITY_TYPE_TAG;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return EntityConditionUtil.isEntityTypeTag(
                ctx.logicalAttacker(),
                tag
        );
    }
}
