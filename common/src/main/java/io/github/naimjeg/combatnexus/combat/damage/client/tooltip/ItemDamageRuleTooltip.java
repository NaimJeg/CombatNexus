package io.github.naimjeg.combatnexus.combat.damage.client.tooltip;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import io.github.naimjeg.combatnexus.combat.damage.api.item.template.DamageItemTemplateReferences;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import io.github.naimjeg.combatnexus.combat.damage.api.item.DamageComponents;
import net.minecraft.network.chat.Component;

/** Loaded exclusively from client event adapters. */
public final class ItemDamageRuleTooltip {
    private ItemDamageRuleTooltip() {}
    /** Component presence only; this does not calculate any effect or adopted value. */
    public static boolean hasDescription(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var display = stack.getOrDefault(net.minecraft.core.component.DataComponents.TOOLTIP_DISPLAY,
                net.minecraft.world.item.component.TooltipDisplay.DEFAULT);
        if (stack.isEmpty() || display.hideTooltip()) return false;
        if (!stack.getOrDefault(DamageComponents.entries(), List.of()).isEmpty()
                || !stack.getOrDefault(DamageComponents.affixes(), List.of()).isEmpty()) return true;
        var templates = stack.getOrDefault(DamageComponents.templates(), DamageItemTemplateReferences.EMPTY);
        if (!templates.entries().isEmpty() || !templates.affixes().isEmpty()) return true;
        var bindings = io.github.naimjeg.combatnexus.combat.api.behavior.EquipmentBindings.component();
        if (display.shows(bindings) && !stack.getOrDefault(bindings,
                io.github.naimjeg.combatnexus.combat.api.behavior.EquipmentBindings.EMPTY).bindings().isEmpty()) return true;
        var enchantments = net.minecraft.core.component.DataComponents.ENCHANTMENTS;
        if (display.shows(enchantments) && !stack.getOrDefault(enchantments, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).isEmpty()) return true;
        var potionType = net.minecraft.core.component.DataComponents.POTION_CONTENTS;
        var potion = stack.get(potionType);
        if (display.shows(potionType) && potion != null && potion.getAllEffects().iterator().hasNext()) return true;
        var consumableType = net.minecraft.core.component.DataComponents.CONSUMABLE;
        var consumable = stack.get(consumableType);
        if (display.shows(consumableType) && consumable != null && consumable.onConsumeEffects().stream()
                .anyMatch(effect -> effect instanceof net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect)) return true;
        var stewType = net.minecraft.core.component.DataComponents.SUSPICIOUS_STEW_EFFECTS;
        var stew = stack.get(stewType);
        return display.shows(stewType) && stew != null && !stew.effects().isEmpty();
    }
    public static List<Component> append(ItemStack stack, List<Component> tooltip, boolean shiftDown, boolean advanced,
            java.util.Optional<com.google.gson.JsonObject> projection) {
        if (projection == null || projection.isEmpty() || projection.get().has("error") || !hasDescription(stack)) return tooltip;
        var data = projection.get();
        for (var section : List.of("documents", "equipment", "carried", "stew"))
            if (!data.has(section) || !data.get(section).isJsonArray()) return tooltip;
        var rendered = new java.util.ArrayList<>(tooltip);
        try {
            appendProjection(stack, rendered, shiftDown, advanced, data);
        } catch (Exception failure) {
            return tooltip;
        }
        tooltip.clear();
        tooltip.addAll(rendered);
        return tooltip;
    }
    private static void appendProjection(ItemStack stack, List<Component> tooltip, boolean shiftDown, boolean advanced,
            com.google.gson.JsonObject data) {
        boolean expanded = shiftDown || isShiftDown();
        var mc = Minecraft.getInstance();
        if (!expanded && mc.level != null)
            io.github.naimjeg.combatnexus.presentation.client.EffectTooltipRenderer.collapsePotionAttributes(
                tooltip, stack, mc.level.tickRateManager().tickrate(), advanced);
        appendDocuments(tooltip, data.getAsJsonArray("documents"), expanded);
        appendDocuments(tooltip, data.getAsJsonArray("equipment"), expanded);
        if (expanded) {
            appendDocuments(tooltip, data.getAsJsonArray("carried"), true);
            if (advanced) appendDocuments(tooltip, data.getAsJsonArray("stew"), true);
        } else if (data.getAsJsonArray("carried").size() > 0 || advanced && data.getAsJsonArray("stew").size() > 0) {
            var hint = Component.translatable("tooltip.combatnexus.hold_shift");
            if (tooltip.stream().noneMatch(line -> line.getString().strip().equals(hint.getString())))
                tooltip.add(hint.withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }
    private static void appendDocuments(List<Component> tooltip, com.google.gson.JsonArray documents, boolean expanded) {
        for (var value : documents) {
            var document = io.github.naimjeg.combatnexus.presentation.api.DescriptionDocument.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, value).getOrThrow();
            var rendered = io.github.naimjeg.combatnexus.presentation.client.DescriptionRenderer.render(document, expanded);
            int anchor = -1;
            if (document.title().isPresent()) {
                var title = io.github.naimjeg.combatnexus.presentation.client.DisplayTextResolver.resolve(document.title().get()).getString();
                for (int i = 0; i < tooltip.size(); i++) if (tooltip.get(i).getString().equals(title)) { anchor = i; break; }
            }
            if (anchor >= 0) { if (!rendered.isEmpty()) rendered.removeFirst(); tooltip.addAll(anchor + 1, rendered); }
            else tooltip.addAll(rendered);
        }
    }

    private static boolean isShiftDown() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return false;
        }
        Window window = minecraft.getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
