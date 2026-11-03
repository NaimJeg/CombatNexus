package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCodecs;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;

import java.util.Objects;

public record TargetEffectTagCondition(
        TagKey<MobEffect> tag
) implements DamageRuleCondition {

    public static final MapCodec<TargetEffectTagCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.MOB_EFFECT_TAG
                            .fieldOf("tag")
                            .forGetter(TargetEffectTagCondition::tag)
            ).apply(instance, TargetEffectTagCondition::new));

    public TargetEffectTagCondition {
        Objects.requireNonNull(tag, "tag");
    }

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.TARGET_EFFECT_TAG;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return MobEffectTagConditionSupport.matches(
                ctx.facts(ctx.victim().getUUID()).effects(), tag
        );
    }
}
