package io.github.naimjeg.combatnexus.presentation.api.phrase;

import net.minecraft.resources.Identifier;
import java.util.Objects;

public record IdentifierValue(Identifier identifier) implements PhraseValue {
    public IdentifierValue {
        Objects.requireNonNull(identifier, "identifier");
    }
}
