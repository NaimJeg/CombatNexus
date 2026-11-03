package io.github.naimjeg.combatnexus.damage.api.client.phrase;

import io.github.naimjeg.combatnexus.damage.api.damage.DamageRequestKind;
import java.util.Objects;

public record RequestKindValue(DamageRequestKind kind) implements PhraseValue {
    public RequestKindValue {
        Objects.requireNonNull(kind, "kind");
    }
}
