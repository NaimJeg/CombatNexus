package io.github.naimjeg.combatnexus.effect.api;

import io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent;
import io.github.naimjeg.combatnexus.presentation.api.TooltipBlock;

/** Startup-registered semantic explanation, evaluated on the server with the adopted level. */
@FunctionalInterface
public interface EffectBehaviorDescriptor<T extends BehaviorComponent> {
    TooltipBlock describe(T behavior, int level);
}
