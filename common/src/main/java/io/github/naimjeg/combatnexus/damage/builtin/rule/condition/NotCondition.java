package io.github.naimjeg.combatnexus.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.damage.api.rule.CompositeDamageRuleCondition;
import io.github.naimjeg.combatnexus.damage.api.rule.RuleExecutionContext;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.List;

public record NotCondition(
        DamageRuleCondition condition
) implements CompositeDamageRuleCondition {

    public static final MapCodec<NotCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCondition.CODEC
                            .fieldOf("condition")
                            .forGetter(NotCondition::condition)
            ).apply(instance, NotCondition::new));

    public NotCondition {
        condition = Objects.requireNonNull(
                condition,
                "condition must not be null"
        );
    }

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.NOT;
    }

    @Override
    public List<DamageRuleCondition> childConditions() {
        return List.of(condition);
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return !condition.test(ctx);
    }

    @Override
    public boolean test(
            DamageRuleContext ctx,
            RuleExecutionContext executionContext
    ) {
        return !condition.test(ctx, executionContext);
    }
}
