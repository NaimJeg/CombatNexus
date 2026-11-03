package io.github.naimjeg.combatnexus.combat.api.behavior;
/** Server-computed display state; clients never infer remaining protection from elapsed time. */
public record BehaviorView(BehaviorSource source, boolean protectionAvailable) {}
