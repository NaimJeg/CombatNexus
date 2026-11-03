package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;

public record TargetHealthBelowCondition(
        float threshold
) implements DamageRuleCondition {

    public static final MapCodec<TargetHealthBelowCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.floatRange(0.0f, 1.0f)
                            .fieldOf("threshold")
                            .forGetter(TargetHealthBelowCondition::threshold)
            ).apply(instance, TargetHealthBelowCondition::new));

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.TARGET_HEALTH_BELOW;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        if (ctx.victim() == null) return false;

        float max = ctx.facts(ctx.victim().getUUID()).maxHealth();
        if (max <= 0.0f) return false;

        return ctx.facts(ctx.victim().getUUID()).health() / max < threshold;
    }
}
