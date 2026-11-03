package io.github.naimjeg.combatnexus.combat.damage.api.rule;

import com.mojang.serialization.Codec;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.registry.rule.DamageRuleConditionTypes;
import net.minecraft.resources.Identifier;

/**
 * A rule condition supplied through the Java API.
 *
 * <p>Implementations must remain structurally immutable after they are
 * attached to a rule. DamageModule defensively copies its own lists and all
 * built-in composite conditions, but cannot copy arbitrary third-party
 * implementation state. Third-party conditions must likewise return stable,
 * immutable reference metadata. Conditions that own nested conditions must
 * also implement {@link CompositeDamageRuleCondition}; otherwise the framework
 * treats them as a trusted Java leaf for structural and reference traversal.</p>
 */
public interface DamageRuleCondition {

    Codec<DamageRuleCondition> CODEC =
            Identifier.CODEC.dispatch(
                    "type",
                    DamageRuleCondition::type,
                    DamageRuleConditionTypes::codec
            );

    Identifier type();

    boolean test(DamageContextView ctx);

    default boolean test(
            DamageContextView ctx,
            RuleExecutionContext executionContext
    ) {
        return test(ctx);
    }
}
