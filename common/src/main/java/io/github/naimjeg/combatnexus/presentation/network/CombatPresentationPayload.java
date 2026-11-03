package io.github.naimjeg.combatnexus.presentation.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/** Bounded server projection; the JSON contains panel rows or resolved display definitions only. */
public record CombatPresentationPayload(UUID nonce, UUID session, Identifier dimension, long generation,
                                        int slot, ItemStack stack, String json) implements CustomPacketPayload {
    public static final int MAX_JSON = 32767;
    public static final Type<CombatPresentationPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("combatnexus", "presentation_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CombatPresentationPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUUID(value.nonce()); buf.writeUUID(value.session()); buf.writeIdentifier(value.dimension());
                buf.writeVarLong(value.generation()); buf.writeVarInt(value.slot());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, value.stack()); buf.writeUtf(value.json(), MAX_JSON);
            }, buf -> new CombatPresentationPayload(buf.readUUID(), buf.readUUID(), buf.readIdentifier(),
                    buf.readVarLong(), buf.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readUtf(MAX_JSON)));
    public CombatPresentationPayload {
        java.util.Objects.requireNonNull(nonce); java.util.Objects.requireNonNull(session);
        java.util.Objects.requireNonNull(dimension); java.util.Objects.requireNonNull(json);
        if (generation < 1 || slot < -2 || slot > 1023 || json.length() > MAX_JSON) throw new IllegalArgumentException("Invalid projection");
        stack = stack.copy();
    }
    public boolean fitsEncodingBudget(net.minecraft.core.RegistryAccess registries) {
        var buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(256, 65_536), registries);
        try { STREAM_CODEC.encode(buffer, this); return buffer.readableBytes() <= 65_536; }
        catch (RuntimeException failure) {
            return false;
        }
        finally { buffer.release(); }
    }
    @Override public ItemStack stack() { return stack.copy(); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
