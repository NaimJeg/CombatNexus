package io.github.naimjeg.combatnexus.presentation.api;

import io.github.naimjeg.combatnexus.effect.api.EffectTooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import java.util.*;
import java.util.function.BiFunction;

/** Describes carried effects using server-authored behavior text and native item parameters. */
public final class ItemPotionTooltipProjector {
    private ItemPotionTooltipProjector() {}
    public static List<TooltipBlock> project(ItemStack stack, float tickRate, boolean advanced,
            BiFunction<Identifier, Integer, Optional<TooltipBlock>> descriptions) {
        var display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        if (display.hideTooltip()) return List.of();
        var result = new ArrayList<TooltipBlock>();
        var budget = new PresentationBudget(4096, 24000);
        var potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion != null && display.shows(DataComponents.POTION_CONTENTS)) {
            float scale = stack.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0f);
            for (var effect : potion.getAllEffects())
                result.add(describe(effect, scale, tickRate, 1, false, descriptions, budget));
        }
        var consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable != null && display.shows(DataComponents.CONSUMABLE)) {
            for (var action : consumable.onConsumeEffects()) if (action instanceof ApplyStatusEffectsConsumeEffect apply) {
                for (var effect : apply.effects())
                    result.add(describe(effect, 1, tickRate, apply.probability(), true, descriptions, budget));
            }
        }
        // Preserve vanilla's deliberate concealment of suspicious stew in normal tooltips.
        var stew = stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (advanced && stew != null && display.shows(DataComponents.SUSPICIOUS_STEW_EFFECTS)) {
            for (var entry : stew.effects())
                result.add(describe(entry.createEffectInstance(), 1, tickRate, 1, false, descriptions, budget));
        }
        return List.copyOf(result);
    }
    private static TooltipBlock describe(MobEffectInstance instance, float scale, float tickRate, float probability,
            boolean attributes, BiFunction<Identifier, Integer, Optional<TooltipBlock>> descriptions, PresentationBudget budget) {
        budget.entry(1);
        var nativeLines = new ArrayList<Component>();
        PotionContents.addPotionTooltip(List.of(instance), nativeLines::add, scale, tickRate);
        var summary = new ArrayList<Component>();
        if (probability < 1) summary.add(Component.translatable("tooltip.combatnexus.item_effect.probability", TooltipNumbers.percent(probability)));
        var adopted = descriptions.apply(instance.getEffect().unwrapKey().orElseThrow().identifier(), instance.getAmplifier());
        if (adopted.isPresent()) summary.addAll(adopted.get().summary());
        else throw new IllegalArgumentException("Missing carried effect description");
        if (attributes) instance.getEffect().value().createModifiers(instance.getAmplifier(), (attribute, modifier) ->
            summary.add(AttributeDescription.describe(BuiltInRegistries.ATTRIBUTE.getKey(attribute.value()), modifier)));
        return new TooltipBlock(nativeLines.getFirst(), summary, adopted.map(TooltipBlock::details).orElse(List.of()));
    }
}
