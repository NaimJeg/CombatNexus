package io.github.naimjeg.combatnexus.presentation.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;

/** No predicate or operation implementation crosses the description boundary. */
public record DescriptionRule(Condition condition, List<Text> operations, DisplayText role) {
    public record Text(DisplayText compact, DisplayText detail) {
        public static final Codec<Text> CODEC = RecordCodecBuilder.create(i -> i.group(
            DisplayText.CODEC.fieldOf("compact").forGetter(Text::compact),
            DisplayText.CODEC.fieldOf("detail").forGetter(Text::detail)).apply(i, Text::new));
    }
    public enum Kind { ALWAYS, PHRASE, AND, OR, NOT }
    public record Condition(Kind kind, Optional<Text> text, List<Condition> children) {
        public static final Codec<Condition> CODEC = codec(0);
        private static Codec<Condition> codec(int depth) {
            Codec<List<Condition>> children = depth < 16 ? Codec.lazyInitialized(() -> codec(depth + 1)).listOf(0,128) : Codec.STRING.listOf(0, 0).flatXmap(v -> com.mojang.serialization.DataResult.success(List.of()), v -> v.isEmpty() ? com.mojang.serialization.DataResult.success(List.of()) : com.mojang.serialization.DataResult.error(() -> "Description nesting exceeds budget"));
            return RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.xmap(Kind::valueOf, Enum::name).fieldOf("kind").forGetter(Condition::kind),
                Text.CODEC.optionalFieldOf("text").forGetter(Condition::text),
                children.fieldOf("children").forGetter(Condition::children)).apply(i, Condition::new));
        }
        public Condition {
            Objects.requireNonNull(kind); Objects.requireNonNull(text); children = List.copyOf(children);
            if ((kind == Kind.PHRASE) != text.isPresent() || kind == Kind.NOT && children.size() != 1
                    || (kind == Kind.ALWAYS || kind == Kind.PHRASE) && !children.isEmpty())
                throw new IllegalArgumentException("Invalid description condition");
        }
    }
    public static final Codec<DescriptionRule> CODEC = RecordCodecBuilder.create(i -> i.group(
        Condition.CODEC.fieldOf("condition").forGetter(DescriptionRule::condition),
        Text.CODEC.listOf(0,128).fieldOf("operations").forGetter(DescriptionRule::operations),
        DisplayText.CODEC.fieldOf("role").forGetter(DescriptionRule::role)).apply(i, DescriptionRule::new));
    public DescriptionRule { Objects.requireNonNull(condition); Objects.requireNonNull(role); operations = List.copyOf(operations); }
}
