package io.github.naimjeg.combatnexus.effect.api;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.CommandRejection;
public record EffectResult(Outcome outcome, Optional<CommandRejection> rejection,
        io.github.naimjeg.combatnexus.combat.api.ApplicationJournal journal,
        Optional<io.github.naimjeg.combatnexus.combat.api.CommandContext> context) {
    public enum Outcome { APPLIED, REMOVED, REJECTED }
    public EffectResult { Objects.requireNonNull(outcome); Objects.requireNonNull(rejection); }
}
