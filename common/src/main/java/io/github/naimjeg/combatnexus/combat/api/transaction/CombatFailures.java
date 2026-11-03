package io.github.naimjeg.combatnexus.combat.api.transaction;

public final class CombatFailures {
    private CombatFailures() {}
    public static void rethrowFatal(Throwable failure) {
        if (failure instanceof VirtualMachineError error) throw error;
        if (failure instanceof LinkageError error) throw error;
    }
}
