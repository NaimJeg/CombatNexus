package io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/** Thread-local vanilla attack capture; the platform owns event timing and multiplier suppression. */
public final class VanillaCriticalCapture {
    static final CaptureSlot<PendingVanillaCrit> PENDING_CRIT = new CaptureSlot<>();
    private VanillaCriticalCapture() {}

    public static void capture(int attackerId, int targetId, long gameTime, float multiplier) {
        if (!Float.isFinite(multiplier) || multiplier < 0.0f) multiplier = 1.0f;
        PENDING_CRIT.set(new PendingVanillaCrit(attackerId, targetId, gameTime, true, multiplier));
    }

    public static @Nullable PendingVanillaCrit consumePendingVanillaCritical(
            @Nullable LivingEntity attacker,
            LivingEntity victim,
            DamageSource source
    ) {
        PendingVanillaCrit pending = PENDING_CRIT.get();

        if (pending == null) {
            return null;
        }

        if (!pending.matches(attacker, victim, source)) {
            clear();
            return null;
        }

        clear();
        return pending;
    }

    public static void clear() {
        PENDING_CRIT.remove();
    }

    public record PendingVanillaCrit(
            int attackerId,
            int targetId,
            long gameTime,
            boolean effectiveCritical,
            float multiplier
    ) {
        private static boolean isVanillaPlayerAttackSource(DamageSource source) {
            return source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK);
        }

        private boolean matches(
                @Nullable LivingEntity attacker,
                LivingEntity victim,
                DamageSource source
        ) {
            if (!effectiveCritical) {
                return false;
            }

            if (attacker == null) {
                return false;
            }

            if (attackerId >= 0 && attacker.getId() != attackerId) {
                return false;
            }

            if (victim.getId() != targetId) {
                return false;
            }

            if (gameTime >= 0L && attacker.level().getGameTime() != gameTime) {
                return false;
            }

            if (source.getEntity() != attacker) {
                return false;
            }

            if (source.getDirectEntity() != attacker) {
                return false;
            }

            return isVanillaPlayerAttackSource(source);
        }
    }
}
