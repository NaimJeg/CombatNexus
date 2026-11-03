package io.github.naimjeg.combatnexus.presentation.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import java.util.*;

/** Explains the already synchronized native instance; never installs or ticks effects. */
public final class NativeEffectTooltip {
    private NativeEffectTooltip() {}
    public static List<Component> append(List<Component> tooltip, MobEffectInstance instance, float tickRate, boolean expanded) {
        var effect = instance.getEffect().value();
        var title = effect.getDisplayName().copy();
        if (instance.getAmplifier() >= 1 && instance.getAmplifier() <= 9)
            title.append(" ").append(Component.translatable("enchantment.level." + (instance.getAmplifier() + 1)));
        var duration = MobEffectUtil.formatDuration(instance, 1, tickRate);
        // Compact widgets supply these lines; wide widgets start with an empty tooltip.
        if (tooltip.stream().noneMatch(line -> line.getString().equals(title.getString()))) tooltip.add(title);
        if (tooltip.stream().noneMatch(line -> line.getString().equals(duration.getString()))) tooltip.add(duration);
        var id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        var adopted = ClientEffectTooltips.get(id);
        if (adopted.isPresent()) {
            var rendered = DescriptionRenderer.render(adopted.get(), expanded);
            if (adopted.get().title().isPresent() && !rendered.isEmpty()) rendered.removeFirst();
            tooltip.addAll(rendered);
        } else tooltip.add(Component.translatable("tooltip.combatnexus.presentation.unavailable").withStyle(net.minecraft.ChatFormatting.GRAY));
        return tooltip;
    }
}
