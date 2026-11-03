package io.github.naimjeg.combatnexus.combat.damage.api.item.template;

import io.github.naimjeg.combatnexus.combat.damage.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.combatnexus.combat.damage.core.template.DamageTemplateRegistry;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Optional;

/**
 * Read-only public lookup facade for complete static templates.
 *
 * <p>The server registry is authoritative. Clients may only observe templates
 * available from their own Java registrations; a client miss says nothing
 * about whether the server can execute the reference.</p>
 */
public final class DamageModuleTemplates {
    private DamageModuleTemplates() {}

    /** Returns the immutable definition in the currently published revision. */
    public static Optional<DamageEntryDefinition> entry(net.minecraft.server.MinecraftServer server, Identifier id) {
        return io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions.forServer(server).damage().templates().entry(Objects.requireNonNull(id, "id"));
    }

    /** Returns the immutable definition in the currently published revision. */
    public static Optional<DamageAffixDefinition> affix(net.minecraft.server.MinecraftServer server, Identifier id) {
        return io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions.forServer(server).damage().templates().affix(Objects.requireNonNull(id, "id"));
    }

    /** Monotonic runtime revision. It is not a persistent or network ID. */
    public static long revision(net.minecraft.server.MinecraftServer server) {
        return io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions.forServer(server).generation();
    }

    /**
     * Reports whether the published definitions have completed strict
     * server-side validation against the current damage-channel content.
     * Lookup availability alone does not imply execution readiness.
     */
    public static boolean serverExecutionReady(net.minecraft.server.MinecraftServer server) {
        var definitions = io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions.forServer(server);
        return definitions.damage().templates().isCompatibleWith(definitions.damage().channels().contentRevision());
    }

    /**
     * Channel content revision validated by the current template snapshot,
     * or {@code -1} for the Java-only pre-reload snapshot.
     */
    public static long validatedChannelRevision(net.minecraft.server.MinecraftServer server) {
        return io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions.forServer(server).damage().templates().validatedChannelRevision();
    }
}
