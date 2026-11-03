package io.github.naimjeg.combatnexus.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;

public record HasParentDamageCondition() implements DamageRuleCondition {
    public static final MapCodec<HasParentDamageCondition> CODEC =
            MapCodec.unit(new HasParentDamageCondition());
    @Override public Identifier type() {
        return DamageModuleConditionIds.HAS_PARENT_DAMAGE;
    }
    @Override public boolean test(DamageRuleContext ctx) {
        return ctx.lineage().hasParent();
    }
}
