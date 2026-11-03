package io.github.naimjeg.combatnexus.combat.api;
import java.util.*;
/** Uniform execution evidence without discarding the domain-specific result. */
public record CommandOutcome<T>(Class<T> resultType, Optional<T> value, boolean applied,
        ApplicationJournal journal, Optional<CommandContext> context, Optional<CommandRejection> rejection) {
    public CommandOutcome { Objects.requireNonNull(resultType); Objects.requireNonNull(value); Objects.requireNonNull(journal); Objects.requireNonNull(context); Objects.requireNonNull(rejection); }
    public boolean completed() { return journal.applicationCompleted(); }
    public <R> Optional<R> result(Class<R> type) { return value.filter(type::isInstance).map(type::cast); }
    public static CommandOutcome<Void> rejected(CommandRejection.Code code, String detail) {
        var r = CommandResult.<Void>rejected(code, detail);
        return new CommandOutcome<>(Void.class, Optional.empty(), false, r.journal(), r.context(), r.rejection());
    }
}
