package io.github.naimjeg.combatnexus.presentation.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Adopted execution description for one projected effect. Empty JSON revokes it. */
public record EffectTooltipPayload(UUID target, UUID session, long generation, Identifier dimension, Identifier effect, String json) implements CustomPacketPayload {
    public static final Type<EffectTooltipPayload> TYPE = new Type<>(Identifier.parse("combatnexus:effect_tooltip"));
    public static final StreamCodec<FriendlyByteBuf, EffectTooltipPayload> STREAM_CODEC = StreamCodec.of(
        (buf, value) -> { buf.writeUUID(value.target()); buf.writeUUID(value.session()); buf.writeVarLong(value.generation()); buf.writeIdentifier(value.dimension()); buf.writeIdentifier(value.effect()); buf.writeUtf(value.json(), 24000); },
        buf -> new EffectTooltipPayload(buf.readUUID(), buf.readUUID(), buf.readVarLong(), buf.readIdentifier(), buf.readIdentifier(), buf.readUtf(24000)));
    public EffectTooltipPayload { if (generation < 1 || json.length() > 24000) throw new IllegalArgumentException("Effect tooltip exceeds packet budget"); }
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
