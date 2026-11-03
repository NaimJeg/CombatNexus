package io.github.naimjeg.combatnexus.effect.api;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class VanillaEffectContributions {
    private static final float EPSILON = 0.0001f;

    private VanillaEffectContributions() {
    }

    /** Observe the native effect; this does not install a second attribute contribution or ticker. */
    public static float resistanceReduction(LivingEntity entity) {
        if (entity == null) return 0.0f;
        MobEffectInstance effect = entity.getEffect(MobEffects.RESISTANCE);
        return effect == null ? 0.0f
                : io.github.naimjeg.combatnexus.formula.MultiplierFormula.resistanceEffectReduction(effect.getAmplifier());
    }

    public static OffensiveMobEffectBreakdown computeBreakdownForLivingAttacker(
            LivingEntity livingAttacker
    ) {
        AttributeInstance attackDamage =
                livingAttacker.getAttribute(Attributes.ATTACK_DAMAGE);

        if (attackDamage == null) {
            return OffensiveMobEffectBreakdown.NONE;
        }

        Set<Identifier> strengthModifierIds =
                collectAttackDamageModifierIds(
                        livingAttacker,
                        MobEffects.STRENGTH
                );

        Set<Identifier> weaknessModifierIds =
                collectAttackDamageModifierIds(
                        livingAttacker,
                        MobEffects.WEAKNESS
                );

        if (strengthModifierIds.isEmpty() && weaknessModifierIds.isEmpty()) {
            return OffensiveMobEffectBreakdown.NONE;
        }

        Set<Identifier> allMobEffectModifierIds = new HashSet<>();
        allMobEffectModifierIds.addAll(strengthModifierIds);
        allMobEffectModifierIds.addAll(weaknessModifierIds);

        Collection<AttributeModifier> currentModifiers =
                attackDamage.getModifiers();

        double currentValue = attackDamage.getValue();

        double valueWithoutBoth = io.github.naimjeg.combatnexus.attribute.api.AttributeReader.valueWithout(
                Attributes.ATTACK_DAMAGE,
                attackDamage.getBaseValue(),
                currentModifiers,
                allMobEffectModifierIds
        );

        float observedDelta =
                finiteDelta(currentValue, valueWithoutBoth);

        /*
         * observedDelta is the authoritative value used for vanilla base
         * reconstruction.
         *
         * strengthDelta / weaknessDelta are debug-facing approximations. If a
         * mod changes Strength or Weakness to use multiplicative operations, the
         * exact split can be order-dependent, but observedDelta still remains
         * the value DN needs.
         */
        double valueWithoutWeakness = io.github.naimjeg.combatnexus.attribute.api.AttributeReader.valueWithout(
                Attributes.ATTACK_DAMAGE,
                attackDamage.getBaseValue(),
                currentModifiers,
                weaknessModifierIds
        );

        float strengthDelta = strengthModifierIds.isEmpty()
                ? 0.0f
                : finiteDelta(valueWithoutWeakness, valueWithoutBoth);

        float weaknessDelta = weaknessModifierIds.isEmpty()
                ? 0.0f
                : observedDelta - strengthDelta;

        float enabledDelta = observedDelta;

        return new OffensiveMobEffectBreakdown(
                strengthDelta,
                weaknessDelta,
                observedDelta,
                enabledDelta
        );
    }

    private static Set<Identifier> collectAttackDamageModifierIds(
            LivingEntity attacker,
            Holder<MobEffect> effect
    ) {
        MobEffectInstance instance = attacker.getEffect(effect);

        if (instance == null) {
            return Set.of();
        }

        Set<Identifier> ids = new HashSet<>();
        if (attacker.level() instanceof net.minecraft.server.level.ServerLevel level) {
            var definition = effect.unwrapKey().orElseThrow().identifier();
            for (var managed : io.github.naimjeg.combatnexus.combat.api.CombatWorldSessions.managedEffects(level, attacker.getUUID()))
                if (definition.equals(managed.display().nativeId()))
                    for (var contribution : managed.attributes())
                        if (contribution.attribute().equals(Attributes.ATTACK_DAMAGE.unwrapKey().orElseThrow().identifier()))
                            ids.add(EffectContributionIdentity.id(managed, contribution));
        }

        effect.value().createModifiers(
                instance.getAmplifier(),
                (attribute, modifier) -> {
                    if (Attributes.ATTACK_DAMAGE.equals(attribute)) {
                        ids.add(modifier.id());
                    }
                }
        );

        return ids;
    }

    private static float finiteDelta(
            double valueWithModifier,
            double valueWithoutModifier
    ) {
        double delta = valueWithModifier - valueWithoutModifier;

        if (!Double.isFinite(delta)) {
            return 0.0f;
        }

        if (Math.abs(delta) <= EPSILON) {
            return 0.0f;
        }

        return (float) delta;
    }

    public record OffensiveMobEffectBreakdown(
            float strengthDelta,
            float weaknessDelta,
            float observedDelta,
            float enabledDelta
    ) {
        public static final OffensiveMobEffectBreakdown NONE =
                new OffensiveMobEffectBreakdown(
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f
                );

        public boolean hasEnabledDelta() {
            return Math.abs(enabledDelta) > EPSILON;
        }

        public boolean hasObservedDelta() {
            return Math.abs(observedDelta) > EPSILON;
        }

        public boolean hasObservedWeaknessDelta() {
            return Math.abs(weaknessDelta) > EPSILON;
        }
    }
}
