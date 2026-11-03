package io.github.naimjeg.combatnexus.presentation.client;

import io.github.naimjeg.combatnexus.presentation.api.TooltipBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Shared item/effect layout. Detail never replaces the authored summary. */
public final class EffectTooltipRenderer {
    private EffectTooltipRenderer() {}
    public static void collapsePotionAttributes(List<Component> tooltip, net.minecraft.world.item.ItemStack stack,
            float tickRate, boolean advanced) {
        var potion = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (potion != null) removeAttributeSection(tooltip, potion.getAllEffects(),
            stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_DURATION_SCALE, 1.0f), tickRate);
        var stew = stack.get(net.minecraft.core.component.DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (advanced && stew != null) removeAttributeSection(tooltip,
            stew.effects().stream().map(net.minecraft.world.item.component.SuspiciousStewEffects.Entry::createEffectInstance).toList(), 1, tickRate);
    }
    private static void removeAttributeSection(List<Component> tooltip, Iterable<net.minecraft.world.effect.MobEffectInstance> effects,
            float scale, float tickRate) {
        var nativeLines = new ArrayList<Component>();
        net.minecraft.world.item.alchemy.PotionContents.addPotionTooltip(effects, nativeLines::add, scale, tickRate);
        for (int i = 0; i < nativeLines.size(); i++) {
            if (nativeLines.get(i).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                    && text.getKey().equals("potion.whenDrank")) {
                var section = nativeLines.subList(Math.max(0, i - 1), nativeLines.size());
                int start = Collections.indexOfSubList(tooltip, section);
                if (start >= 0) tooltip.subList(start, start + section.size()).clear();
                return;
            }
        }
    }
    /** Carried potion/food explanations are entirely optional; preserve the native compact tooltip. */
    public static List<Component> appendDetails(List<Component> tooltip, List<TooltipBlock> blocks, boolean expanded) {
        if (expanded) return append(tooltip, blocks, true);
        var hint = Component.translatable("tooltip.combatnexus.hold_shift");
        if (!blocks.isEmpty() && tooltip.stream().noneMatch(line -> line.getString().strip().equals(hint.getString())))
            tooltip.add(hint.withStyle(ChatFormatting.DARK_GRAY));
        return tooltip;
    }
    public static List<Component> append(List<Component> tooltip, List<TooltipBlock> blocks, boolean expanded) {
        boolean hint = false;
        for (var block : blocks) {
            int anchor = -1;
            for (int i = 0; i < tooltip.size(); i++) if (tooltip.get(i).getString().equals(block.title().getString())) { anchor = i; break; }
            var lines = DescriptionRenderer.render(io.github.naimjeg.combatnexus.presentation.api.DescriptionDocument.leaf(block), expanded);
            if (anchor >= 0 && !lines.isEmpty()) lines.removeFirst();
            var hintText = Component.translatable("tooltip.combatnexus.hold_shift").getString();
            hint |= lines.removeIf(line -> line.getString().strip().equals(hintText));
            if (anchor < 0) tooltip.addAll(lines); else tooltip.addAll(anchor + 1, lines);
        }
        var hintLine = Component.translatable("tooltip.combatnexus.hold_shift");
        if (!expanded && hint && tooltip.stream().noneMatch(line -> line.getString().strip().equals(hintLine.getString())))
            tooltip.add(hintLine.withStyle(ChatFormatting.DARK_GRAY));
        return tooltip;
    }
}
