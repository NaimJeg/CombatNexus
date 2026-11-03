package io.github.naimjeg.combatnexus.combat.internal.definition;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime;

import io.github.naimjeg.combatnexus.combat.config.CombatServerSettings;
import io.github.naimjeg.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.naimjeg.combatnexus.combat.damage.core.rule.DatapackDamageRuleStore;
import net.minecraft.server.MinecraftServer;
import java.util.Objects;
import io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration;

/** One publication boundary for the currently adopted server resource generation. */
public final class CombatDefinitions {
    public static DefinitionGeneration forServer(MinecraftServer server) {
        return io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.of(server)
                .definitions().orElseThrow(() -> new IllegalStateException("Server definitions unavailable"));
    }
    private static final java.util.concurrent.atomic.AtomicLong GENERATIONS = new java.util.concurrent.atomic.AtomicLong();
    public static long nextGeneration() { return GENERATIONS.incrementAndGet(); }
    private CombatDefinitions() {}
    public static synchronized void publish(MinecraftServer server, DefinitionGeneration candidate) {
        if (!server.isSameThread()) throw new IllegalStateException("Definition publication requires server thread");
        if (io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.of(server).definitions().orElse(null) == candidate) return;
        var previous = io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.of(server).definitions();
        if (previous.isPresent() && candidate.generation() <= previous.get().generation())
            throw new IllegalArgumentException("Adopted generations must increase");
        io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.of(server).adoptDefinitions(candidate);
        io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageModulePipeline.clearCache();
        io.github.naimjeg.combatnexus.combat.damage.core.security.DamageModuleItemSecurity.invalidateExecutionCache();
    }
    public static synchronized void clear(MinecraftServer server) {
        io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.existing(server).ifPresent(runtime -> runtime.clearDefinitions());
    }
}
