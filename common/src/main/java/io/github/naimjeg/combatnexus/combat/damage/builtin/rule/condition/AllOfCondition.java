package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.CompositeDamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleLimits;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.RuleExecutionContext;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record AllOfCondition(
        List<DamageRuleCondition> conditions
) implements CompositeDamageRuleCondition {

    public static final MapCodec<AllOfCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleLimits.boundedList(
                                    DamageRuleCondition.CODEC,
                                    DamageRuleLimits.MAX_RULE_CONDITIONS,
                                    "all_of conditions"
                            )
                            .fieldOf("conditions")
                            .forGetter(AllOfCondition::conditions)
            ).apply(instance, AllOfCondition::new));

    public AllOfCondition {
        Objects.requireNonNull(
                conditions,
                "conditions must not be null"
        );
        List<DamageRuleCondition> copy =
                new ArrayList<>(conditions.size());

        for (DamageRuleCondition condition : conditions) {
            copy.add(Objects.requireNonNull(
                    condition,
                    "conditions must not contain null elements"
            ));
        }

        conditions = List.copyOf(copy);
    }

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.ALL_OF;
    }

    @Override
    public List<DamageRuleCondition> childConditions() {
        return conditions;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        for (DamageRuleCondition condition : conditions) {
            if (!condition.test(ctx)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean test(
            DamageContextView ctx,
            RuleExecutionContext executionContext
    ) {
        for (DamageRuleCondition condition : conditions) {
            if (!condition.test(ctx, executionContext)) {
                return false;
            }
        }

        return true;
    }
}
