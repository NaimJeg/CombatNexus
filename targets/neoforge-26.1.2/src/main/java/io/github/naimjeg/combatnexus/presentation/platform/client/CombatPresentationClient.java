package io.github.naimjeg.combatnexus.presentation.platform.client;

import com.google.gson.*;
import io.github.naimjeg.combatnexus.presentation.network.*;
import io.github.naimjeg.combatnexus.presentation.client.ClientTooltipDefinitions;
import io.github.naimjeg.combatnexus.presentation.client.ItemDescriptionProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import java.util.*;

@EventBusSubscriber(modid = "combatnexus", value = Dist.CLIENT)
public final class CombatPresentationClient {
    private static final KeyMapping PANEL = new KeyMapping("key.combatnexus.panel", 67, KeyMapping.Category.INVENTORY);
    private record Pending(UUID nonce, ClientLevel level, int containerId, int slot, ItemStack stack, long sentAt, long generation) {}
    private static Pending panel;
    // Detached server projection, used only by the display. Never an input to combat calculation.
    private static JsonArray panelRows;
    private static ClientLevel world;
    private static net.minecraft.client.player.LocalPlayer player;
    private static UUID session;
    private static long generation;
    private CombatPresentationClient() {}
    public static void keys(RegisterKeyMappingsEvent event) { event.register(PANEL); }
    public static void register(RegisterClientPayloadHandlersEvent event) {
        CombatControlClient.register();
        event.register(CombatControlPayload.TYPE, (payload, context) -> CombatControlClient.accept(payload));
        event.register(CombatGenerationPayload.TYPE, (payload, context) -> adoptGeneration(payload.generation()));
        event.register(CombatPresentationPayload.TYPE, (payload, context) -> accept(payload));
        event.register(TooltipDefinitionPayload.TYPE, (payload, context) -> ClientTooltipDefinitions.accept(payload, context.player().registryAccess()));
        event.register(EffectTooltipPayload.TYPE, (payload, context) -> {
            reconcile();
            if (context.player().getUUID().equals(payload.target()) && context.player().level().dimension().identifier().equals(payload.dimension())
                    && payload.generation() == generation && (session == null || session.equals(payload.session()))) {
                session = payload.session();
                io.github.naimjeg.combatnexus.presentation.client.ClientEffectTooltips.accept(payload, context.player().registryAccess());
            }
        });
    }
    private static void reconcile() {
        var mc = Minecraft.getInstance();
        if (world != mc.level || player != mc.player) {
            clearWorld(); world = mc.level; player = mc.player;
        }
    }
    private static void clearWorld() {
        io.github.naimjeg.combatnexus.presentation.client.ClientEffectTooltips.clear();
        panel = null; world = null; player = null; session = null;
        panelRows = null;
        if (Minecraft.getInstance().screen instanceof CombatPanelScreen screen) screen.invalidate();
    }
    private static void clear() { clearWorld(); generation = 0; ClientTooltipDefinitions.clear(); }
    private static void adoptGeneration(long next) {
        if (next <= generation) return;
        generation = next;
        ClientTooltipDefinitions.begin(next);
        io.github.naimjeg.combatnexus.presentation.client.ClientEffectTooltips.clear();
        panel = null;
        panelRows = null;
        if (Minecraft.getInstance().screen instanceof CombatPanelScreen screen) screen.invalidate();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        reconcile();
        var mc = Minecraft.getInstance();
        while (PANEL.consumeClick()) {
            if (mc.player != null && mc.level != null && mc.screen == null) {
                mc.setScreen(new CombatPanelScreen());
                if (panelRows != null) ((CombatPanelScreen) mc.screen).accept(panelRows);
            }
        }
        long now = System.nanoTime();
        if (panel != null && (now - panel.sentAt() > 2_000_000_000L
                || mc.player == null || panel.containerId() != synchronizedMenu(mc.player).containerId)) panel = null;
        if (panel == null) requestPanel();
    }
    /** Creative's picker is client-only; its slot numbers must never be sent as inventory slots. */
    private static net.minecraft.world.inventory.AbstractContainerMenu synchronizedMenu(net.minecraft.client.player.LocalPlayer player) {
        return player.containerMenu instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu
                ? player.inventoryMenu : player.containerMenu;
    }
    public static Optional<JsonObject> item(ItemStack stack) {
        var mc = Minecraft.getInstance();
        if (stack == null || stack.isEmpty() || mc.player == null || mc.level == null) return Optional.empty();
        try { return Optional.of(ItemDescriptionProjection.capture(mc.player, stack)); }
        catch (Exception failure) {
            return Optional.empty();
        }
    }
    private static void requestPanel() {
        var mc = Minecraft.getInstance();
        if (generation == 0 || mc.player == null || mc.level == null) return;
        panel = new Pending(UUID.randomUUID(), mc.level, synchronizedMenu(mc.player).containerId, -1, ItemStack.EMPTY, System.nanoTime(), generation);
        ClientPacketDistributor.sendToServer(new CombatPresentationRequest(panel.nonce(), panel.containerId(), -1, generation));
    }
    private static void accept(CombatPresentationPayload payload) {
        reconcile();
        var mc = Minecraft.getInstance();
        Pending pending = panel;
        if (pending == null || !pending.nonce().equals(payload.nonce()) || pending.level() != mc.level
                || mc.player == null || !mc.level.dimension().identifier().equals(payload.dimension())
                || pending.slot() != payload.slot() || pending.containerId() != synchronizedMenu(mc.player).containerId
                || pending.generation() != generation || payload.generation() != generation) return;
        if (session != null && !session.equals(payload.session())) return;
        session = payload.session();
        JsonObject data = JsonParser.parseString(payload.json()).getAsJsonObject();
            panel = new Pending(pending.nonce(), pending.level(), pending.containerId(), -1,
                    ItemStack.EMPTY, System.nanoTime(), pending.generation());
            panelRows = data.getAsJsonArray("rows").deepCopy();
            if (mc.screen instanceof CombatPanelScreen screen) screen.accept(panelRows);
    }
}
