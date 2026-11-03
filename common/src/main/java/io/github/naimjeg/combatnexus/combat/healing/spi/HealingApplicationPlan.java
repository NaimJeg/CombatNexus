package io.github.naimjeg.combatnexus.combat.healing.spi;
import io.github.naimjeg.combatnexus.combat.healing.api.*;

import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame;
import io.github.naimjeg.combatnexus.formula.Amounts;

public record HealingApplicationPlan(HealingRequest request, double modifiedAmount,
        long definitionGeneration, Optional<CombatInputFrame> input, List<HealingContribution> contributions, io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan beforeApplication) {
    public HealingApplicationPlan {
        Objects.requireNonNull(beforeApplication); Objects.requireNonNull(request); Objects.requireNonNull(input);
        Amounts.nonNegative(modifiedAmount, "modifiedAmount");
        contributions = List.copyOf(contributions);
        if (contributions.size() > HealingContribution.MAX_FACTS) throw new IllegalArgumentException("Healing contribution capacity exceeded");
    }
}
