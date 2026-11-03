package io.github.naimjeg.combatnexus.damage.core.trace;

public enum RuleSkipReason {
    CONDITION_FAILED,
    EMPTY_OPERATIONS,
    PHASE_MISMATCH,
    ROLE_MISMATCH,
    PHASE_OPERATION_MISMATCH,
    STACKING_DROPPED,
    PROVIDER_FILTERED
}
