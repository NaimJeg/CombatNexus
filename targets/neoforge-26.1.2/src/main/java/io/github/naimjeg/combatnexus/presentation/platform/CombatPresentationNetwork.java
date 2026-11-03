package io.github.naimjeg.combatnexus.presentation.platform;

import com.google.gson.*;
import io.github.naimjeg.combatnexus.presentation.network.*;
import io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

public final class CombatPresentationNetwork {
    private static final Map<ServerPlayer, long[]> REQUESTS = new WeakHashMap<>();
    private record PendingPanel(ServerLevel level, CombatPresentationRequest request) {}
    private static final Map<ServerPlayer, PendingPanel> PANELS = new IdentityHashMap<>();
    private CombatPresentationNetwork() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("combatnexus-description-4")
                .playToServer(CombatPresentationRequest.TYPE, CombatPresentationRequest.STREAM_CODEC, CombatPresentationNetwork::request)
                .playToClient(CombatPresentationPayload.TYPE, CombatPresentationPayload.STREAM_CODEC)
                .playToClient(CombatGenerationPayload.TYPE, CombatGenerationPayload.STREAM_CODEC)
                .playToClient(TooltipDefinitionPayload.TYPE, TooltipDefinitionPayload.STREAM_CODEC)
                .playToClient(EffectTooltipPayload.TYPE, EffectTooltipPayload.STREAM_CODEC)
                .playToClient(CombatControlPayload.TYPE, CombatControlPayload.STREAM_CODEC);
    }
    private static void request(CombatPresentationRequest request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || request.slot() != -1) return;
        var level = player.level();
        long now = level.getGameTime();
        int kind = request.slot() < 0 ? 0 : 1;
        var previous = REQUESTS.computeIfAbsent(player, ignored -> new long[]{Long.MIN_VALUE, Long.MIN_VALUE});
        if (previous[kind] != Long.MIN_VALUE && now >= previous[kind] && now - previous[kind] < 5) return;
        previous[kind] = now;
        if (request.slot() < 0) {
            // Packet handling can run after the world clock advances but before its lifecycle does.
            // Keep one subscription per player; capture only in the lifecycle's ready phase.
            PANELS.put(player, new PendingPanel(level, request));
        }
    }
    public static void flushControls(ServerLevel level) {
        var session = io.github.naimjeg.combatnexus.combat.api.CombatWorldSessions.current(level).orElseThrow();
        for (var player : level.players()) {
            int mask = 0;
            for (var restriction : io.github.naimjeg.combatnexus.combat.api.CombatWorldSessions.restrictions(level, player.getUUID())) mask |= 1 << restriction.ordinal();
            PacketDistributor.sendToPlayer(player, new CombatControlPayload(session, level.dimension().identifier(), player.getId(), level.getGameTime(), mask));
        }
    }
    public static void flushPanels(ServerLevel level) {
        if (level.getGameTime() % 10 != 0) return;
        for (var player : level.players()) {
            var pending = PANELS.get(player);
            if (pending != null && pending.level() == level && !player.hasDisconnected())
                respond(pending.request(), player);
        }
    }
    public static void clear(ServerPlayer player) {
        PANELS.remove(player);
        REQUESTS.remove(player);
    }
    public static void clear(ServerLevel level) {
        PANELS.values().removeIf(pending -> pending.level() == level);
    }
    private static void respond(CombatPresentationRequest request, ServerPlayer player) {
        var level = player.level();
        long now = level.getGameTime();
        long generation = DamageModuleApi.definitionGeneration(level.getServer());
        if (generation == 0 || generation != request.generation()) {
            if (request.slot() < 0) PANELS.remove(player);
            return;
        }
        if (player.containerMenu.containerId != request.containerId()
                || request.slot() >= player.containerMenu.slots.size()) {
            if (request.slot() < 0) PANELS.remove(player);
            return;
        }
        var session = io.github.naimjeg.combatnexus.combat.api.CombatWorldSessions.current(level).orElseThrow();
        var json = new JsonObject();
        ItemStack stack = ItemStack.EMPTY;
        if (request.slot() < 0) {
            var captureBudget = new io.github.naimjeg.combatnexus.presentation.api.PresentationBudget(256, 24000);
            var panel = io.github.naimjeg.combatnexus.presentation.api.CombatPanelRequests.request(player, captureBudget);
            if (panel.snapshot().isEmpty()) {
                json = io.github.naimjeg.combatnexus.presentation.api.CombatPanelProjector.refused(panel.refusal().name());
            } else {
            var value = panel.snapshot().get();
            try { json = io.github.naimjeg.combatnexus.presentation.api.CombatPanelProjector.project(value, now); }
            catch (io.github.naimjeg.combatnexus.presentation.api.PresentationBudget.Exceeded exceeded) { json = io.github.naimjeg.combatnexus.presentation.api.CombatPanelProjector.refused("BUDGET_EXCEEDED"); }
            }
        }
        String encoded = json.toString();
        if (encoded.length() > CombatPresentationPayload.MAX_JSON) encoded = "{\"error\":\"BUDGET_EXCEEDED\"}";
        var payload = new CombatPresentationPayload(request.nonce(), session,
                level.dimension().identifier(), generation, request.slot(), stack, encoded);
        if (!payload.fitsEncodingBudget(player.registryAccess())) {
            payload = new CombatPresentationPayload(request.nonce(), session,
                    level.dimension().identifier(), generation, request.slot(), ItemStack.EMPTY,
                    io.github.naimjeg.combatnexus.presentation.api.CombatPanelProjector.refused("ENCODING_FAILED").toString());
        }
        PacketDistributor.sendToPlayer(player, payload);
    }
}
