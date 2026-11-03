package io.github.naimjeg.combatnexus.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;

public record DamageTypeIsCondition(
        Identifier damageType
) implements DamageRuleCondition {

    public static final MapCodec<DamageTypeIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC
                            .fieldOf("damage_type")
                            .forGetter(DamageTypeIsCondition::damageType)
            ).apply(instance, DamageTypeIsCondition::new));

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.DAMAGE_TYPE_IS;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return ctx.source().typeHolder()
                .unwrapKey()
                .map(key -> key.identifier().equals(damageType))
                .orElse(false);
    }
}
