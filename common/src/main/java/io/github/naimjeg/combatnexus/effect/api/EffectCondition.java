package io.github.naimjeg.combatnexus.effect.api;

import java.util.Map;

/** Pure predicate over a detached fact sample. */
@FunctionalInterface
public interface EffectCondition { boolean test(Map<String, Double> facts); }
