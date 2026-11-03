package io.github.naimjeg.combatnexus.combat.damage.api.context;

import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import java.util.*;

/** Frozen native calculation facts. Contains no entities, mutable stacks, runtime, or write authority. */
public record NativeDamageInput(float initialBaseAmount, DamageApplicationBucket initialBaseBucket,
        boolean projectile, boolean meleeEffects, boolean rebuildMobEffects, boolean rebuildEnchantments,
        boolean rebuildPreEvent, float mobEffectDelta, DamageApplicationBucket mobEffectBucket,
        DamageApplicationBucket enchantmentBucket, Optional<Offense> offense,
        Reduction armor, Reduction enchantments, Reduction effects, Reduction innate,
        float armorEffectiveness, float protectionScore, float ratingPerProtectionScore,
        List<Adjustment> priorAdjustments) {
    public NativeDamageInput {
        Objects.requireNonNull(initialBaseBucket); Objects.requireNonNull(mobEffectBucket);
        Objects.requireNonNull(enchantmentBucket); Objects.requireNonNull(offense);
        Objects.requireNonNull(armor); Objects.requireNonNull(enchantments); Objects.requireNonNull(effects); Objects.requireNonNull(innate);
        priorAdjustments = List.copyOf(priorAdjustments);
    }
    public enum Reduction { VANILLA, COMBATNEXUS, DISABLED }
    public enum DeltaKind { NONE, DIFFICULTY_SCALING, SPECIAL_ATTACK_SCALING, PLAYER_ATTACK_SCALING,
        PROJECTILE_SCALING, SPEAR_STAB_BONUS, SPEAR_CHARGE_BONUS, SPEAR_ATTACK_BONUS, UNKNOWN }
    public record Delta(DeltaKind kind, float before, float after, float delta, float ratio, String reason) {}
    public record Offense(float preEnchantDamage, float postEnchantDamage, float enchantDelta,
            float eventOriginalDamage, Delta preEventDelta, float offensiveMobEffectDelta,
            float projectileCriticalBonus, boolean projectileCritical) {}
    /** Already applied by the native caller; descriptive only, never an instruction to apply again. */
    public record Adjustment(net.minecraft.resources.Identifier source, float before, float after) {}
}
