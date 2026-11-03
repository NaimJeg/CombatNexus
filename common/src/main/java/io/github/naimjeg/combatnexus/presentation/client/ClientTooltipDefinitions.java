package io.github.naimjeg.combatnexus.presentation.client;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import io.github.naimjeg.combatnexus.combat.api.behavior.EquipmentBindings;
import io.github.naimjeg.combatnexus.presentation.api.DescriptionDocument;
import io.github.naimjeg.combatnexus.presentation.network.TooltipDefinitionPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.core.HolderLookup;
import java.util.*;

/** Connection-wide definitions. ItemStacks retain only their vanilla-synchronized components. */
public final class ClientTooltipDefinitions {
    private static long generation;
    private static final Map<Identifier, DescriptionDocument> ENTRIES = new HashMap<>();
    private static final Map<Identifier, DescriptionDocument> AFFIXES = new HashMap<>();
    private static final Map<Identifier, DescriptionDocument> EFFECTS = new HashMap<>(), BEHAVIORS = new HashMap<>();
    private static final Map<Identifier, DescriptionDocument> ITEM_EFFECTS = new HashMap<>();
    private ClientTooltipDefinitions() {}
    public static void begin(long next) {
        if (next <= generation) return;
        clear(); generation = next;
    }
    public static void accept(TooltipDefinitionPayload payload, HolderLookup.Provider registries) {
        if (payload.generation() != generation) return;
        var destination = switch (payload.kind()) { case ENTRY -> ENTRIES; case AFFIX -> AFFIXES; case EFFECT -> EFFECTS; case BEHAVIOR -> BEHAVIORS; case ITEM_EFFECT -> ITEM_EFFECTS; };
        destination.remove(payload.id());
        if (payload.json().isEmpty()) return;
        try {
            destination.put(payload.id(), DescriptionDocument.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(payload.json())).getOrThrow());
        } catch (Exception failure) {
        }
    }

    public static Optional<DescriptionDocument> entry(Identifier id) { return Optional.ofNullable(ENTRIES.get(id)); }
    public static Optional<DescriptionDocument> affix(Identifier id) { return Optional.ofNullable(AFFIXES.get(id)); }
    public static Optional<DescriptionDocument> effect(Identifier id, EquipmentBindings.Kind kind) {
        return Optional.ofNullable((kind == EquipmentBindings.Kind.EFFECT ? EFFECTS : BEHAVIORS).get(id));
    }
    public static long generation() { return generation; }
    public static Optional<DescriptionDocument> itemEffect(Identifier id) { return Optional.ofNullable(ITEM_EFFECTS.get(id)); }
    public static void clear() { generation = 0; ENTRIES.clear(); AFFIXES.clear(); EFFECTS.clear(); BEHAVIORS.clear(); ITEM_EFFECTS.clear(); }
}
