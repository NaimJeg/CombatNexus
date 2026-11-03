package io.github.naimjeg.combatnexus.combat.damage.platform.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions;
import io.github.naimjeg.combatnexus.effect.api.*;
import io.github.naimjeg.combatnexus.presentation.api.*;
import io.github.naimjeg.combatnexus.presentation.network.TooltipDefinitionPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import java.util.*;

/** Projects each adopted shared definition once per login/reload event, independent of any inventory. */
final class TooltipDefinitionSync {
    private TooltipDefinitionSync() {}
    static List<TooltipDefinitionPayload> capture(MinecraftServer server) {
        var definitions = CombatDefinitions.forServer(server);
        var result = new ArrayList<TooltipDefinitionPayload>();
        long generation = definitions.generation();
        var templates = definitions.damage().templates();
        templates.entries().forEach((id, value) -> result.add(encode(generation, TooltipDefinitionPayload.Kind.ENTRY, id, DescriptionDocument.CODEC, JsonOps.INSTANCE, () -> io.github.naimjeg.combatnexus.combat.damage.api.display.DamageDescriptions.project(List.of(value), List.of()).getFirst())));
        templates.affixes().forEach((id, value) -> result.add(encode(generation, TooltipDefinitionPayload.Kind.AFFIX, id, DescriptionDocument.CODEC, JsonOps.INSTANCE, () -> io.github.naimjeg.combatnexus.combat.damage.api.display.DamageDescriptions.project(List.of(), List.of(value)).getFirst())));
        var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        // Item descriptions are independent of the player's active effects and their amplifiers.
        net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.listElements().forEach(holder -> {
            var id = holder.key().identifier();
            if (!id.getNamespace().equals("minecraft")) return;
            var description = id.getPath().equals("resistance")
                ? net.minecraft.network.chat.Component.translatable("tooltip.combatnexus.item_effect.resistance_per_level",
                    TooltipNumbers.percent(io.github.naimjeg.combatnexus.formula.MultiplierFormula.resistanceEffectReduction(0)))
                : net.minecraft.network.chat.Component.translatable("tooltip.combatnexus.native_effect.minecraft." + id.getPath());
            result.add(encode(generation, TooltipDefinitionPayload.Kind.ITEM_EFFECT, id, DescriptionDocument.CODEC, ops,
                () -> DescriptionDocument.leaf(new TooltipBlock(holder.value().getDisplayName(), List.of(description), List.of()))));
        });
        definitions.domains().effects().forEach((id, value) -> result.add(encode(generation, TooltipDefinitionPayload.Kind.EFFECT, id, DescriptionDocument.CODEC, ops, () -> DescriptionDocument.leaf(EffectTooltip.describe(id, value)))));
        definitions.behaviors().forEach((id, value) -> result.add(encode(generation, TooltipDefinitionPayload.Kind.BEHAVIOR, id, DescriptionDocument.CODEC, ops,
                () -> DescriptionDocument.leaf(EffectTooltip.describe(id, new EffectDefinition(0, value.durationTicks(), EffectService.Stacking.REPLACE, EffectPolicy.DEFAULT, List.of(), value.plan(), Set.of()))))));
        return List.copyOf(result);
    }
    private static <T> TooltipDefinitionPayload encode(long generation, TooltipDefinitionPayload.Kind kind, Identifier id, Codec<T> codec, DynamicOps<JsonElement> ops, java.util.function.Supplier<T> value) {
        // A definition too large to display remains explicitly unavailable; never truncate its conditions.
        String json;
        try { json = codec.encodeStart(ops, value.get()).getOrThrow().toString(); }
        catch (Exception failure) {
            json = "";
        }
        if (json.length() > TooltipDefinitionPayload.MAX_JSON) json = "";
        return new TooltipDefinitionPayload(generation, kind, id, json);
    }
}
