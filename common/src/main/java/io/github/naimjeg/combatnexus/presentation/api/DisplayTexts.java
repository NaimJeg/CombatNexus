package io.github.naimjeg.combatnexus.presentation.api;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import java.util.*;

/** Converts simple text leaves without consulting the server's language. */
public final class DisplayTexts {
    private DisplayTexts() {}
    public static Optional<DisplayText> fromComponent(Component component) {
        Optional<DisplayText> result;
        if (component.getContents() instanceof TranslatableContents t) {
            var args = Arrays.stream(t.getArgs()).map(DisplayTexts::argument).toList();
            result = Optional.of(new DisplayText.Translatable(t.getKey(), args, Optional.ofNullable(t.getFallback())));
        } else if (component.getContents() instanceof PlainTextContents p) {
            result = p.text().isEmpty() ? Optional.empty() : p.text().isBlank()
                ? Optional.of(new DisplayText.Translatable("tooltip.combatnexus.text.literal", List.of(new DisplayArgument.Literal(p.text())), Optional.of("%s")))
                : Optional.of(DisplayText.literal(p.text()));
        } else throw new IllegalArgumentException("Unsupported description text contents");
        for (var sibling : component.getSiblings()) {
            var next = fromComponent(sibling);
            if (next.isEmpty()) continue;
            result = result.isEmpty() ? next : Optional.of(DisplayText.translated("tooltip.combatnexus.text.concat",
                DisplayArgument.text(result.get()), DisplayArgument.text(next.get())));
        }
        return result;
    }
    private static DisplayArgument argument(Object value) {
        if (value instanceof Component component) return fromComponent(component).map(DisplayArgument::text).orElseGet(() -> new DisplayArgument.Literal(""));
        if (value instanceof Number number) return DisplayArgument.number(number.doubleValue());
        return new DisplayArgument.Literal(value.toString());
    }
}
