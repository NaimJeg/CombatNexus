package io.github.naimjeg.combatnexus.combat.shield.api;

import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.api.ApplicationJournal;

/** Confirmed pool writes, available before any observers are scheduled. */
public record ShieldCommitReceipt(UUID target, long applicationId, double absorbed,
                                  List<ApplicationJournal.Entry> writes, Map<Identifier, Double> absorbedChannels) {
    public ShieldCommitReceipt {
        Objects.requireNonNull(target); writes = List.copyOf(writes);
        absorbedChannels = Map.copyOf(absorbedChannels);
        io.github.naimjeg.combatnexus.formula.Amounts.nonNegative(absorbed, "absorbed");
        if (applicationId < 1) throw new IllegalArgumentException("Invalid application identity");
    }
}
