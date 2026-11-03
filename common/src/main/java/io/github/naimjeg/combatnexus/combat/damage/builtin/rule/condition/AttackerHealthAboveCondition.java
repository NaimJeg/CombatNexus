package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

public record AttackerHealthAboveCondition(
        float threshold
) implements DamageRuleCondition {

    public static final MapCodec<AttackerHealthAboveCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.floatRange(0.0f, 1.0f)
                            .fieldOf("threshold")
                            .forGetter(AttackerHealthAboveCondition::threshold)
            ).apply(instance, AttackerHealthAboveCondition::new));

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.ATTACKER_HEALTH_ABOVE;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        if (!(ctx.logicalAttacker() instanceof LivingEntity livingAttacker)) {
            return false;
        }

        float max = ctx.facts(livingAttacker.getUUID()).maxHealth();
        if (max <= 0.0f) {
            return false;
        }

        return ctx.facts(livingAttacker.getUUID()).health() / max > threshold;
    }
}
