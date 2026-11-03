package io.github.naimjeg.combatnexus.combat.api;

import java.util.*;

/** Frozen resource evidence. Whole-call observations never masquerade as native write receipts. */
public record ApplicationJournal(List<Entry> entries, boolean applicationCompleted) {
    public ApplicationJournal { entries = List.copyOf(entries); }
    public static final ApplicationJournal EMPTY = new ApplicationJournal(List.of(), false);
    public enum Resource { HEALTH, SHIELD, ABSORPTION, PROTECTION, EFFECT, POSITION, VELOCITY, FIRE, PROJECTILE, COOLDOWN, STATE, FOOD, SATURATION }
    public enum Observation { NATIVE_WRITE, WHOLE_CALL, UNKNOWN }
    public record Entry(Resource resource, Optional<net.minecraft.resources.Identifier> source, OptionalDouble before, OptionalDouble after, double confirmedAmount,
                        Observation observation, boolean completed) {
        public Entry {
            Objects.requireNonNull(resource); Objects.requireNonNull(source); Objects.requireNonNull(before); Objects.requireNonNull(after);
            Objects.requireNonNull(observation);
            if (!Double.isFinite(confirmedAmount) || confirmedAmount < 0
                    || before.isPresent() && !Double.isFinite(before.getAsDouble())
                    || after.isPresent() && !Double.isFinite(after.getAsDouble()))
                throw new IllegalArgumentException("Invalid application evidence");
            if (observation == Observation.UNKNOWN && confirmedAmount != 0)
                throw new IllegalArgumentException("Unknown write cannot claim an amount");
        }
        public static Entry write(Resource resource, net.minecraft.resources.Identifier source,
                                  double before, double after, double confirmed, boolean completed) {
            return new Entry(resource, Optional.of(source), OptionalDouble.of(before), OptionalDouble.of(after),
                    confirmed, Observation.NATIVE_WRITE, completed);
        }
        public static Entry write(Resource resource, double before, double after, double confirmed, boolean completed) {
            return new Entry(resource, Optional.empty(), OptionalDouble.of(before), OptionalDouble.of(after), confirmed,
                    Observation.NATIVE_WRITE, completed);
        }
    }
}
