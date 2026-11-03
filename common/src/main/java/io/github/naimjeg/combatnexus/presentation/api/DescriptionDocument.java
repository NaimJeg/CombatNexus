package io.github.naimjeg.combatnexus.presentation.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;

/** Server-authored document. Children and rule operations retain their supplied order. */
public record DescriptionDocument(Optional<DisplayText> title, List<DisplayText> authoredSummary,
        List<DisplayText> generated, List<DisplayText> details, Optional<DisplayText> flavor,
        List<DescriptionDocument> children, List<DescriptionRule> rules, boolean showRuleBreakdown) {
    public static final Codec<DescriptionDocument> CODEC = codec(0);
    private static Codec<DescriptionDocument> codec(int depth) {
        Codec<List<DescriptionDocument>> children = depth < 8 ? Codec.lazyInitialized(() -> codec(depth + 1)).listOf(0, 128) : Codec.STRING.listOf(0, 0).flatXmap(v -> com.mojang.serialization.DataResult.success(List.of()), v -> v.isEmpty() ? com.mojang.serialization.DataResult.success(List.of()) : com.mojang.serialization.DataResult.error(() -> "Description nesting exceeds budget"));
        return RecordCodecBuilder.create(i -> i.group(
            DisplayText.CODEC.optionalFieldOf("title").forGetter(DescriptionDocument::title),
            DisplayText.CODEC.listOf(0,128).fieldOf("authored").forGetter(DescriptionDocument::authoredSummary),
            DisplayText.CODEC.listOf(0,128).fieldOf("generated").forGetter(DescriptionDocument::generated),
            DisplayText.CODEC.listOf(0,128).fieldOf("details").forGetter(DescriptionDocument::details),
            DisplayText.CODEC.optionalFieldOf("flavor").forGetter(DescriptionDocument::flavor),
            children.fieldOf("children").forGetter(DescriptionDocument::children),
            DescriptionRule.CODEC.listOf(0,128).fieldOf("rules").forGetter(DescriptionDocument::rules),
            Codec.BOOL.fieldOf("show_rules").forGetter(DescriptionDocument::showRuleBreakdown)
        ).apply(i, DescriptionDocument::new));
    }
    public DescriptionDocument {
        Objects.requireNonNull(title); Objects.requireNonNull(flavor);
        authoredSummary = List.copyOf(authoredSummary); generated = List.copyOf(generated); details = List.copyOf(details);
        children = List.copyOf(children); rules = List.copyOf(rules);
    }
    public static DescriptionDocument leaf(TooltipBlock block) {
        return new DescriptionDocument(DisplayTexts.fromComponent(block.title()), List.of(),
            block.summary().stream().map(DisplayTexts::fromComponent).flatMap(Optional::stream).toList(),
            block.details().stream().map(DisplayTexts::fromComponent).flatMap(Optional::stream).toList(),
            Optional.empty(), List.of(), List.of(), true);
    }
}
