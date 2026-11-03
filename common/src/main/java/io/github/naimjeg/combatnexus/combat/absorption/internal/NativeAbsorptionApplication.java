package io.github.naimjeg.combatnexus.combat.absorption.internal;

import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.absorption.api.*;

public final class NativeAbsorptionApplication {
    private NativeAbsorptionApplication() {}
    public static CommandResult<AbsorptionSource> apply(LivingEntity target, AbsorptionCommand command) {
        var ledger = NativeAbsorptionState.ledger(target);
        float before = target.getAbsorptionAmount();
        double owned = ledger.balance();
        boolean completed = false;
        try {
            ledger.change(command, before);
            target.setAbsorptionAmount((float)Math.max(0, Math.min(Float.MAX_VALUE, before + ledger.balance() - owned)));
            completed = true;
        } catch (RuntimeException failure) {
        } finally {
            ledger.reconcile(target.getAbsorptionAmount());
        }
        var journal = new ApplicationJournal(List.of(new ApplicationJournal.Entry(ApplicationJournal.Resource.ABSORPTION,
                Optional.of(command.source().id()), OptionalDouble.of(before), OptionalDouble.of(target.getAbsorptionAmount()),
                completed ? Math.abs(target.getAbsorptionAmount() - before) : 0,
                completed ? ApplicationJournal.Observation.NATIVE_WRITE : ApplicationJournal.Observation.UNKNOWN, completed)), completed);
        if (!completed) return CommandResult.<AbsorptionSource>rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "Absorption application incomplete; do not retry").withJournal(journal);
        var source = ledger.snapshot().stream().filter(s -> s.source().id().equals(command.source().id()) && s.source().owner().equals(command.source().owner())).findFirst().orElseThrow();
        return CommandResult.applied(source).withJournal(journal);
    }
}
