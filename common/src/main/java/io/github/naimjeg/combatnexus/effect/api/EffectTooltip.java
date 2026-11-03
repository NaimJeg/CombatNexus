package io.github.naimjeg.combatnexus.effect.api;

import io.github.naimjeg.combatnexus.attribute.api.AttributeUnits;
import io.github.naimjeg.combatnexus.combat.api.behavior.*;
import io.github.naimjeg.combatnexus.combat.api.action.TriggerLimit;
import io.github.naimjeg.combatnexus.presentation.api.TooltipBlock;
import io.github.naimjeg.combatnexus.presentation.api.TooltipNumbers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Shared definition explanation for equipment and effect tooltips, without live entity queries. */
public final class EffectTooltip {
    private EffectTooltip() {}
    /** Registered platform description at the carried amplifier, without a live entity or settlement. */
    public static List<Component> capability(Identifier id, int amplifier) {
        return List.copyOf(io.github.naimjeg.combatnexus.effect.internal.EffectCapabilities.describeRequired(id, amplifier));
    }
    public static TooltipBlock describe(Identifier id, EffectDefinition definition) {
        return describe(id, definition, definition.level(), Optional.empty(), Optional.empty(), true, definition.level());
    }
    private static TooltipBlock describe(Identifier id, EffectDefinition definition, int level,
            Optional<EffectView> instance, Optional<Component> source, boolean lifetime, int displayedLevel) {
        var summary = new ArrayList<Component>();
        var details = new ArrayList<Component>();
        for (var attribute : definition.attributes()) {
            var contribution = attribute.modifier();
            summary.add(io.github.naimjeg.combatnexus.presentation.api.AttributeDescription.describe(attribute.attribute(), new net.minecraft.world.entity.ai.attributes.AttributeModifier(contribution.source(), contribution.amount(), contribution.operation())));
        }
        definition.restrictions().stream().sorted().forEach(action -> summary.add(text("restriction." + action.name().toLowerCase(Locale.ROOT))));
        for (var behavior : definition.behavior().components()) {
            var explanation = io.github.naimjeg.combatnexus.effect.internal.EffectBehaviorDescriptors.describe(behavior, level);
            summary.addAll(explanation.summary());
            details.addAll(explanation.details());
            if (behavior instanceof PeriodicBehavior periodic) {
                details.add(text("phase." + periodic.phase().name().toLowerCase(Locale.ROOT)));
                if (periodic.phase() != PeriodicPhase.REMAINING_DURATION)
                    details.add(text("first_delay", TooltipNumbers.number(periodic.initialDelayTicks() / 20.0)));
            }
        }
        if (lifetime) summary.add(instance.map(view -> view.remainingTicks().isPresent()
            ? text("remaining", TooltipNumbers.number(Math.max(0, view.remainingTicks().getAsLong()) / 20.0))
            : text("permanent")).orElseGet(() -> switch (definition.lifetime().kind()) {
            case TIMED -> text("duration", TooltipNumbers.number(definition.lifetime().ticks() / 20.0));
            case INSTANT -> text("instant");
            case PERMANENT -> text("permanent");
        }));
        details.addFirst(text("level", displayedLevel));
        source.ifPresent(value -> details.add(text("source", value)));
        details.add(text("stacking." + definition.stacking().getPath()));
        details.add(text(definition.policy().dispellable() ? "dispellable" : "not_dispellable"));
        return new TooltipBlock(Component.translatableWithFallback("effect." + id.getNamespace() + "." + id.getPath().replace('/', '.'), id.getPath().replace('_', ' ')), summary, details);
    }
    public static TooltipBlock instance(Identifier id, EffectDefinition definition, EffectView view, Component source) {
        return describe(id, definition, view.level(), Optional.of(view), Optional.of(source), true, view.level());
    }

    public static TooltipBlock nativeInstance(Identifier id, EffectDefinition definition) {
        return describe(id, definition, definition.level(), Optional.empty(), Optional.empty(), false, definition.level() + 1);
    }
    private static Component limit(TriggerLimit limit) {
        return text("limit", text("scope." + limit.scope().name().toLowerCase(Locale.ROOT)), limit.maximum());
    }
    private static Component text(String key, Object... args) { return Component.translatable("tooltip.combatnexus.effect." + key, args); }
}
