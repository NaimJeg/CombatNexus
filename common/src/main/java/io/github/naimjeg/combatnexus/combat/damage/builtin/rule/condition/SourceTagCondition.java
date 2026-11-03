package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;

import java.util.Objects;

public record SourceTagCondition(
        Identifier tag
) implements DamageRuleCondition {
    public static final MapCodec<SourceTagCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC.fieldOf("tag")
                            .forGetter(SourceTagCondition::tag)
            ).apply(instance, SourceTagCondition::new));

    public SourceTagCondition {
        Objects.requireNonNull(tag, "tag");
    }

    @Override public Identifier type() {
        return DamageModuleConditionIds.SOURCE_TAG;
    }

    @Override public boolean test(DamageContextView ctx) {
        return ctx.sourceTags().contains(tag);
    }
}
