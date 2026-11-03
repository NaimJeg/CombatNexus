package io.github.naimjeg.combatnexus.presentation.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;

/** Authenticated self-panel capture from an existing world; never advances lifecycle. */
public final class CombatPanelRequests {
    private CombatPanelRequests() {}
    public static CombatPanelService.Result request(ServerPlayer player, PresentationBudget budget) {
        var level = player.level();
        var runtime = io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(level);
        return runtime.panels().request(player.getUUID(), player.getUUID(), level.getGameTime(), () -> {
            var attributes = runtime.captureAttributes(player, BuiltInRegistries.ATTRIBUTE.listElements()
                .<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>>map(holder -> holder).toList(), budget::entry);
            return runtime.panel(player, attributes, budget::entry);
        });
    }
}
