package io.github.naimjeg.combatnexus.presentation.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** One shared definition per packet, sent at login/reload, never in response to hovering an item. */
public record TooltipDefinitionPayload(long generation, Kind kind, Identifier id, String json) implements CustomPacketPayload {
    public enum Kind { ENTRY, AFFIX, EFFECT, BEHAVIOR, ITEM_EFFECT }
    public static final int MAX_JSON = 24000;
    public static final Type<TooltipDefinitionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("combatnexus", "tooltip_definition"));
    public static final StreamCodec<FriendlyByteBuf, TooltipDefinitionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeVarLong(value.generation()); buf.writeEnum(value.kind()); buf.writeIdentifier(value.id()); buf.writeUtf(value.json(), MAX_JSON); },
            buf -> new TooltipDefinitionPayload(buf.readVarLong(), buf.readEnum(Kind.class), buf.readIdentifier(), buf.readUtf(MAX_JSON)));
    public TooltipDefinitionPayload {
        if (generation < 1 || json.length() > MAX_JSON) throw new IllegalArgumentException("Invalid tooltip definition payload");
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
