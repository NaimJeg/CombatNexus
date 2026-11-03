package io.github.naimjeg.combatnexus.combat.api.bridge;

import java.util.*;

/** Decision evidence is descriptive and contains no retained live sampler or entity. */
public record DamageFormulaResult(DamageFormulaRequest request, DamageFormulaPolicy.Output output,
        List<Float> samples, long definitionGeneration, boolean completed) {
    public DamageFormulaResult { samples = List.copyOf(samples); }
}
