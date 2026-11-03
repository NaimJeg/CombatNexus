package io.github.naimjeg.combatnexus.presentation.api;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;

/** Bounds for the shared text wire format. */
public final class DisplayTextLimits {
    private DisplayTextLimits() {}
    public static final int MAX_TRANSLATION_ARGS = 16;
    public static final int MAX_DISPLAY_CODE_POINTS = 256;
    public static Codec<String> boundedString(
            int maximumCodePoints,
            String name
    ) {
        return Codec.STRING.validate(value -> {
            int length = value.codePointCount(0, value.length());

            if (length <= maximumCodePoints) {
                return DataResult.success(value);
            }

            return DataResult.error(() ->
                    name + " exceeds " + maximumCodePoints
                            + " Unicode code points"
            );
        });
    }
    public static <T> Codec<List<T>> boundedList(
            Codec<T> elementCodec,
            int maximum,
            String name
    ) {
        return elementCodec
                .listOf(0, maximum)
                .xmap(List::copyOf, List::copyOf)
                .validate(values -> values.size() <= maximum
                        ? DataResult.success(values)
                        : DataResult.error(() ->
                        name + " exceeds maximum " + maximum));
    }
}
