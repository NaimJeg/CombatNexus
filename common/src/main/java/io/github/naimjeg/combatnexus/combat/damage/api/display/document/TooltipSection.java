package io.github.naimjeg.combatnexus.combat.damage.api.display.document;

public sealed interface TooltipSection permits
        AffixTooltipView,
        EntryTooltipView,
        VanillaTooltipAugmentation {
}
