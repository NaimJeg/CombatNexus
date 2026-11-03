package io.github.naimjeg.combatnexus.combat.damage.platform.event;

import io.github.naimjeg.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla.VanillaCriticalCapture;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

@EventBusSubscriber(modid = DamageModule.MODID)
public final class VanillaCritHandler {
    private VanillaCritHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onVanillaCriticalHit(CriticalHitEvent event) {
        var attacker = event.getEntity();
        if (attacker.level().isClientSide()
                || !io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla.VanillaAttackTakeover.managed()) {
            clear();
            return;
        }
        if (event.isCriticalHit() && !attacker.level().isClientSide()) {
            VanillaCriticalCapture.capture(attacker.getId(), event.getTarget().getId(),
                    attacker.level().getGameTime(), event.getDamageMultiplier());
        } else {
            clear();
        }
        // The common damage pipeline reconstructs this bonus exactly once.
        event.setDamageMultiplier(1.0f);
    }

    public static void clear() { VanillaCriticalCapture.clear(); }
}
