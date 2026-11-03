package io.github.naimjeg.combatnexus.presentation.client;

import io.github.naimjeg.combatnexus.presentation.api.DisplayText;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public final class DisplayTextResolver {
    private DisplayTextResolver() {
    }

    public static Component resolve(DisplayText text) {
        if (text == null) {
            return Component.empty();
        }
        return switch (text) {
            case DisplayText.Literal literal -> Component.literal(literal.text());
            case DisplayText.Translatable translatable -> {
                Object[] arguments = translatable.args().stream()
                        .map(DisplayTextResolver::argument)
                        .toArray(Object[]::new);
                if (!Language.getInstance().has(translatable.key())
                        && translatable.fallback().isEmpty()) {
                    yield Component.translatable(
                            "tooltip.combatnexus.unlocalized_display_text"
                    );
                }
                yield translatable.fallback().isPresent()
                        ? Component.translatableWithFallback(
                                translatable.key(),
                                translatable.fallback().get(),
                                arguments
                        )
                        : Component.translatable(translatable.key(), arguments);
            }
        };
    }

    public static Component argument(io.github.naimjeg.combatnexus.presentation.api.DisplayArgument argument) {
        return switch (argument) {
            case io.github.naimjeg.combatnexus.presentation.api.DisplayArgument.Binding ignored -> Component.translatable("tooltip.combatnexus.presentation.unavailable");
            case io.github.naimjeg.combatnexus.presentation.api.DisplayArgument.Literal value -> Component.literal(value.value());
            case io.github.naimjeg.combatnexus.presentation.api.DisplayArgument.Text value -> resolve(value.value());
            case io.github.naimjeg.combatnexus.presentation.api.DisplayArgument.Numeric value -> Component.literal(switch (value.kind()) {
                case NUMBER -> io.github.naimjeg.combatnexus.presentation.api.TooltipNumbers.number(value.value());
                case PERCENT -> io.github.naimjeg.combatnexus.presentation.api.TooltipNumbers.percent(value.value());
                case DURATION -> io.github.naimjeg.combatnexus.presentation.api.TooltipNumbers.number(value.value() / 20.0);
            });
        };
    }

    public static Optional<Component> resolveOptional(Optional<DisplayText> text) {
        return text == null ? Optional.empty() : text.map(DisplayTextResolver::resolve);
    }

}
