package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.naimjeg.combatnexus.combat.damage.util.EntityConditionUtil;
import net.minecraft.resources.Identifier;

public record TargetIsBossCondition() implements DamageRuleCondition {

    public static final MapCodec<TargetIsBossCondition> CODEC =
            MapCodec.unit(new TargetIsBossCondition());

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.TARGET_IS_BOSS;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return EntityConditionUtil.isBoss(ctx.victim());
    }
}
