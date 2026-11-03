package io.github.naimjeg.combatnexus.damage.api.client.phrase;

import net.minecraft.resources.Identifier;
import java.util.Objects;

public record EffectValue(Identifier effectId) implements PhraseValue {
    public EffectValue {
        Objects.requireNonNull(effectId, "effectId");
    }
}
