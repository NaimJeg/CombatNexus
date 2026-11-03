package io.github.naimjeg.combatnexus.presentation.api.phrase;

public enum PhraseForm {
    COMPACT("compact"),
    DETAIL("detail");

    private final String serializedName;

    PhraseForm(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
