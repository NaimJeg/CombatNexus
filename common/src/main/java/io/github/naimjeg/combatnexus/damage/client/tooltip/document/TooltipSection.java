package io.github.naimjeg.combatnexus.damage.client.tooltip.document;

public sealed interface TooltipSection permits
        AffixTooltipView,
        EntryTooltipView,
        VanillaTooltipAugmentation,
        DebugTooltipSection {
}
