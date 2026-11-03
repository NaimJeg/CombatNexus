package io.github.naimjeg.combatnexus.presentation.client;

import io.github.naimjeg.combatnexus.presentation.api.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Common layout policy; entry points choose insertion and wrapping. */
public final class DescriptionRenderer {
    private DescriptionRenderer() {}
    public static List<Component> render(DescriptionDocument document, boolean expanded) {
        var lines = new ArrayList<Component>();
        render(lines, document, expanded, 0);
        if (!expanded && hasDetails(document)) lines.add(Component.translatable("tooltip.combatnexus.hold_shift").withStyle(ChatFormatting.DARK_GRAY));

        return lines;
    }
    private static void render(List<Component> lines, DescriptionDocument doc, boolean expanded, int depth) {
        doc.title().ifPresent(t -> line(lines, t, depth, ChatFormatting.AQUA));
        boolean authored = !doc.authoredSummary().isEmpty();
        for (var text : authored ? doc.authoredSummary() : doc.generated()) line(lines, text, depth + 1, ChatFormatting.GRAY);
        if (doc.showRuleBreakdown() && (!authored || expanded)) {
            if (authored) for (var text : doc.generated()) line(lines, text, depth + 1, ChatFormatting.GRAY);
            for (var rule : doc.rules()) {
                line(lines, rule.role(), depth + 1, ChatFormatting.DARK_AQUA);
                condition(lines, rule.condition(), expanded, depth + 2);
                for (var operation : rule.operations()) line(lines, expanded ? operation.detail() : operation.compact(), depth + 2, ChatFormatting.GRAY);
            }
            for (var child : doc.children()) render(lines, child, expanded, depth + 1);
        }
        if (expanded) for (var text : doc.details()) {
            if (!doc.generated().contains(text) && !doc.authoredSummary().contains(text)) line(lines, text, depth + 1, ChatFormatting.DARK_GRAY);
        }
        doc.flavor().ifPresent(t -> line(lines, t, depth + 1, ChatFormatting.DARK_GRAY));
    }
    private static void condition(List<Component> lines, DescriptionRule.Condition condition, boolean expanded, int depth) {
        if (condition.kind() == DescriptionRule.Kind.PHRASE) {
            var text = condition.text().orElseThrow();
            line(lines, expanded ? text.detail() : text.compact(), depth, ChatFormatting.GRAY);
            return;
        }
        if (condition.kind() == DescriptionRule.Kind.AND && condition.children().size() == 1) {
            condition(lines, condition.children().getFirst(), expanded, depth); return;
        }
        String key = switch (condition.kind()) {
            case ALWAYS -> "rule_phrase.combatnexus.always.default.compact";
            case AND -> "rule_condition_group.combatnexus.all_of";
            case OR -> "rule_condition_group.combatnexus.any_of";
            case NOT -> "rule_condition_group.combatnexus.not";
            default -> throw new IllegalStateException();
        };
        line(lines, DisplayText.translatable(key), depth, ChatFormatting.GRAY);
        for (var child : condition.children()) condition(lines, child, expanded, depth + 1);
    }

    private static boolean hasDetails(DescriptionDocument doc) {
        return !doc.details().isEmpty() || doc.showRuleBreakdown() && ((!doc.authoredSummary().isEmpty() && (!doc.rules().isEmpty() || !doc.children().isEmpty()))
            || doc.rules().stream().anyMatch(r -> r.operations().stream().anyMatch(t -> !t.compact().equals(t.detail())))
            || doc.children().stream().anyMatch(DescriptionRenderer::hasDetails));
    }
    private static void line(List<Component> lines, DisplayText text, int depth, ChatFormatting color) {
        lines.add(Component.literal("  ".repeat(depth)).append(DisplayTextResolver.resolve(text)).withStyle(color));
    }
}
