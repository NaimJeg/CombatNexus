package io.github.naimjeg.combatnexus.combat.damage.api;

import io.github.naimjeg.combatnexus.CombatNexusCommon;
import net.minecraft.resources.Identifier;

/**
 * Central factory for identifiers owned by DamageModule itself.
 *
 * <p>External mods must preserve their own namespace by constructing their
 * complete {@link Identifier}; this helper must not be used to rewrite external IDs.</p>
 */
public final class DamageModuleIds {

    private DamageModuleIds() {
    }

    /** Creates a {@code combatnexus:*} identifier for a built-in resource only. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CombatNexusCommon.MOD_ID, path);
    }
}
