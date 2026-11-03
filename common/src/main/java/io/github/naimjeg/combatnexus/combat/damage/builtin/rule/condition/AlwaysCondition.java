package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;

public record AlwaysCondition() implements DamageRuleCondition {

    public static final MapCodec<AlwaysCondition> CODEC =
            MapCodec.unit(new AlwaysCondition());

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.ALWAYS;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return true;
    }
}
