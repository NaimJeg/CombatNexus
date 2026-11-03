package io.github.naimjeg.combatnexus.combat.damage.platform.client.screen;

import io.github.naimjeg.combatnexus.combat.damage.client.screen.DamageDummyScreen;

import io.github.naimjeg.combatnexus.combat.damage.platform.registry.ModMenuTypes;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Physical-client-only registration of container screens via
 * {@link RegisterMenuScreensEvent}. Referenced exclusively from
 * {@code ModClientHandler} (the {@code Dist.CLIENT} mod entry point), so this
 * class and {@link DamageDummyScreen} are never loaded on a dedicated
 * server.
 */
public final class ModMenuScreens {

    private ModMenuScreens() {
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.<io.github.naimjeg.combatnexus.combat.damage.menu.DamageDummyMenu, DamageDummyScreen>register(
                ModMenuTypes.DAMAGE_DUMMY.get(),
                (menu, inventory, title) -> new DamageDummyScreen(menu, inventory, title,
                        net.neoforged.neoforge.client.network.ClientPacketDistributor::sendToServer)
        );
    }
}
