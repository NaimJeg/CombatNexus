package io.github.naimjeg.combatnexus.presentation.internal;

import io.github.naimjeg.combatnexus.presentation.api.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.*;

public final class ItemComponentDescriptions {
    private ItemComponentDescriptions() {}
    public record Entry<T>(DataComponentType<T> component, ItemComponentDescriptor<T> descriptor) {
        public Entry { Objects.requireNonNull(component); Objects.requireNonNull(descriptor); }
        List<TooltipBlock> describe(ItemStack stack) {
            if (!stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).shows(component)) return List.of();
            T value = stack.get(component);
            return value == null ? List.of() : List.copyOf(descriptor.describe(stack.copy(), value));
        }
    }
    private static List<Entry<?>> entries = List.of();
    public static void install(Map<Identifier, Entry<?>> registered) { entries = List.copyOf(registered.values()); }
    public static List<TooltipBlock> project(ItemStack stack) {
        var result = new ArrayList<TooltipBlock>();
        for (var entry : entries) result.addAll(entry.describe(stack));
        return List.copyOf(result);
    }
}
