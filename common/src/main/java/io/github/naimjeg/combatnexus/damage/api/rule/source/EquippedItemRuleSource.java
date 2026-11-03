package io.github.naimjeg.combatnexus.damage.api.rule.source;

import java.util.List;

/**
 * Supplies external equipment stacks to DamageModule' normal item-security and
 * entry/affix collection path. Implementations never receive an internal
 * collector or transaction object.
 */
@FunctionalInterface
public interface EquippedItemRuleSource {
    List<EquippedItemRuleContribution> collect(
            EquippedItemRuleSourceQuery query
    );
}
