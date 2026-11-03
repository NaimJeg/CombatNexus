package io.github.naimjeg.combatnexus.damage.core.request;

import io.github.naimjeg.combatnexus.damage.DamageModule;
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
