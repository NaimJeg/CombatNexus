package io.github.naimjeg.combatnexus.combat.api.action;

import io.github.naimjeg.combatnexus.combat.api.CombatWorldSessions;
import io.github.naimjeg.combatnexus.effect.api.EffectSnapshot.RestrictedAction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** Server action gate for native adapters and explicit third-party casting/movement integration. */
public final class CombatActionGate {
    private CombatActionGate() {}
    private static java.util.function.BiPredicate<LivingEntity, RestrictedAction> clientView = (entity, action) -> true;
    public static void clientView(java.util.function.BiPredicate<LivingEntity, RestrictedAction> view) { clientView = java.util.Objects.requireNonNull(view); }
    public static boolean allows(LivingEntity entity, RestrictedAction action) {
        java.util.Objects.requireNonNull(action);
        if (!(entity.level() instanceof ServerLevel level)) return clientView.test(entity, action);
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Action decisions require server thread");
        return !CombatWorldSessions.restrictions(level, entity.getUUID()).contains(action);
    }
}
