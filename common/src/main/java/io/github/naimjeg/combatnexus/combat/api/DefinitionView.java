package io.github.naimjeg.combatnexus.combat.api;
import java.util.*;
import net.minecraft.resources.Identifier;
/** Detached catalog description; never an execution definition or a publication capability. */
public record DefinitionView(long generation, Set<Identifier> effects, Set<Identifier> behaviors,
        Set<Identifier> shields, Set<Identifier> regeneration, Set<Identifier> healingRules) {
    public DefinitionView { effects = Set.copyOf(effects); behaviors = Set.copyOf(behaviors); shields = Set.copyOf(shields);
        regeneration = Set.copyOf(regeneration); healingRules = Set.copyOf(healingRules); }
}
