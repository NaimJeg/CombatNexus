package io.github.naimjeg.combatnexus.combat.api;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingRuleDefinition;
import io.github.naimjeg.combatnexus.combat.regeneration.api.RegenerationDefinition;
import io.github.naimjeg.combatnexus.combat.shield.api.ShieldDefinition;
import io.github.naimjeg.combatnexus.effect.api.EffectDefinition;
import net.minecraft.resources.Identifier;
import java.util.*;
/** Immutable domain definitions, adopted atomically with damage and behavior definitions. */
public record DomainDefinitions(Map<Identifier, HealingRuleDefinition> healingRules,
        Map<Identifier, RegenerationDefinition> regeneration, Map<Identifier, ShieldDefinition> shields,
        Map<Identifier, EffectDefinition> effects) {
    public static final DomainDefinitions EMPTY = new DomainDefinitions(Map.of(), Map.of(), Map.of(), Map.of());
    public DomainDefinitions {
        healingRules = Map.copyOf(healingRules); regeneration = Map.copyOf(regeneration);
        shields = Map.copyOf(shields); effects = Map.copyOf(effects);
        if (healingRules.size() > 64 || regeneration.size() > 256 || shields.size() > 256 || effects.size() > 256)
            throw new IllegalArgumentException("Domain definition capacity exceeded");
    }
}
