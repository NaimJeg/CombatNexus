package io.github.naimjeg.combatnexus.effect.api;

import java.util.*;
import net.minecraft.resources.Identifier;

/** Unified detached facts. Unknown instance/owner attribution is explicit; observation never installs contributions. */
public record EffectView(UUID target, Identifier definition, Optional<Identifier> source, Optional<UUID> sourceInstance,
        EffectInstance.Ownership ownership, Set<Identifier> tags, int level, OptionalLong remainingTicks,
        Optional<UUID> owner, Completeness completeness, Set<Operation> operations, io.github.naimjeg.combatnexus.combat.api.source.SourceProvenance provenance) {
    public enum Completeness { COMPLETE, UNKNOWN_SOURCE }
    public enum Operation { DISPEL }
    public EffectView {
        Objects.requireNonNull(target); Objects.requireNonNull(definition); Objects.requireNonNull(source);
        Objects.requireNonNull(sourceInstance); Objects.requireNonNull(ownership); Objects.requireNonNull(remainingTicks);
        Objects.requireNonNull(provenance); Objects.requireNonNull(owner); Objects.requireNonNull(completeness);
        tags = Set.copyOf(tags); operations = Set.copyOf(operations);
    }
    public boolean hasTag(Identifier tag) { return tags.contains(tag); }
}
