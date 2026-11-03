package io.github.naimjeg.combatnexus.combat.damage.api.rule.entry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.presentation.api.DisplayText;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleLimits;

import java.util.List;
import java.util.Optional;

public record DamageEntryDisplay(
        Optional<DisplayText> name,
        List<DisplayText> authoredSummary,
        Optional<DisplayText> flavorText,
        boolean showRuleBreakdown
) {
    public static final DamageEntryDisplay EMPTY =
            new DamageEntryDisplay(
                    Optional.empty(),
                    List.of(),
                    Optional.empty(),
                    false
            );

    public static final Codec<DamageEntryDisplay> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    DisplayText.CODEC.optionalFieldOf("name").forGetter(DamageEntryDisplay::name),
                    DamageRuleLimits.boundedList(DisplayText.CODEC, DamageRuleLimits.MAX_TOOLTIP_LINES,
                                    "entry authored summary")
                            .optionalFieldOf("authored_summary", List.of()).forGetter(DamageEntryDisplay::authoredSummary),
                    DisplayText.CODEC.optionalFieldOf("flavor_text").forGetter(DamageEntryDisplay::flavorText),
                    Codec.BOOL.optionalFieldOf("show_rule_breakdown", false).forGetter(DamageEntryDisplay::showRuleBreakdown)
            ).apply(instance, DamageEntryDisplay::new));

    public DamageEntryDisplay {
        name = name == null ? Optional.empty() : name;
        authoredSummary = authoredSummary == null ? List.of() : List.copyOf(authoredSummary);
        flavorText = flavorText == null ? Optional.empty() : flavorText;
    }

    public boolean hasVisibleText() {
        return name.isPresent()
                || !authoredSummary.isEmpty()
                || flavorText.isPresent()
                || showRuleBreakdown;
    }

    public DamageEntryDisplay(
            DisplayText name,
            List<DisplayText> authoredSummary,
            Optional<DisplayText> flavorText,
            boolean showRuleBreakdown
    ) {
        this(Optional.ofNullable(name), authoredSummary, flavorText, showRuleBreakdown);
    }
}
