package io.github.naimjeg.combatnexus.combat.damage.core.pipeline;

import io.github.naimjeg.combatnexus.combat.damage.api.context.NativeDamageInput;
import io.github.naimjeg.combatnexus.combat.damage.config.VanillaCompatibilitySettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import java.util.*;

final class NativeDamageInputs {
    private NativeDamageInputs() {}
    static NativeDamageInput capture(DamageModuleContext context) {
        var settings = context.settings().values().vanillaCompatibility();
        var snapshot = context.getVanillaSnapshot();
        var offense = Optional.ofNullable(snapshot).map(s -> {
            var d = s.preEventDelta();
            return new NativeDamageInput.Offense(s.preEnchantDamage(), s.postEnchantDamage(), s.enchantDelta(),
                s.eventOriginalDamage(), new NativeDamageInput.Delta(NativeDamageInput.DeltaKind.valueOf(d.kind().name()),
                    d.postEnchantDamage(), d.eventOriginalDamage(), d.delta(), d.ratio(), d.reason()),
                s.offensiveMobEffectDelta(), s.projectileCriticalBonus(), s.projectileCritical());
        });
        var level = (ServerLevel)context.victim().level();
        float armor = 1, protection = 0;
        if (settings.executor(VanillaCompatibilitySettings.Reduction.ARMOR) == VanillaCompatibilitySettings.Executor.COMBATNEXUS
                && context.logicalAttacker() != null) {
            var weapon = context.projectileLaunch().map(io.github.naimjeg.combatnexus.combat.projectile.api.ProjectileLaunch::weapon)
                .orElseGet(() -> context.facts(context.logicalAttacker().getUUID()).weapon());
            if (!weapon.isEmpty()) armor = EnchantmentHelper.modifyArmorEffectiveness(level, weapon, context.victim(), context.source(), 1);
        }
        if (settings.executor(VanillaCompatibilitySettings.Reduction.ENCHANTMENTS) == VanillaCompatibilitySettings.Executor.COMBATNEXUS
                && !context.source().is(DamageTypeTags.BYPASSES_EFFECTS) && !context.source().is(DamageTypeTags.BYPASSES_ENCHANTMENTS))
            protection = EnchantmentHelper.getDamageProtection(level, context.victim(), context.source());
        return new NativeDamageInput(context.getInitialBaseAmount(), context.getInitialBaseBucket(),
            context.vanillaSourceProfile().projectile(), context.vanillaSourceProfile().shouldApplyMeleeOffensiveMobEffects(),
            context.shouldRebuildVanillaOffensiveMobEffects(), context.shouldRebuildVanillaOffensiveEnchantment(),
            context.shouldRebuildVanillaPreEventDelta(), context.getVanillaOffensiveMobEffectDelta(),
            context.getVanillaOffensiveMobEffectBucket(), context.getVanillaOffensiveEnchantmentBucket(), offense,
            owner(settings, VanillaCompatibilitySettings.Reduction.ARMOR), owner(settings, VanillaCompatibilitySettings.Reduction.ENCHANTMENTS),
            owner(settings, VanillaCompatibilitySettings.Reduction.MOB_EFFECTS), owner(settings, VanillaCompatibilitySettings.Reduction.INNATE_RESISTANCE),
            Float.isFinite(armor) ? Math.max(0, armor) : 1, Float.isFinite(protection) ? Math.max(0, protection) : 0,
            context.settings().values().formulas().ratingPerProtScore(), io.github.naimjeg.combatnexus.combat.damage.core.settlement.NativeDamageAdapter.takeAdjustments(context.victim(), context.source()));
    }
    private static NativeDamageInput.Reduction owner(VanillaCompatibilitySettings settings, VanillaCompatibilitySettings.Reduction reduction) {
        return NativeDamageInput.Reduction.valueOf(settings.executor(reduction).name());
    }
}
