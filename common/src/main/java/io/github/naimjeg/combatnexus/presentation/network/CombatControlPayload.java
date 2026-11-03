package io.github.naimjeg.combatnexus.presentation.network;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
/** Server-owned input restrictions for one player/world; never a client combat decision. */
public record CombatControlPayload(UUID session, Identifier dimension, int entityId, long tick, int mask) implements CustomPacketPayload {
    public CombatControlPayload { if (tick < 0 || mask < 0 || mask > 255) throw new IllegalArgumentException("Invalid control state"); }
    public static final Type<CombatControlPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("combatnexus", "control_state"));
    public static final StreamCodec<FriendlyByteBuf, CombatControlPayload> STREAM_CODEC = StreamCodec.of(
        (buf, v) -> { buf.writeUUID(v.session()); buf.writeIdentifier(v.dimension()); buf.writeVarInt(v.entityId()); buf.writeVarLong(v.tick()); buf.writeByte(v.mask()); },
        buf -> new CombatControlPayload(buf.readUUID(), buf.readIdentifier(), buf.readVarInt(), buf.readVarLong(), buf.readUnsignedByte()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
