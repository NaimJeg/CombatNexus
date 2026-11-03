package io.github.naimjeg.combatnexus.combat.damage.api.display.phrase;
import io.github.naimjeg.combatnexus.presentation.api.phrase.*;

import net.minecraft.resources.Identifier;
import java.util.Objects;

public record EntityTypeValue(Identifier entityTypeId) implements PhraseValue {
    public EntityTypeValue {
        Objects.requireNonNull(entityTypeId, "entityTypeId");
    }
}
