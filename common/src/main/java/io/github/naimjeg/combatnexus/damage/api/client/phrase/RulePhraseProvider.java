package io.github.naimjeg.combatnexus.damage.api.client.phrase;

@FunctionalInterface
public interface RulePhraseProvider<T> {
    RulePhrase provide(T value, RulePhraseFactory phrases);
}
