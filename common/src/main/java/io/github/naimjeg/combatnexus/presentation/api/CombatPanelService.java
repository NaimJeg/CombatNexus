package io.github.naimjeg.combatnexus.presentation.api;

import java.util.*;
import java.util.function.Supplier;

/** Self-only, rate-limited snapshot requests; lifecycle belongs to the world. */
public interface CombatPanelService {
    /** Call with authenticated sender and server tick, never a client supplied identity or clock. */
    public enum Refusal { NONE, FORBIDDEN, RATE_LIMITED, BUDGET_EXCEEDED }
    public record Result(Optional<CombatPanelSnapshot> snapshot, Refusal refusal) {
        public static Result reject(Refusal reason) { return new Result(Optional.empty(), reason); }
    }
    Result request(UUID sender, UUID target, long gameTick, Supplier<CombatPanelSnapshot> capture);
}
