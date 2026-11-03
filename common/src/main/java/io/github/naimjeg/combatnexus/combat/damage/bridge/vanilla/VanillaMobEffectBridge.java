package io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla;
import io.github.naimjeg.combatnexus.effect.api.VanillaEffectContributions;
import io.github.naimjeg.combatnexus.effect.api.VanillaEffectContributions.OffensiveMobEffectBreakdown;

public final class VanillaMobEffectBridge {
    private VanillaMobEffectBridge() {}
    public static OffensiveMobEffectBreakdown computeOffensiveBreakdown(VanillaDamageSourceProfile profile) {
        if (profile == null || profile.livingAttacker() == null || !profile.shouldApplyMeleeOffensiveMobEffects()) {
            return OffensiveMobEffectBreakdown.NONE;
        }
        return VanillaEffectContributions.computeBreakdownForLivingAttacker(profile.livingAttacker());
    }
}
