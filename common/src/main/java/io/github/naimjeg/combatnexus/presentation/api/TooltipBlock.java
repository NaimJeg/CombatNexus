package io.github.naimjeg.combatnexus.presentation.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import java.util.List;

/** Detached, server-authored explanation. Details supplement the summary. */
public record TooltipBlock(Component title, List<Component> summary, List<Component> details) {
    public static final Codec<TooltipBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("title").forGetter(TooltipBlock::title),
            ComponentSerialization.CODEC.listOf(0, 128).fieldOf("summary").forGetter(TooltipBlock::summary),
            ComponentSerialization.CODEC.listOf(0, 128).fieldOf("details").forGetter(TooltipBlock::details)
    ).apply(instance, TooltipBlock::new));
    public TooltipBlock {
        title = title.copy();
        summary = summary.stream().map(Component::copy).map(Component.class::cast).toList();
        details = details.stream().map(Component::copy).map(Component.class::cast).toList();
    }
}
