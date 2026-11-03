package io.github.naimjeg.combatnexus.combat.damage.platform.network;

import io.github.naimjeg.combatnexus.combat.damage.network.payload.DamageDummyApplyAttributesPayload;
import io.github.naimjeg.combatnexus.combat.damage.network.payload.DamageDummyAttributesPayload;
import io.github.naimjeg.combatnexus.combat.damage.network.payload.DamageNumberPayload;
import io.github.naimjeg.combatnexus.combat.damage.network.payload.FormulaSettingsPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import io.github.naimjeg.combatnexus.CombatNexusCommon;

public final class DamageModuleNetwork {

    private DamageModuleNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // NeoForge requires a non-empty token; this is fixed identity, not a release version.
        event.registrar("combatnexus-description-3")
                .playToClient(FormulaSettingsPayload.TYPE,
                        FormulaSettingsPayload.STREAM_CODEC)
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
