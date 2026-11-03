package io.github.naimjeg.combatnexus.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.damage.DamageRequestKind;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;

public record IsProcDamageCondition() implements DamageRuleCondition {
    public static final MapCodec<IsProcDamageCondition> CODEC =
            MapCodec.unit(new IsProcDamageCondition());
    @Override public Identifier type() {
        return DamageModuleConditionIds.IS_PROC_DAMAGE;
    }
    @Override public boolean test(DamageRuleContext ctx) {
        return ctx.requestKind() == DamageRequestKind.PROC;
    }
}
