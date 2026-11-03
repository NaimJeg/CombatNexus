package io.github.naimjeg.combatnexus.combat.api;
import java.util.*;
import net.minecraft.resources.Identifier;
/** Read-only adopted definition and its publication identity. */
public record DefinitionDescription(long generation, Identifier id, Kind kind, Map<String,String> parameters) {
    public enum Kind { EFFECT, BEHAVIOR, SHIELD, REGENERATION, HEALING_RULE }
    public DefinitionDescription { Objects.requireNonNull(id);Objects.requireNonNull(kind);parameters=Map.copyOf(parameters); }
}
