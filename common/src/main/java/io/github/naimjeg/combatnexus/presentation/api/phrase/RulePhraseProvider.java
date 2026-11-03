package io.github.naimjeg.combatnexus.presentation.api.phrase;

@FunctionalInterface
public interface RulePhraseProvider<T> {
    RulePhrase provide(T value, RulePhraseFactory phrases);
}
