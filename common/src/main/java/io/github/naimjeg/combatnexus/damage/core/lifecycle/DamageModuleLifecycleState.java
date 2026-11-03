package io.github.naimjeg.combatnexus.damage.core.lifecycle;

public enum DamageModuleLifecycleState {
    CONSTRUCTING,
    REGISTERING,
    FROZEN,
    RUNNING,

    /**
     * Terminal state entered when framework bootstrap fails after
     * registration has begun.
     */
    FAILED
}
