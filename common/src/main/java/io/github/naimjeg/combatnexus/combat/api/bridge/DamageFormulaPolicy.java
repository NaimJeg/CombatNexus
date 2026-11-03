package io.github.naimjeg.combatnexus.combat.api.bridge;

import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame;

/** Pure numerical extension: all participant facts are frozen and sampling is recorded by the host. */
@FunctionalInterface
public interface DamageFormulaPolicy {
    Output calculate(Input input, Samples samples);
    record Input(double amount, CombatInputFrame target, Optional<CombatInputFrame> attacker, Optional<UUID> action) {}
    record Output(double amount, List<Double> multipliers) {
        public Output {
            multipliers = List.copyOf(multipliers);
            if (!Double.isFinite(amount) || amount < 0 || multipliers.stream().anyMatch(v -> !Double.isFinite(v) || v < 0))
                throw new IllegalArgumentException("Invalid numerical decision");
        }
    }
    interface Samples {
        enum Sharing { ACTION, TARGET }
        float nextFloat();
        float shared(net.minecraft.resources.Identifier decision,Sharing sharing);
    }
}
