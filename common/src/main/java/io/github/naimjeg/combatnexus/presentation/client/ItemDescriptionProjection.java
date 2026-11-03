package io.github.naimjeg.combatnexus.presentation.client;

import io.github.naimjeg.combatnexus.combat.damage.api.item.*;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.entry.DamageEntrySelectionResolver;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.affix.DamageAffixSelectionResolver;
import io.github.naimjeg.combatnexus.combat.damage.client.ClientFormulaSettings;

import com.google.gson.*;
import io.github.naimjeg.combatnexus.presentation.api.*;
import io.github.naimjeg.combatnexus.combat.damage.api.display.ItemPresentationProjector;
import io.github.naimjeg.combatnexus.combat.api.behavior.EquipmentBindings;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import java.util.*;

public final class ItemDescriptionProjection {
    private ItemDescriptionProjection() {}
    public static JsonObject capture(Player player, ItemStack stack) {
        stack = stack.copy();
        var display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, net.minecraft.world.item.component.TooltipDisplay.DEFAULT);
        if (!display.shows(DataComponents.ENCHANTMENTS)) stack.remove(DataComponents.ENCHANTMENTS);
        if (!display.shows(EquipmentBindings.component())) stack.remove(EquipmentBindings.component());
        var result = ItemPresentationProjector.project(new ItemCombatPresentationSnapshot(0,
            DamageEntrySelectionResolver.resolve(stack.getOrDefault(DamageComponents.entries(), List.of())),
            DamageAffixSelectionResolver.resolve(stack.getOrDefault(DamageComponents.affixes(), List.of()))));
        var references = stack.getOrDefault(DamageComponents.templates(),
            io.github.naimjeg.combatnexus.combat.damage.api.item.template.DamageItemTemplateReferences.EMPTY);
        var documents = result.getAsJsonArray("documents");
        for (var reference : references.entries()) appendTemplate(documents, ClientTooltipDefinitions.entry(reference.id()));
        for (var reference : references.affixes()) appendTemplate(documents, ClientTooltipDefinitions.affix(reference.id()));
        var settings = ClientFormulaSettings.current();
        var equipment = ItemEffectTooltipProjector.project(player.registryAccess(), (id, kind) ->
            Optional.of(block(ClientTooltipDefinitions.effect(id, kind)
                .orElseThrow(() -> new IllegalArgumentException("Missing equipment description: " + id)))), stack,
            settings.map(s -> s.managedEnchantments()).orElse(false),
            settings.map(s -> (double) s.ratingPerProtectionScore()).orElse(0.0));
        var carriedStack = stack.copy(); carriedStack.remove(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        var stewStack = stack.copy(); stewStack.remove(DataComponents.POTION_CONTENTS); stewStack.remove(DataComponents.CONSUMABLE);
        java.util.function.BiFunction<net.minecraft.resources.Identifier, Integer, Optional<TooltipBlock>> nativeDescription = (id, amplifier) -> {
            var holder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(id);
            if (holder.isEmpty() || !id.getNamespace().equals("minecraft"))
                throw new IllegalArgumentException("Missing carried effect description: " + id);
            List<Component> description = id.getPath().equals("resistance")
                ? List.of(Component.translatable("tooltip.combatnexus.item_effect.resistance_per_level", TooltipNumbers.percent(io.github.naimjeg.combatnexus.formula.MultiplierFormula.resistanceEffectReduction(0))))
                : io.github.naimjeg.combatnexus.effect.api.EffectTooltip.capability(id, amplifier);
            return Optional.of(new TooltipBlock(holder.get().value().getDisplayName(), description, List.of()));
        };
        var budget = new PresentationBudget(4096, 24000);
        var ops = new BudgetJsonOps(budget);
        result.add("equipment", encode(equipment, ops));
        result.add("carried", encode(ItemPotionTooltipProjector.project(carriedStack, player.level().tickRateManager().tickrate(), false, nativeDescription), ops));
        result.add("stew", encode(ItemPotionTooltipProjector.project(stewStack, player.level().tickRateManager().tickrate(), true, nativeDescription), ops));
        return result;
    }
    private static void appendTemplate(JsonArray documents, Optional<DescriptionDocument> document) {
        documents.add(DescriptionDocument.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,
            document.orElseThrow(() -> new IllegalArgumentException("Missing item description template"))).getOrThrow());
    }
    private static TooltipBlock block(DescriptionDocument document) {
        return new TooltipBlock(document.title().map(DisplayTextResolver::resolve).orElse(Component.empty()),
            (document.authoredSummary().isEmpty() ? document.generated() : document.authoredSummary()).stream().map(DisplayTextResolver::resolve).toList(),
            document.details().stream().map(DisplayTextResolver::resolve).toList());
    }
    private static JsonArray encode(List<TooltipBlock> blocks, BudgetJsonOps ops) {
        var result = new JsonArray();
        for (var block : blocks) result.add(DescriptionDocument.CODEC.encodeStart(ops, DescriptionDocument.leaf(block)).getOrThrow());
        return result;
    }
}
