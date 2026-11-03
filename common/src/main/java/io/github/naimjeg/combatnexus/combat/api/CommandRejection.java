package io.github.naimjeg.combatnexus.combat.api;

import java.util.Objects;

/** Stable business refusal; application failures retain their separate evidence. */
public record CommandRejection(Code code, String detail) {
    public CommandRejection { Objects.requireNonNull(code); Objects.requireNonNull(detail); }
    public enum Code { DUPLICATE, WRONG_THREAD, CLOSED_SESSION, INVALID_PHASE, TARGET_UNAVAILABLE,
        DEPTH, ROOT_BUDGET, TICK_BUDGET, COMPLETION_CAPACITY, IMMUNE, EXPIRED, NOT_FOUND,
        NOT_DISPELLABLE, STALE_HANDLE, INCOMPLETE_APPLICATION, INVALID_INPUT, NATIVE_REFUSED }
}
