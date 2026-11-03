package io.github.naimjeg.combatnexus.presentation.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
/** Connection-wide adopted resource identity, independent of world presentation replies. */
public record CombatGenerationPayload(long generation) implements CustomPacketPayload {
    public static final Type<CombatGenerationPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("combatnexus", "definition_generation"));
    public static final StreamCodec<FriendlyByteBuf, CombatGenerationPayload> STREAM_CODEC = StreamCodec.of(
        (buf, value) -> buf.writeVarLong(value.generation()), buf -> new CombatGenerationPayload(buf.readVarLong()));
    public CombatGenerationPayload { if (generation < 1) throw new IllegalArgumentException("Invalid generation"); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
