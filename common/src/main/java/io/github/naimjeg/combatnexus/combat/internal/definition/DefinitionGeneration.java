package io.github.naimjeg.combatnexus.combat.internal.definition;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.config.CombatServerSettings;
import io.github.naimjeg.combatnexus.combat.damage.core.definition.DamageDefinitions;
import io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorDefinition;
import net.minecraft.resources.Identifier;
import java.util.*;
/** Complete immutable resource generation. Requests retain this exact handle. */
public record DefinitionGeneration(long generation, CombatServerSettings settings,
        DamageDefinitions damage, Map<Identifier, BehaviorDefinition> behaviors, DomainDefinitions domains) {
    public DefinitionGeneration {
        if (generation < 1) throw new IllegalArgumentException("Invalid generation");
        Objects.requireNonNull(settings); Objects.requireNonNull(damage); behaviors = Map.copyOf(behaviors); Objects.requireNonNull(domains);
    }
}
