package io.github.naimjeg.combatnexus.combat.shield.api;

import io.github.naimjeg.combatnexus.formula.Amounts;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;

/** Confirmed custom-shield change. It never replaces vanilla absorption or damage settlement fields. */
public record ShieldSettlement(UUID target, Kind kind, double amount, OptionalLong damageId, java.util.Set<net.minecraft.resources.Identifier> brokenSources) {
    public ShieldSettlement(UUID target, Kind kind, double amount, OptionalLong damageId) { this(target,kind,amount,damageId,java.util.Set.of()); }
    public ShieldSettlement {
        brokenSources=java.util.Set.copyOf(brokenSources);
        Objects.requireNonNull(target); Objects.requireNonNull(kind); Objects.requireNonNull(damageId);
        Amounts.nonNegative(amount, "amount");
    }
    public enum Kind { GRANTED, CONSUMED }
}
