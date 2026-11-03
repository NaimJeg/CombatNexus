package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.naimjeg.combatnexus.combat.damage.util.EntityConditionUtil;
import net.minecraft.resources.Identifier;

public record AttackerIsBossCondition() implements DamageRuleCondition {

    public static final MapCodec<AttackerIsBossCondition> CODEC =
            MapCodec.unit(new AttackerIsBossCondition());

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.ATTACKER_IS_BOSS;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return EntityConditionUtil.isBoss(ctx.logicalAttacker());
    }
}
