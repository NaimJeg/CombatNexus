package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;

public record AttackerHealthBelowCondition(
        float threshold
) implements DamageRuleCondition {

    public static final MapCodec<AttackerHealthBelowCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.floatRange(0.0f, 1.0f)
                            .fieldOf("threshold")
                            .forGetter(AttackerHealthBelowCondition::threshold)
            ).apply(instance, AttackerHealthBelowCondition::new));

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.ATTACKER_HEALTH_BELOW;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        if (ctx.logicalAttacker() == null) {
            return false;
        }

        float maxHealth = ctx.facts(ctx.logicalAttacker().getUUID()).maxHealth();

        if (maxHealth <= 0.0f) {
            return false;
        }

        float ratio = ctx.facts(ctx.logicalAttacker().getUUID()).health() / maxHealth;
        return ratio < threshold;
    }
}
