package io.github.naimjeg.combatnexus.combat.damage.api.display.narrative;

import io.github.naimjeg.combatnexus.presentation.api.phrase.PhraseArguments;
import io.github.naimjeg.combatnexus.presentation.api.phrase.PhraseVariant;
import io.github.naimjeg.combatnexus.presentation.api.phrase.RulePhrase;
import io.github.naimjeg.combatnexus.combat.damage.api.display.phrase.RulePhraseRegistry;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleOperation;
import io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition.AllOfCondition;
import io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition.AlwaysCondition;
import io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition.AnyOfCondition;
import io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition.NotCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.github.naimjeg.combatnexus.combat.damage.api.display.phrase.DamageModuleRulePhrases.UNKNOWN_CONDITION;
import static io.github.naimjeg.combatnexus.combat.damage.api.display.phrase.DamageModuleRulePhrases.UNKNOWN_EFFECT;

public final class RuleNarrativePlanner {
    private final RulePhraseRegistry registry;

    public RuleNarrativePlanner(RulePhraseRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public RuleNarrative plan(DamageRuleDefinition rule) {
        Objects.requireNonNull(rule, "rule");
        return plan(rule.conditions(), rule.operations());
    }

    public RuleNarrative plan(
            List<DamageRuleCondition> conditionValues,
            List<DamageRuleOperation> operationValues
    ) {
        List<DamageRuleCondition> safeConditions = conditionValues == null
                ? List.of() : List.copyOf(conditionValues);
        List<DamageRuleOperation> safeOperations = operationValues == null
                ? List.of() : List.copyOf(operationValues);
        List<ConditionExpression> conditions = safeConditions.stream()
                .map(this::condition)
                .toList();
        ConditionExpression root = conditions.isEmpty()
                ? new ConditionExpression.Always()
                : new ConditionExpression.AllOf(conditions, true);

        List<RulePhrase> effects = new ArrayList<>();
        for (DamageRuleOperation operation : safeOperations) {
            effects.add(registry.describeOperation(operation)
                    .orElseGet(() -> registry.create(
                            UNKNOWN_EFFECT,
                            PhraseVariant.DEFAULT,
                            PhraseArguments.EMPTY
                    )));
        }
        return new RuleNarrative(root, effects);
    }

    private ConditionExpression condition(DamageRuleCondition condition) {
        if (condition instanceof AlwaysCondition) {
            return new ConditionExpression.Always();
        }
        if (condition instanceof AllOfCondition allOf) {
            return new ConditionExpression.AllOf(
                    allOf.conditions().stream().map(this::condition).toList(),
                    false
            );
        }
        if (condition instanceof AnyOfCondition anyOf) {
            return new ConditionExpression.AnyOf(
                    anyOf.conditions().stream().map(this::condition).toList()
            );
        }
        if (condition instanceof NotCondition not) {
            return new ConditionExpression.Not(condition(not.condition()));
        }
        RulePhrase phrase = registry.describeCondition(condition)
                .orElseGet(() -> registry.create(
                        UNKNOWN_CONDITION,
                        PhraseVariant.DEFAULT,
                        PhraseArguments.EMPTY
                ));
        return new ConditionExpression.Phrase(phrase);
    }

}
