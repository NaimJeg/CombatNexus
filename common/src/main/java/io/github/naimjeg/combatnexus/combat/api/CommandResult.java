package io.github.naimjeg.combatnexus.combat.api;

import java.util.*;

/** Result of a state command. A refused command has no value. */
public record CommandResult<T>(Optional<T> value, Optional<CommandRejection> rejection,
        ApplicationJournal journal, Optional<CommandContext> context) {
    public CommandResult {
        Objects.requireNonNull(value); Objects.requireNonNull(rejection);
        Objects.requireNonNull(journal); Objects.requireNonNull(context);
        if (value.isPresent() == rejection.isPresent()) throw new IllegalArgumentException("Exactly one result required");
    }
    public boolean applied() { return rejection.isEmpty(); }
    public CommandResult<T> withJournal(ApplicationJournal journal) { return new CommandResult<>(value, rejection, journal, context); }
    public CommandResult<T> withContext(CommandContext context) { return new CommandResult<>(value, rejection, journal, Optional.of(context)); }
    public static <T> CommandResult<T> applied(T value) { return new CommandResult<>(Optional.of(value), Optional.empty(), new ApplicationJournal(List.of(), true), Optional.empty()); }
    public static <T> CommandResult<T> rejected(CommandRejection.Code code, String detail) {
        return new CommandResult<>(Optional.empty(), Optional.of(new CommandRejection(code, detail)),
                new ApplicationJournal(List.of(), code != CommandRejection.Code.INCOMPLETE_APPLICATION), Optional.empty());
    }
}
