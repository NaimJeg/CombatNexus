package io.github.naimjeg.combatnexus.combat.damage.api.rule.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.presentation.api.DisplayText;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleLimits;

import java.util.List;
import java.util.Optional;

public record DamageAffixDisplay(
        Optional<DisplayText> name,
        List<DisplayText> authoredSummary,
        Optional<DisplayText> flavorText,
        boolean showRuleBreakdown
) {
    public static final DamageAffixDisplay EMPTY =
            new DamageAffixDisplay(
                    Optional.empty(),
                    List.of(),
                    Optional.empty(),
                    false
            );

    public static final Codec<DamageAffixDisplay> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    DisplayText.CODEC.optionalFieldOf("name").forGetter(DamageAffixDisplay::name),
                    DamageRuleLimits.boundedList(DisplayText.CODEC, DamageRuleLimits.MAX_TOOLTIP_LINES,
                                    "affix authored summary")
                            .optionalFieldOf("authored_summary", List.of()).forGetter(DamageAffixDisplay::authoredSummary),
                    DisplayText.CODEC.optionalFieldOf("flavor_text").forGetter(DamageAffixDisplay::flavorText),
                    Codec.BOOL.optionalFieldOf("show_rule_breakdown", false).forGetter(DamageAffixDisplay::showRuleBreakdown)
            ).apply(instance, DamageAffixDisplay::new));

    public DamageAffixDisplay {
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

    public DamageAffixDisplay(
            DisplayText name,
            List<DisplayText> authoredSummary,
            Optional<DisplayText> flavorText,
            boolean showRuleBreakdown
    ) {
        this(Optional.ofNullable(name), authoredSummary, flavorText, showRuleBreakdown);
    }
}
