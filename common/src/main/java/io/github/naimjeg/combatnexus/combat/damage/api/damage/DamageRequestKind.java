package io.github.naimjeg.combatnexus.combat.damage.api.damage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Framework-level classification for a damage request.
 *
 * <p>These values describe how damage entered the framework. They do not
 * define skills, equipment, or proc behavior. The framework treats a content
 * mod's declaration as trusted and does not guess a kind from item or skill
 * semantics.</p>
 */
public enum DamageRequestKind {
    PRIMARY,
    SWEEP,
    PROC,
    DOT,
    REFLECTED,
    THORNS,
    ENVIRONMENTAL,
    CUSTOM;

    /** Stable lowercase data representation; enum ordinals are never used. */
    public static final Codec<DamageRequestKind> CODEC =
            Codec.STRING.comapFlatMap(
                    DamageRequestKind::decode,
                    DamageRequestKind::serializedName
            );

    public String serializedName() {
        return switch (this) {
            case PRIMARY -> "primary";
            case SWEEP -> "sweep";
            case PROC -> "proc";
            case DOT -> "dot";
            case REFLECTED -> "reflected";
            case THORNS -> "thorns";
            case ENVIRONMENTAL -> "environmental";
            case CUSTOM -> "custom";
        };
    }

    public String translationKey() {
        return "damage_request_kind.combatnexus." + serializedName();
    }

    private static DataResult<DamageRequestKind> decode(String value) {
        return switch (value) {
            case "primary" -> DataResult.success(PRIMARY);
            case "sweep" -> DataResult.success(SWEEP);
            case "proc" -> DataResult.success(PROC);
            case "dot" -> DataResult.success(DOT);
            case "reflected" -> DataResult.success(REFLECTED);
            case "thorns" -> DataResult.success(THORNS);
            case "environmental" -> DataResult.success(ENVIRONMENTAL);
            case "custom" -> DataResult.success(CUSTOM);
            default -> DataResult.error(() ->
                    "Unknown DamageModule request kind: " + value
            );
        };
    }
}
