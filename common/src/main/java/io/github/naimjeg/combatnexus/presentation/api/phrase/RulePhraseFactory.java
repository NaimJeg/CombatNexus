package io.github.naimjeg.combatnexus.presentation.api.phrase;

@FunctionalInterface
public interface RulePhraseFactory {
    RulePhrase create(
            RulePhraseType type,
            PhraseVariant variant,
            PhraseArguments arguments
    );
}
