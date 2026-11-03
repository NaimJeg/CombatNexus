package io.github.naimjeg.combatnexus.presentation.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

/** Slot -1 requests the authenticated player's panel; no client values are accepted. */
public record CombatPresentationRequest(UUID nonce, int containerId, int slot, long generation) implements CustomPacketPayload {
    public static final Type<CombatPresentationRequest> TYPE = new Type<>(Identifier.fromNamespaceAndPath("combatnexus", "presentation_request"));
    public static final StreamCodec<FriendlyByteBuf, CombatPresentationRequest> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeUUID(value.nonce()); buf.writeVarInt(value.containerId()); buf.writeVarInt(value.slot()); buf.writeVarLong(value.generation()); },
            buf -> new CombatPresentationRequest(buf.readUUID(), buf.readVarInt(), buf.readVarInt(), buf.readVarLong()));
    public CombatPresentationRequest { java.util.Objects.requireNonNull(nonce); if (slot < -1 || slot > 1023) throw new IllegalArgumentException("Invalid slot"); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
