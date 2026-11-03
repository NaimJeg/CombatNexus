package io.github.naimjeg.combatnexus.combat.api.source;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.CommandRejection;
/** Failed compensation still returns its cleanup handle. A handle alone never means installation succeeded. */
public record SourceResult(Optional<InstallationHandle> value, Optional<CommandRejection> rejection,
        io.github.naimjeg.combatnexus.combat.api.ApplicationJournal journal,
        Optional<io.github.naimjeg.combatnexus.combat.api.CommandContext> context) {
    public SourceResult { Objects.requireNonNull(value); Objects.requireNonNull(rejection); }
    public boolean applied() { return rejection.isEmpty() && value.isPresent(); }
}
