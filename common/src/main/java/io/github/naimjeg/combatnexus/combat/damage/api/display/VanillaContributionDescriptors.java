package io.github.naimjeg.combatnexus.combat.damage.api.display;

import io.github.naimjeg.combatnexus.combat.damage.api.display.DamageContributionDescriptor;
import io.github.naimjeg.combatnexus.combat.damage.api.display.DamageContributionOperationKind;
import io.github.naimjeg.combatnexus.combat.damage.api.display.DamageContributionSourceKind;
import io.github.naimjeg.combatnexus.combat.damage.api.display.DamageContributionStatus;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class VanillaContributionDescriptors {
    private VanillaContributionDescriptors() {
    }

    public static DamageContributionDescriptor vanillaBase(
            Identifier id,
            DamagePhase phase,
            Identifier channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceLabel
    ) {
        return vanillaBase(
                id,
                DamageContributionSourceKind.VANILLA_BRIDGE,
                phase,
                channel,
                bucket,
                value,
                sourceLabel
        );
    }

    public static DamageContributionDescriptor vanillaBase(
            Identifier id,
            DamageContributionSourceKind sourceKind,
            DamagePhase phase,
            Identifier channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceLabel
    ) {
        DamageContributionSourceKind resolvedSourceKind =
                sourceKind == null
                        ? DamageContributionSourceKind.VANILLA_BRIDGE
                        : sourceKind;

        return new DamageContributionDescriptor(
                id,
                resolvedSourceKind,
                DamageContributionOperationKind.ADD_BASE_DAMAGE,
                phase,
                Optional.ofNullable(channel),
                Optional.ofNullable(bucket),
                Optional.empty(),
                value,
                DamageContributionStatus.APPLIED,
                displayGroup(id, sourceLabel),
                Optional.empty(),
                displayName(resolvedSourceKind, sourceLabel),
                tooltipLines(
                        "vanilla_base",
                        channel,
                        bucket,
                        null,
                        value,
                        sourceLabel
                ),
                true);
    }

    public static DamageContributionDescriptor vanillaMultiplier(
            Identifier id,
            DamagePhase phase,
            Identifier channel,
            DamageApplicationBucket bucket,
            Identifier preMultiplierBucket,
            float value,
            String sourceLabel
    ) {
        return new DamageContributionDescriptor(
                id,
                DamageContributionSourceKind.VANILLA_BRIDGE,
                DamageContributionOperationKind.ADD_APPLICATION_PRE_MULTIPLIER,
                phase,
                Optional.ofNullable(channel),
                Optional.ofNullable(bucket),
                Optional.ofNullable(preMultiplierBucket),
                value,
                DamageContributionStatus.APPLIED,
                displayGroup(id, sourceLabel),
                Optional.empty(),
                Optional.of(displayName("Vanilla Scaling", sourceLabel)),
                tooltipLines(
                        "vanilla_multiplier",
                        channel,
                        bucket,
                        preMultiplierBucket,
                        value,
                        sourceLabel
                ),
                true);
    }

    public static DamageContributionDescriptor vanillaTemporaryResistance(
            Identifier id,
            DamagePhase phase,
            Identifier channel,
            float value,
            String sourceLabel
    ) {
        return new DamageContributionDescriptor(
                id,
                DamageContributionSourceKind.VANILLA_BRIDGE,
                DamageContributionOperationKind.ADD_TEMPORARY_RESISTANCE,
                phase,
                Optional.ofNullable(channel),
                Optional.empty(),
                Optional.empty(),
                value,
                DamageContributionStatus.APPLIED,
                displayGroup(id, sourceLabel),
                Optional.empty(),
                Optional.of(displayName("Vanilla Resistance", sourceLabel)),
                tooltipLines(
                        "vanilla_temporary_resistance",
                        channel,
                        null,
                        null,
                        value,
                        sourceLabel
                ),
                true);
    }

    public static DamageContributionDescriptor vanillaMitigation(
            Identifier id,
            DamagePhase phase,
            Identifier channel,
            float value,
            String sourceLabel
    ) {
        return new DamageContributionDescriptor(
                id,
                DamageContributionSourceKind.VANILLA_BRIDGE,
                DamageContributionOperationKind.ADD_CHANNEL_MITIGATION,
                phase,
                Optional.ofNullable(channel),
                Optional.empty(),
                Optional.empty(),
                value,
                DamageContributionStatus.APPLIED,
                displayGroup(id, sourceLabel),
                Optional.empty(),
                Optional.of(displayName("Vanilla Mitigation", sourceLabel)),
                tooltipLines(
                        "vanilla_mitigation",
                        channel,
                        null,
                        null,
                        value,
                        sourceLabel
                ),
                true);
    }

    public static DamageContributionDescriptor vanillaArmorEffectiveness(
            Identifier id,
            DamagePhase phase,
            float value,
            String sourceLabel
    ) {
        return new DamageContributionDescriptor(
                id,
                DamageContributionSourceKind.VANILLA_BRIDGE,
                DamageContributionOperationKind.MULTIPLY_ARMOR_EFFECTIVENESS,
                phase,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                value,
                DamageContributionStatus.APPLIED,
                displayGroup(id, sourceLabel),
                Optional.empty(),
                Optional.of(displayName("Vanilla Armor Effectiveness", sourceLabel)),
                tooltipLines(
                        "vanilla_armor_effectiveness",
                        null,
                        null,
                        null,
                        value,
                        sourceLabel
                ),
                true);
    }

    private static Optional<Identifier> displayGroup(
            Identifier fallbackId,
            String sourceLabel
    ) {
        if (fallbackId == null) {
            return Optional.empty();
        }

        if (sourceLabel == null || sourceLabel.isBlank()) {
            return Optional.of(fallbackId);
        }

        return Optional.of(Identifier.fromNamespaceAndPath(
                fallbackId.getNamespace(),
                "vanilla/" + sanitize(sourceLabel)
        ));
    }

    private static Optional<String> displayName(
            DamageContributionSourceKind sourceKind,
            String sourceLabel
    ) {
        String base = switch (sourceKind) {
            case VANILLA_ENCHANTMENT -> "Vanilla Enchantment";
            case VANILLA_MOB_EFFECT -> "Vanilla Effect";
            case VANILLA_DAMAGE_TYPE -> "Vanilla Damage Type";
            case VANILLA_BRIDGE -> "Vanilla";
            default -> "Vanilla";
        };

        return Optional.of(displayName(base, sourceLabel));
    }

    private static String displayName(
            String base,
            String sourceLabel
    ) {
        if (sourceLabel == null || sourceLabel.isBlank()) {
            return base;
        }

        return base + " - " + readableSource(sourceLabel);
    }

    private static List<String> tooltipLines(
            String kind,
            Identifier channel,
            DamageApplicationBucket bucket,
            Identifier preMultiplierBucket,
            float value,
            String sourceLabel
    ) {
        List<String> lines = new ArrayList<>();

        lines.add("kind=" + kind);

        if (channel != null) {
            lines.add("channel=" + channel);
        }

        if (bucket != null) {
            lines.add("application_bucket=" + bucket);
        }

        if (preMultiplierBucket != null) {
            lines.add("pre_multiplier_bucket=" + preMultiplierBucket);
        }

        lines.add("value=" + value);

        if (sourceLabel != null && !sourceLabel.isBlank()) {
            lines.add("source=" + sourceLabel);
        }

        return List.copyOf(lines);
    }

    private static String readableSource(String sourceLabel) {
        String value = sourceLabel;

        int colon = value.indexOf(':');
        if (colon >= 0 && colon + 1 < value.length()) {
            value = value.substring(colon + 1);
        }

        return value
                .replace('/', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .strip();
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        String sanitized = value
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._/-]", "_")
                .replace('/', '_');

        if (sanitized.isBlank()) {
            return "unknown";
        }

        return sanitized;
    }

    public static DamageContributionDescriptor vanillaEnchantmentBase(
            Identifier id,
            DamagePhase phase,
            Identifier channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceLabel
    ) {
        return vanillaBase(
                id,
                DamageContributionSourceKind.VANILLA_ENCHANTMENT,
                phase,
                channel,
                bucket,
                value,
                sourceLabel
        );
    }

    public static DamageContributionDescriptor vanillaMobEffectBase(
            Identifier id,
            DamagePhase phase,
            Identifier channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceLabel
    ) {
        return vanillaBase(
                id,
                DamageContributionSourceKind.VANILLA_MOB_EFFECT,
                phase,
                channel,
                bucket,
                value,
                sourceLabel
        );
    }
}
