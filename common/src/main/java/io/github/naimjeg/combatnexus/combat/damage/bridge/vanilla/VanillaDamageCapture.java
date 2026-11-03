package io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class VanillaDamageCapture {
    private static final float EPSILON = 0.0001f;

    static final CaptureSlot<OffensiveEnchantFrame> OFFENSIVE_ENCHANT =
            new CaptureSlot<>();

    private VanillaDamageCapture() {
    }

    public static void captureModifyDamage(
            ServerLevel level,
            ItemStack weapon,
            Entity victim,
            DamageSource source,
            float inputDamage,
            float outputDamage
    ) {
        OffensiveEnchantFrame previous = OFFENSIVE_ENCHANT.get();

        float delta = outputDamage - inputDamage;

        OFFENSIVE_ENCHANT.set(new OffensiveEnchantFrame(
                source.getEntity(),
                victim,
                source,
                weapon.copy(),
                inputDamage,
                outputDamage,
                delta
        ));

        boolean forceLogProjectileWeapon =
                source.is(DamageTypes.TRIDENT);
    }

    public static @Nullable OffensiveSnapshot consumeOffensiveSnapshot(
            DamageSource source,
            LivingEntity victim,
            float eventOriginalDamage
    ) {
        OffensiveEnchantFrame frame = OFFENSIVE_ENCHANT.get();

        try {
            OffensiveSnapshot spearSnapshot =
                    consumeSpearSnapshot(
                            source,
                            victim,
                            eventOriginalDamage,
                            frame
                    );

            if (spearSnapshot != null) {
                return spearSnapshot;
            }

            OffensiveSnapshot projectileSnapshot =
                    consumeProjectileSnapshot(
                            source,
                            victim,
                            eventOriginalDamage
                    );

            if (projectileSnapshot != null) {
                return projectileSnapshot;
            }

            if (frame == null) {
                return null;
            }

            if (frame.source() != source || frame.victim() != victim) {
                return null;
            }

            PreEventDelta preEventDelta = classifyPreEventDelta(
                    source,
                    frame.attacker(),
                    victim,
                    frame.weapon(),
                    frame.outputDamage(),
                    eventOriginalDamage
            );

            float offensiveMobEffectDelta =
                    computeOffensiveMobEffectDelta(
                            source,
                            victim,
                            frame.attacker()
                    );

            return new OffensiveSnapshot(
                    frame.attacker(),
                    frame.victim(),
                    frame.source(),
                    frame.weapon(),
                    frame.inputDamage(),
                    frame.outputDamage(),
                    frame.enchantDelta(),
                    eventOriginalDamage,
                    preEventDelta,
                    offensiveMobEffectDelta
            );
        } finally {
            clear();
        }
    }

    private static @Nullable OffensiveSnapshot consumeSpearSnapshot(
            DamageSource source,
            LivingEntity victim,
            float eventOriginalDamage,
            @Nullable OffensiveEnchantFrame enchantFrame
    ) {
        Entity attacker = source.getEntity();

        if (attacker == null) {
            return null;
        }

        SpearDamageCapture.SpearFrame spearFrame =
                SpearDamageCapture.peekFor(attacker, victim);

        if (spearFrame == null) {
            return null;
        }

        /*
         * No kinetic bonus means there is nothing to rebuild.
         * Let ordinary enchant capture continue if one exists.
         */
        if (!spearFrame.hasSpeedBonus()) {
            return null;
        }

        boolean hasMatchingEnchantFrame =
                enchantFrame != null
                        && enchantFrame.source() == source
                        && enchantFrame.victim() == victim;

        // The kinetic frame alone has no DamageSource identity. Do not attach it
        // to an unrelated event from the same attacker/target pair.
        if (!hasMatchingEnchantFrame) return null;

        float enchantDelta =
                hasMatchingEnchantFrame
                        ? enchantFrame.enchantDelta()
                        : 0.0f;

        ItemStack weapon =
                hasMatchingEnchantFrame
                        ? enchantFrame.weapon()
                        : spearFrame.weapon();

        float rawBaseDamage = Math.max(0.0f, spearFrame.rawBaseDamage());
        float scaledDamage = Math.max(0.0f, spearFrame.scaledDamage());
        float speedBonusDamage = scaledDamage - rawBaseDamage;

        if (!Float.isFinite(speedBonusDamage)
                || Math.abs(speedBonusDamage) <= EPSILON) {
            return null;
        }

        PreEventDeltaKind kind = switch (spearFrame.mode()) {
            case STAB -> PreEventDeltaKind.SPEAR_STAB_BONUS;
            case CHARGE -> PreEventDeltaKind.SPEAR_CHARGE_BONUS;
        };

        float ratio =
                Math.abs(rawBaseDamage) > EPSILON
                        ? scaledDamage / rawBaseDamage
                        : 1.0f;

        PreEventDelta preEventDelta = new PreEventDelta(
                kind,
                rawBaseDamage,
                scaledDamage,
                speedBonusDamage,
                ratio,
                "spear mode=" + spearFrame.mode()
                        + " raw_base=" + rawBaseDamage
                        + " scaled=" + scaledDamage
                        + " speed_bonus=" + speedBonusDamage
                        + " relative_speed=" + spearFrame.relativeSpeed()
                        + " damage_multiplier=" + spearFrame.damageMultiplier()
                        + " ticks_used=" + spearFrame.ticksUsed()
                        + " deals_damage=" + spearFrame.dealsDamage()
                        + " deals_knockback=" + spearFrame.dealsKnockback()
                        + " dismounts=" + spearFrame.dismounts()
                        + " actual_event_original=" + eventOriginalDamage
                        + " enchant_delta=" + enchantDelta
        );

        float offensiveMobEffectDelta =
                computeOffensiveMobEffectDelta(
                        source,
                        victim,
                        attacker
                );

        return new OffensiveSnapshot(
                attacker,
                victim,
                source,
                weapon,

                /*
                 * Logical DN reconstruction base.
                 * This intentionally differs from vanilla's local pre-enchant
                 * value for spear, because KineticWeapon already added speed
                 * bonus before stabAttack.
                 */
                rawBaseDamage,
                rawBaseDamage + enchantDelta,

                enchantDelta,
                eventOriginalDamage,
                preEventDelta,
                offensiveMobEffectDelta
        );
    }

    private static @Nullable OffensiveSnapshot consumeProjectileSnapshot(
            DamageSource source,
            LivingEntity victim,
            float eventOriginalDamage
    ) {
        ProjectileDamageCapture.ProjectileFrame frame =
                ProjectileDamageCapture.peekFor(source, victim);

        if (frame == null) {
            return null;
        }

        float preEnchantDamage = frame.preEnchantDamage();
        float postEnchantDamage = frame.postEnchantDamage();
        float preCritDamage = frame.preCritDamage();

        float enchantDelta = postEnchantDamage - preEnchantDamage;

        PreEventDelta projectileScaling =
                makePreEventDelta(
                        PreEventDeltaKind.PROJECTILE_SCALING,
                        postEnchantDamage,
                        preCritDamage,
                        "projectile_scaling source=" + sourceRegistryId(source)
                                + " pre_enchant=" + preEnchantDamage
                                + " post_enchant=" + postEnchantDamage
                                + " pre_crit=" + preCritDamage
                                + " event_original=" + eventOriginalDamage
                );

        float criticalBonus =
                frame.critical()
                        ? Math.max(0.0f, frame.criticalBonus())
                        : 0.0f;

        float offensiveMobEffectDelta =
                computeOffensiveMobEffectDelta(
                        source,
                        victim,
                        source.getEntity()
                );

        return new OffensiveSnapshot(
                source.getEntity(),
                victim,
                source,
                frame.weapon(),
                preEnchantDamage,
                postEnchantDamage,
                enchantDelta,
                eventOriginalDamage,
                projectileScaling,
                offensiveMobEffectDelta,
                criticalBonus,
                frame.critical()
        );
    }

    private static float computeOffensiveMobEffectDelta(
            DamageSource source,
            LivingEntity victim,
            @Nullable Entity attacker
    ) {
        LivingEntity livingAttacker =
                attacker instanceof LivingEntity le ? le : null;

        VanillaDamageSourceProfile profile =
                VanillaDamageSourceProfile.create(
                        source,
                        livingAttacker,
                        victim
                );

        return VanillaMobEffectBridge
                .computeOffensiveBreakdown(profile)
                .enabledDelta();
    }

    private static PreEventDelta makePreEventDelta(
            PreEventDeltaKind kind,
            float from,
            float to,
            String reason
    ) {
        float delta = to - from;

        if (Math.abs(delta) <= EPSILON) {
            return PreEventDelta.none(from, to);
        }

        float ratio =
                Math.abs(from) > EPSILON
                        ? to / from
                        : 1.0f;

        return new PreEventDelta(
                kind,
                from,
                to,
                delta,
                ratio,
                reason
        );
    }

    public static void clear() {
        OFFENSIVE_ENCHANT.remove();
        MaceDamageCapture.clear();
        SpearDamageCapture.clear();
        ProjectileDamageCapture.clear();
    }

    private static PreEventDelta classifyPreEventDelta(
            DamageSource source,
            @Nullable Entity attacker,
            LivingEntity victim,
            ItemStack weapon,
            float postEnchantDamage,
            float eventOriginalDamage
    ) {
        float delta = eventOriginalDamage - postEnchantDamage;

        if (Math.abs(delta) <= EPSILON) {
            return PreEventDelta.none(postEnchantDamage, eventOriginalDamage);
        }

        float ratio = Math.abs(postEnchantDamage) > EPSILON
                ? eventOriginalDamage / postEnchantDamage
                : 1.0f;

        String sourceId = sourceRegistryId(source);

        Entity sourceAttacker = source.getEntity();

        if (victim instanceof Player && sourceAttacker instanceof Mob && source.scalesWithDifficulty()) {
            Difficulty difficulty = victim.level().getDifficulty();

            float expected = switch (difficulty) {
                case PEACEFUL -> 0.0f;
                case EASY -> Math.min(postEnchantDamage, postEnchantDamage * 0.5f + 1.0f);
                case NORMAL -> postEnchantDamage;
                case HARD -> postEnchantDamage * 1.5f;
            };

            if (Math.abs(expected - eventOriginalDamage) <= EPSILON) {
                return new PreEventDelta(
                        PreEventDeltaKind.DIFFICULTY_SCALING,
                        postEnchantDamage,
                        eventOriginalDamage,
                        delta,
                        ratio,
                        "mob_damage_to_player difficulty=" + difficulty.getSerializedName()
                );
            }
        }

        MaceDamageCapture.MaceFrame maceFrame =
                MaceDamageCapture.peekFor(source, victim);

        if (maceFrame != null) {
            return new PreEventDelta(
                    PreEventDeltaKind.SPECIAL_ATTACK_SCALING,
                    postEnchantDamage,
                    eventOriginalDamage,
                    delta,
                    ratio,
                    "mace_smash"
                            + " fall_distance=" + maceFrame.fallDistance()
                            + " returned_bonus=" + maceFrame.returnedBonus()
            );
        }

        if (source.is(DamageTypes.MACE_SMASH)) {
            return new PreEventDelta(
                    PreEventDeltaKind.SPECIAL_ATTACK_SCALING,
                    postEnchantDamage,
                    eventOriginalDamage,
                    delta,
                    ratio,
                    "special_attack source=" + sourceId
            );
        }

        if (source.is(DamageTypes.SPEAR) && isKineticWeapon(weapon)) {
            // A generic spear type cannot distinguish stab/charge without the dedicated capture.
            return new PreEventDelta(
                    PreEventDeltaKind.SPEAR_ATTACK_BONUS,
                    postEnchantDamage, eventOriginalDamage, delta, ratio,
                    "spear_attack source=" + sourceId
            );
        }

        if (attacker instanceof Player && source.is(DamageTypes.PLAYER_ATTACK)) {
            if (ratio <= 1.0f + EPSILON) {
                return new PreEventDelta(
                        PreEventDeltaKind.PLAYER_ATTACK_SCALING,
                        postEnchantDamage,
                        eventOriginalDamage,
                        delta,
                        ratio,
                        "player_attack_scaling source=" + sourceId
                );
            }

            return new PreEventDelta(
                    PreEventDeltaKind.UNKNOWN,
                    postEnchantDamage,
                    eventOriginalDamage,
                    delta,
                    ratio,
                    "player_attack_positive_delta_unclassified source=" + sourceId
            );
        }

        if (source.is(DamageTypes.ARROW) || source.is(DamageTypes.TRIDENT)) {
            return new PreEventDelta(
                    PreEventDeltaKind.PROJECTILE_SCALING,
                    postEnchantDamage,
                    eventOriginalDamage,
                    delta,
                    ratio,
                    "projectile_scaling source=" + sourceId
            );
        }

        return new PreEventDelta(
                PreEventDeltaKind.UNKNOWN,
                postEnchantDamage,
                eventOriginalDamage,
                delta,
                ratio,
                "unclassified source=" + sourceId
        );
    }

    private static boolean isKineticWeapon(ItemStack stack) {
        return !stack.isEmpty()
                && stack.has(DataComponents.KINETIC_WEAPON);
    }

    private static String sourceRegistryId(DamageSource source) {
        return source.typeHolder()
                .unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse(source.type().msgId());
    }

    private static String sourceId(DamageSource source) {
        return source.typeHolder()
                .unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse(source.type().msgId());
    }

    public record OffensiveEnchantFrame(
            @Nullable Entity attacker,
            Entity victim,
            DamageSource source,
            ItemStack weapon,
            float inputDamage,
            float outputDamage,
            float enchantDelta
    ) {
    }

    public record OffensiveSnapshot(
            @Nullable Entity attacker,
            Entity victim,
            DamageSource source,
            ItemStack weapon,
            float preEnchantDamage,
            float postEnchantDamage,
            float enchantDelta,
            float eventOriginalDamage,
            PreEventDelta preEventDelta,
            float offensiveMobEffectDelta,
            float projectileCriticalBonus,
            boolean projectileCritical
    ) {
        public OffensiveSnapshot(
                @Nullable Entity attacker,
                Entity victim,
                DamageSource source,
                ItemStack weapon,
                float preEnchantDamage,
                float postEnchantDamage,
                float enchantDelta,
                float eventOriginalDamage,
                PreEventDelta preEventDelta,
                float offensiveMobEffectDelta,
                float projectileCriticalBonus
        ) {
            this(attacker, victim, source, weapon, preEnchantDamage,
                    postEnchantDamage, enchantDelta, eventOriginalDamage,
                    preEventDelta, offensiveMobEffectDelta,
                    projectileCriticalBonus, projectileCriticalBonus > EPSILON);
        }

        public OffensiveSnapshot(
                @Nullable Entity attacker,
                Entity victim,
                DamageSource source,
                ItemStack weapon,
                float preEnchantDamage,
                float postEnchantDamage,
                float enchantDelta,
                float eventOriginalDamage,
                PreEventDelta preEventDelta,
                float offensiveMobEffectDelta
        ) {
            this(
                    attacker,
                    victim,
                    source,
                    weapon,
                    preEnchantDamage,
                    postEnchantDamage,
                    enchantDelta,
                    eventOriginalDamage,
                    preEventDelta,
                    offensiveMobEffectDelta,
                    0.0f,
                    false
            );
        }

        public boolean hasEnchantDelta() {
            return Math.abs(enchantDelta) > EPSILON;
        }

        public boolean hasPreEventDelta() {
            return preEventDelta.kind() != PreEventDeltaKind.NONE
                    || projectileCritical;
        }

        public boolean hasOffensiveMobEffectDelta() {
            return Math.abs(offensiveMobEffectDelta) > EPSILON;
        }

        public boolean hasProjectileCriticalBonus() {
            return projectileCriticalBonus > EPSILON;
        }
    }

    public record PreEventDelta(
            PreEventDeltaKind kind,
            float postEnchantDamage,
            float eventOriginalDamage,
            float delta,
            float ratio,
            String reason
    ) {
        public static PreEventDelta none(float postEnchantDamage, float eventOriginalDamage) {
            return new PreEventDelta(
                    PreEventDeltaKind.NONE,
                    postEnchantDamage,
                    eventOriginalDamage,
                    0.0f,
                    1.0f,
                    "none"
            );
        }
    }
}

