package io.github.naimjeg.combatnexus.presentation.api;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;

/** Detached values only. Strings remain literal, even when they resemble translation keys. */
public sealed interface DisplayArgument {
    record Binding(String name) implements DisplayArgument {
        public Binding { if (name == null || !name.matches("[a-z0-9_:/.-]{1,256}")) throw new IllegalArgumentException("Invalid description binding"); }
    }
    enum Kind { NUMBER, PERCENT, DURATION }
    record Literal(String value) implements DisplayArgument {
        public Literal {
            if (value == null || !value.matches(" {0,256}"))
                DisplayText.invalidText(value, "argument").ifPresent(reason -> { throw new IllegalArgumentException(reason); });
        }
    }
    record Text(DisplayText value) implements DisplayArgument {
        public Text { Objects.requireNonNull(value); if (depth(value) > 8) throw new IllegalArgumentException("Text nesting exceeds 8"); }
    }
    record Numeric(Kind kind, double value) implements DisplayArgument {
        public Numeric { Objects.requireNonNull(kind); if (!Double.isFinite(value) || kind == Kind.DURATION && value < 0) throw new IllegalArgumentException("Invalid numeric display argument"); }
    }
    static DisplayArgument number(double value) { return new Numeric(Kind.NUMBER, value); }
    static DisplayArgument percent(double ratio) { return new Numeric(Kind.PERCENT, ratio); }
    static DisplayArgument duration(long ticks) { return new Numeric(Kind.DURATION, ticks); }
    static DisplayArgument text(DisplayText value) { return new Text(value); }
    static Codec<DisplayArgument> codec(int depth) {
        Codec<Numeric> numeric = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.xmap(Kind::valueOf, Enum::name).fieldOf("kind").forGetter(Numeric::kind),
            Codec.DOUBLE.fieldOf("value").forGetter(Numeric::value)).apply(i, Numeric::new));
        Codec<DisplayArgument> scalar = Codec.either(DisplayTextLimits.boundedString(256, "argument"), numeric)
            .xmap(e -> e.map(Literal::new, n -> n), a -> a instanceof Literal l ? Either.left(l.value()) : Either.right((Numeric) a));
        Codec<DisplayArgument> values = Codec.either(scalar, Codec.STRING.fieldOf("bind").codec())
            .xmap(e -> e.map(a -> a, Binding::new), a -> a instanceof Binding b ? Either.right(b.name()) : Either.left(a));
        if (depth >= 8) return values;
        return Codec.either(values, DisplayText.codec(depth + 1).fieldOf("nested").codec())
            .xmap(e -> e.map(a -> a, Text::new), a -> a instanceof Text t ? Either.right(t.value()) : Either.left(a));
    }
    default Optional<String> validationProblem() { return Optional.empty(); }
    default long codePoints() {
        return switch (this) {
            case Literal l -> l.value().codePointCount(0, l.value().length());
            case Text t -> t.value().codePoints();
            case Numeric n -> Double.toString(n.value()).length();
            case Binding b -> b.name().length();
        };
    }
    private static int depth(DisplayText text) {
        return text instanceof DisplayText.Translatable t ? 1 + t.args().stream()
            .filter(Text.class::isInstance).map(Text.class::cast).mapToInt(a -> depth(a.value())).max().orElse(0) : 1;
    }
}
