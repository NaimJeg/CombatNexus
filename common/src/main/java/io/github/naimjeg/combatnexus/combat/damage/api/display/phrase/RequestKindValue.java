package io.github.naimjeg.combatnexus.combat.damage.api.display.phrase;
import io.github.naimjeg.combatnexus.presentation.api.phrase.*;

import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageRequestKind;
import java.util.Objects;

public record RequestKindValue(DamageRequestKind kind) implements PhraseValue {
    public RequestKindValue {
        Objects.requireNonNull(kind, "kind");
    }
}
