package io.github.naimjeg.combatnexus.combat.api.input;

import java.util.Objects;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

/** Typed capture schema. Values must encode without consulting a live world. */
public record FactKey<T>(Identifier id, Codec<T> codec) {
    public FactKey { Objects.requireNonNull(id); Objects.requireNonNull(codec); }
}
