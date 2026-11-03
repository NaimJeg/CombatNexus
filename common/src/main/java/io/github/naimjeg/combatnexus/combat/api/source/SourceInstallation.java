package io.github.naimjeg.combatnexus.combat.api.source;
import java.util.*;
public record SourceInstallation(UUID requestId, UUID target, SourceInstance source, SourcePlan plan) {
    public SourceInstallation { Objects.requireNonNull(requestId); Objects.requireNonNull(target); Objects.requireNonNull(source); Objects.requireNonNull(plan); }
}
