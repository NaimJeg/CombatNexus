package io.github.naimjeg.combatnexus.effect.api;

import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;

/** Native owner retains duration, stacking and modifier ownership. REFRESH uses native merge rules. */
public record NativeEffectCommand(UUID requestId, UUID target, CombatSource source, Identifier effect,
        Operation operation, int durationTicks, int amplifier, boolean ambient, boolean visible, boolean icon) {
    public enum Operation { APPLY, REFRESH, REMOVE }
    public NativeEffectCommand {
        Objects.requireNonNull(requestId); Objects.requireNonNull(target); Objects.requireNonNull(source);
        Objects.requireNonNull(effect); Objects.requireNonNull(operation);
        if (durationTicks < -1 || amplifier < 0) throw new IllegalArgumentException("Invalid native effect parameters");
    }
}
