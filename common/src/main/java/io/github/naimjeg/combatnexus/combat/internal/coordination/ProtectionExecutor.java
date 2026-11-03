package io.github.naimjeg.combatnexus.combat.internal.coordination;

import io.github.naimjeg.combatnexus.combat.api.ApplicationJournal;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
import io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorSource;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.naimjeg.combatnexus.combat.spi.CombatBehaviorAccess.ProtectionDecision;
import java.util.*;

/** Owns one-use protection policy and its installation-local consumption. */
final class ProtectionExecutor {
    private final Map<Long, Map<net.minecraft.resources.Identifier, Integer>> consumed = new HashMap<>();
    boolean available(BehaviorSource source) { return source.plan().components().stream().anyMatch(c -> c instanceof io.github.naimjeg.combatnexus.combat.api.behavior.ProtectionBehavior p && consumed(source.installation()).getOrDefault(c.type(), 0) < p.maximumUses()); }
    Map<net.minecraft.resources.Identifier, Integer> consumed(long installation) { return Map.copyOf(consumed.getOrDefault(installation, Map.of())); }
    void restore(long installation, Map<net.minecraft.resources.Identifier, Integer> values) { consumed.put(installation, new HashMap<>(values)); }
    void remove(long installation) { consumed.remove(installation); }
    void clear() { consumed.clear(); }

    ProtectionDecision decide(CombatRuntime world, WorldBehaviorSources sources, UUID target,
            List<BehaviorSource> captured, float before, float proposed, io.github.naimjeg.combatnexus.combat.api.behavior.ReactionInputs inputs, java.util.Optional<io.github.naimjeg.combatnexus.combat.api.CombatSource> origin) {
        var server = CombatServerRuntime.of(world.level().getServer());
        if (!CombatExecutionScope.isCurrentPhase(CombatExecutionScope.Phase.APPLICATION))
            throw new IllegalStateException("Protection requires admitted application");
        var lineage = CombatExecutionScope.currentLineage(world.level().getServer()).orElseThrow();
        if (lineage.operation() != io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.DAMAGE
                && lineage.operation() != io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.LETHAL)
            throw new IllegalStateException("Protection belongs to damage application");
        server.ticket(lineage).bindSession(world.level(), target);
        if (captured.size() > 64 || captured.stream().anyMatch(s -> !s.target().equals(target) || !s.session().equals(world.sessionId())))
            throw new IllegalArgumentException("Foreign protection input");
        if (before <= 0 || !Float.isFinite(proposed) || proposed >= before)
            return new ProtectionDecision(proposed, Optional.empty());
        for (var source : captured) {
            if (!sources.valid(source) || !available(source)) continue;
            for (var component : source.plan().components()) if (component instanceof io.github.naimjeg.combatnexus.combat.api.behavior.ProtectionBehavior protection
                    && consumed(source.installation()).getOrDefault(component.type(), 0) < protection.maximumUses()) {
                float health;
                try (var compute = CombatExecutionScope.computation()) {
                    var decision = protection.protect(new io.github.naimjeg.combatnexus.combat.api.behavior.ProtectionContext(target,before,proposed,lineage.operation(),origin,inputs,source));
                    health = decision.isPresent() ? (float)decision.getAsDouble() : proposed;
                    if (!Float.isFinite(health) || health < proposed || health > before) throw new IllegalArgumentException("Protection cannot increase health beyond pre-write health");
                }
                catch (RuntimeException failure) { continue; }
                if (health == proposed) continue;
                consumed.computeIfAbsent(source.installation(), ignored -> new HashMap<>()).merge(component.type(), 1, Integer::sum);
                return new ProtectionDecision(health, Optional.of(ApplicationJournal.Entry.write(ApplicationJournal.Resource.PROTECTION, source.source(), 1, 0, 1, true)));
            }
        }
        return new ProtectionDecision(proposed, Optional.empty());
    }
}
