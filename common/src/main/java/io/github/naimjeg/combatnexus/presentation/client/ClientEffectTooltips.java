package io.github.naimjeg.combatnexus.presentation.client;

import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.core.HolderLookup;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import io.github.naimjeg.combatnexus.presentation.api.DescriptionDocument;
import io.github.naimjeg.combatnexus.presentation.network.EffectTooltipPayload;

public final class ClientEffectTooltips {
    private static final Map<Identifier, DescriptionDocument> ACTIVE = new HashMap<>();
    private ClientEffectTooltips() {}
    public static void clear() { ACTIVE.clear(); }
    public static Optional<DescriptionDocument> get(Identifier effect) { return Optional.ofNullable(ACTIVE.get(effect)); }
    public static void accept(EffectTooltipPayload payload, HolderLookup.Provider registries) {
        if (payload.json().isEmpty()) { ACTIVE.remove(payload.effect()); return; }
        ACTIVE.remove(payload.effect());
        try { ACTIVE.put(payload.effect(), DescriptionDocument.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(payload.json())).getOrThrow()); }
        catch (Exception failure) {}
    }
}
