package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public record AttackerHasEffectCondition(
        Identifier effect
) implements DamageRuleCondition {

    public static final MapCodec<AttackerHasEffectCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC
                            .fieldOf("effect")
                            .forGetter(AttackerHasEffectCondition::effect)
            ).apply(instance, AttackerHasEffectCondition::new));

    private static Optional<Holder.Reference<MobEffect>> resolveEffect(
            LivingEntity entity,
            Identifier effectId
    ) {
        Registry<MobEffect> registry =
                entity.level()
                        .registryAccess()
                        .lookupOrThrow(Registries.MOB_EFFECT);

        ResourceKey<MobEffect> key =
                ResourceKey.create(Registries.MOB_EFFECT, effectId);

        return registry.get(key);
    }

    @Override
    public Identifier type() {
        return DamageModuleConditionIds.ATTACKER_HAS_EFFECT;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        if (!(ctx.logicalAttacker() instanceof LivingEntity livingAttacker)) {
            return false;
        }

        Optional<Holder.Reference<MobEffect>> holder =
                resolveEffect(livingAttacker, effect);

        return holder.isPresent()
                && ctx.facts(livingAttacker.getUUID()).hasEffect(holder.get());
    }
}
