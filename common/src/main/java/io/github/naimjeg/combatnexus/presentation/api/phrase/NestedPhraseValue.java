package io.github.naimjeg.combatnexus.presentation.api.phrase;

import java.util.Objects;

public record NestedPhraseValue(RulePhrase phrase) implements PhraseValue {
    public NestedPhraseValue {
        Objects.requireNonNull(phrase, "phrase");
    }
}
