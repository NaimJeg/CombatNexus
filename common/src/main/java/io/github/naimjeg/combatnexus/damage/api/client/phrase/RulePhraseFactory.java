package io.github.naimjeg.combatnexus.damage.api.client.phrase;

@FunctionalInterface
public interface RulePhraseFactory {
    RulePhrase create(
            RulePhraseType type,
            PhraseVariant variant,
            PhraseArguments arguments
    );
}
