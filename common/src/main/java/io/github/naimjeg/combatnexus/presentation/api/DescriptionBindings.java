package io.github.naimjeg.combatnexus.presentation.api;

import java.util.*;

/** Explicit descriptor parameters only; no reflective field access or expression evaluation. */
public final class DescriptionBindings {
    private DescriptionBindings() {}
    public static DisplayText bind(DisplayText text, Map<String, DisplayArgument> parameters) {
        return switch (text) {
            case DisplayText.Literal literal -> literal;
            case DisplayText.Translatable translated -> new DisplayText.Translatable(translated.key(), translated.args().stream()
                .map(a -> argument(a, parameters)).toList(), translated.fallback());
        };
    }
    private static DisplayArgument argument(DisplayArgument argument, Map<String, DisplayArgument> parameters) {
        return switch (argument) {
            case DisplayArgument.Binding binding -> {
                var value = Objects.requireNonNull(parameters.get(binding.name()), "Missing descriptor parameter: " + binding.name());
                if (value instanceof DisplayArgument.Binding) throw new IllegalArgumentException("Bindings cannot refer to bindings");
                yield value;
            }
            case DisplayArgument.Text nested -> DisplayArgument.text(bind(nested.value(), parameters));
            default -> argument;
        };
    }
}
