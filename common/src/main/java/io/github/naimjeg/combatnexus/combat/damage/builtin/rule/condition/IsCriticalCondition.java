package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;

public record IsCriticalCondition() implements DamageRuleCondition {

    public static final MapCodec<IsCriticalCondition> CODEC =
            MapCodec.unit(new IsCriticalCondition());

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.IS_CRITICAL;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return ctx.isCritical();
    }
}
