package io.github.naimjeg.combatnexus.damage.network;

import io.github.naimjeg.combatnexus.damage.network.payload.DamageNumberPayload;
import io.github.naimjeg.combatnexus.damage.network.payload.DamageDummyApplyAttributesPayload;
import io.github.naimjeg.combatnexus.damage.network.payload.DamageDummyAttributesPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class DamageModuleNetwork {

    private DamageModuleNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("2")
                .playToClient(
                        DamageNumberPayload.TYPE,
                        DamageNumberPayload.STREAM_CODEC
                )
                .playToServer(
                        DamageDummyApplyAttributesPayload.TYPE,
                        DamageDummyApplyAttributesPayload.STREAM_CODEC,
                        DamageDummyAttributePayloadHandler::handleApply
                )
                .playToClient(
                        DamageDummyAttributesPayload.TYPE,
                        DamageDummyAttributesPayload.STREAM_CODEC
                );
    }
}
