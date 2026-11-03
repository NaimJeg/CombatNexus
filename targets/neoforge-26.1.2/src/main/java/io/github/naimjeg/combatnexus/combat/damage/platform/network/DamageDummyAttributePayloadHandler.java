package io.github.naimjeg.combatnexus.combat.damage.platform.network;

import io.github.naimjeg.combatnexus.combat.damage.network.DamageDummyAttributeEditor;

import io.github.naimjeg.combatnexus.combat.damage.network.payload.DamageDummyApplyAttributesPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Authenticated, main-thread transport adapter; editor owns menu and anchor authorization. */
public final class DamageDummyAttributePayloadHandler {
    private static final DamageDummyAttributeEditor EDITOR =
            new DamageDummyAttributeEditor((player, payload) -> PacketDistributor.sendToPlayer(player, payload));
    private DamageDummyAttributePayloadHandler() {}
    public static void handleApply(DamageDummyApplyAttributesPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) EDITOR.handleApply(payload, player);
    }
}
