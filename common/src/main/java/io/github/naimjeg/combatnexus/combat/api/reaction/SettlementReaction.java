package io.github.naimjeg.combatnexus.combat.api.reaction;
import java.util.*;
import java.util.function.*;
import net.minecraft.resources.Identifier;
/** Condition returns an explicit rejection reason; action runs only inside the existing callback boundary. */
public record SettlementReaction<C>(Identifier id, int priority, Function<C, Optional<String>> rejection, Function<C, ReactionResult> action) {
    public SettlementReaction { Objects.requireNonNull(id); Objects.requireNonNull(rejection); Objects.requireNonNull(action);
        if (id.toString().length() > 256 || priority < -10000 || priority > 10000) throw new IllegalArgumentException("Invalid reaction identity or priority"); }
}
