package io.github.naimjeg.combatnexus.combat.api.source;
import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.api.DefinitionDescription;
public record DefinitionInstallation(UUID requestId,UUID target,SourceInstance source,Identifier definition,DefinitionDescription.Kind kind,boolean persistent,int shieldDuration) {
    public DefinitionInstallation { Objects.requireNonNull(requestId);Objects.requireNonNull(target);Objects.requireNonNull(source);Objects.requireNonNull(definition);Objects.requireNonNull(kind);if(kind==DefinitionDescription.Kind.HEALING_RULE)throw new IllegalArgumentException("Global healing rules cannot be installed as entity sources");if(shieldDuration<1||shieldDuration>1200000)throw new IllegalArgumentException("Invalid shield lifetime"); }
}
