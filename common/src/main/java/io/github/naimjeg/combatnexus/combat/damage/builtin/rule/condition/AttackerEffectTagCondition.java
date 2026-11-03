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
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

public record AttackerEffectTagCondition(
        TagKey<MobEffect> tag
) implements DamageRuleCondition {

    public static final MapCodec<AttackerEffectTagCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.MOB_EFFECT_TAG
                            .fieldOf("tag")
                            .forGetter(AttackerEffectTagCondition::tag)
            ).apply(instance, AttackerEffectTagCondition::new));

    public AttackerEffectTagCondition {
        Objects.requireNonNull(tag, "tag");
    }

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.ATTACKER_EFFECT_TAG;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        LivingEntity attacker = ctx.logicalAttacker();
        if (attacker == null) return false;
        return MobEffectTagConditionSupport.matches(
                ctx.facts(attacker.getUUID()).effects(), tag
        );
    }
}
