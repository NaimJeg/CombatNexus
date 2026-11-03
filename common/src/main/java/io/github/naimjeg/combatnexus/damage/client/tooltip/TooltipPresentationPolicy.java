package io.github.naimjeg.combatnexus.damage.client.tooltip;

import io.github.naimjeg.combatnexus.damage.config.TooltipDebugLevel;
import java.util.Objects;

public record TooltipPresentationPolicy(
        TooltipDetailLevel detailLevel,
        TooltipDebugLevel debugLevel
) {
    public TooltipPresentationPolicy {
        Objects.requireNonNull(detailLevel, "detailLevel");
        Objects.requireNonNull(debugLevel, "debugLevel");
    }
}
