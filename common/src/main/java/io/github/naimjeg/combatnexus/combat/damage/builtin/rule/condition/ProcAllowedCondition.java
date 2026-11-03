package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;

/** Tests the current origin's frozen downstream PROC permission. */
public record ProcAllowedCondition() implements DamageRuleCondition {
    public static final MapCodec<ProcAllowedCondition> CODEC =
            MapCodec.unit(new ProcAllowedCondition());
    @Override public Identifier type() {
        return DamageModuleConditionIds.PROC_ALLOWED;
    }
    @Override public boolean test(DamageContextView ctx) {
        return ctx.origin().triggerPolicy().procAllowed();
    }
}
