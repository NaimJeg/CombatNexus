package io.github.naimjeg.combatnexus.presentation.platform.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.naimjeg.combatnexus.presentation.client.NativeEffectTooltip;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.GatherEffectScreenTooltipsEvent;
import org.lwjgl.glfw.GLFW;

/** Uses the native inventory effect widget in both compact and wide layouts. */
@EventBusSubscriber(modid = "combatnexus", value = Dist.CLIENT)
public final class NativeEffectTooltipHandler {
    private NativeEffectTooltipHandler() {}
    @SubscribeEvent public static void tooltip(GatherEffectScreenTooltipsEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        boolean expanded = InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
        NativeEffectTooltip.append(event.getTooltip(), event.getEffectInstance(), mc.level.tickRateManager().tickrate(), expanded);
    }
}
