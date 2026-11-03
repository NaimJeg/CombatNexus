package io.github.naimjeg.combatnexus.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public record DamageTypeTagCondition(
        TagKey<DamageType> tag
) implements DamageRuleCondition {

    public static final MapCodec<DamageTypeTagCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC
                            .fieldOf("tag")
                            .xmap(
                                    id -> TagKey.create(Registries.DAMAGE_TYPE, id),
                                    TagKey::location
                            )
                            .forGetter(DamageTypeTagCondition::tag)
            ).apply(instance, DamageTypeTagCondition::new));

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.DAMAGE_TYPE_TAG;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return ctx.source().is(tag);
    }
}
