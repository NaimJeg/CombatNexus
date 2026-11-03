package io.github.naimjeg.combatnexus.damage.diagnostics.logging;

import io.github.naimjeg.combatnexus.damage.DamageModule;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@EventBusSubscriber(modid = DamageModule.MODID)
final class DamageModuleDiagnosticLifecycle {

    private DamageModuleDiagnosticLifecycle() {
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        DamageModuleDiagnosticState.clearAll();
    }
}
