package io.github.naimjeg.combatnexus.combat.api.reaction;
import java.util.*;
/** Descriptive child request identity, never reusable authority. */
public record ReactionResult(String outcome, Optional<String> childRequest) {
    public ReactionResult { Objects.requireNonNull(outcome); Objects.requireNonNull(childRequest);
        if (outcome.length() > 128 || childRequest.map(String::length).orElse(0) > 128) throw new IllegalArgumentException("Reaction fact too large"); }
    public static ReactionResult completed() { return new ReactionResult("executed", Optional.empty()); }
}
