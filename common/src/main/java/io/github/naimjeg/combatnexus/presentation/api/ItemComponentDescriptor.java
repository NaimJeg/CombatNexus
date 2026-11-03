package io.github.naimjeg.combatnexus.presentation.api;

import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Startup-registered description of a typed native component; no gameplay execution. */
@FunctionalInterface
public interface ItemComponentDescriptor<T> {
    List<TooltipBlock> describe(ItemStack stack, T component);
}
