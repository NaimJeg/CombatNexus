package io.github.naimjeg.combatnexus.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleConditionIds;
import io.github.naimjeg.combatnexus.damage.util.EntityConditionUtil;
import net.minecraft.resources.Identifier;

public record AttackerIsBossCondition() implements DamageRuleCondition {

    public static final MapCodec<AttackerIsBossCondition> CODEC =
            MapCodec.unit(new AttackerIsBossCondition());

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.ATTACKER_IS_BOSS;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return EntityConditionUtil.isBoss(ctx.logicalAttacker());
    }
}
