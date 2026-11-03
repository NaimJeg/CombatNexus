package io.github.naimjeg.combatnexus.combat.damage.platform.lifecycle;

import io.github.naimjeg.combatnexus.combat.damage.core.request.DamageAdmissionController;

import io.github.naimjeg.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Releases per-server tick state when its owning server stops. */
@EventBusSubscriber(modid = DamageModule.MODID)
final class DamageAdmissionLifecycle {

    private DamageAdmissionLifecycle() {
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        DamageAdmissionController.clearServer(event.getServer());
    }
}
