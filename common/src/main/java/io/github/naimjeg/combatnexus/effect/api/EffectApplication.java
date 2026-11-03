package io.github.naimjeg.combatnexus.effect.api;
import java.util.*;
public record EffectApplication(UUID requestId, EffectInstance effect, net.minecraft.resources.Identifier stacking) {
    public EffectApplication(UUID requestId, EffectInstance effect, EffectService.Stacking stacking) { this(requestId, effect, stacking.id()); }
    public EffectApplication { Objects.requireNonNull(requestId); Objects.requireNonNull(effect); Objects.requireNonNull(stacking); }
}
