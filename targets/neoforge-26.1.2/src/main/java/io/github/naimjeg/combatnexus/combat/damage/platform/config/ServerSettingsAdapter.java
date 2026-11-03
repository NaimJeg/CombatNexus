package io.github.naimjeg.combatnexus.combat.damage.platform.config;

import io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitionReloadListener;
import io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions;
import io.github.naimjeg.combatnexus.combat.damage.core.config.DamageSettingsStore;
import io.github.naimjeg.combatnexus.combat.damage.config.VanillaCompatibilitySettings;
import io.github.naimjeg.combatnexus.combat.damage.network.payload.FormulaSettingsPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.resource.ListenerKey;

/** Retained listeners belong to one reload; publish only once the server adopts those resources. */
@EventBusSubscriber(modid = "combatnexus")
public final class ServerSettingsAdapter {
    private static final ListenerKey<CombatDefinitionReloadListener> KEY = ListenerKey.create(
            Identifier.fromNamespaceAndPath("combatnexus", "settings"));
    private ServerSettingsAdapter() {}
    @SubscribeEvent public static void register(AddServerReloadListenersEvent event) {
        event.addRetainedListener(KEY, new CombatDefinitionReloadListener());
    }
    @SubscribeEvent public static void start(ServerAboutToStartEvent event) { commit(event.getServer()); }
    private static DamageSettingsStore.Snapshot commit(MinecraftServer server) {
        var candidate = server.getServerResources().managers().getListener(KEY).candidate();
        CombatDefinitions.publish(server, candidate);
        return DamageSettingsStore.current(server);
    }
    @SubscribeEvent public static void sync(OnDatapackSyncEvent event) {
        var server = event.getPlayerList().getServer();
        // A single-player sync is login, not resource adoption. It must never republish
        // the retained candidate or replace a newer adopted server generation.
        var snapshot = event.getPlayer() == null ? commit(server) : DamageSettingsStore.current(server);
        var formulas = snapshot.values().formulas();
        var compatibility = snapshot.values().vanillaCompatibility();
        var payload = new FormulaSettingsPayload(snapshot.revision(), formulas.asymptoticKValue(),
                formulas.resistanceKValue(), formulas.ratingPerProtScore(),
                compatibility.executor(VanillaCompatibilitySettings.Reduction.ARMOR)
                        == VanillaCompatibilitySettings.Executor.COMBATNEXUS,
                compatibility.executor(VanillaCompatibilitySettings.Reduction.ENCHANTMENTS)
                        == VanillaCompatibilitySettings.Executor.COMBATNEXUS);
        var generation = new io.github.naimjeg.combatnexus.presentation.network.CombatGenerationPayload(CombatDefinitions.forServer(server).generation());
        var tooltipDefinitions = TooltipDefinitionSync.capture(server);
        event.getRelevantPlayers().forEach(player -> {
            PacketDistributor.sendToPlayer(player, generation);
            io.github.naimjeg.combatnexus.effect.platform.NativeEffectBridge.synchronizeDescriptions(player, java.util.List.of());
            PacketDistributor.sendToPlayer(player, payload);
            tooltipDefinitions.forEach(definition -> PacketDistributor.sendToPlayer(player, definition));
        });
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { CombatDefinitions.clear(event.getServer()); }
}
