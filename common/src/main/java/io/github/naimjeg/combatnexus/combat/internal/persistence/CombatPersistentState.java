package io.github.naimjeg.combatnexus.combat.internal.persistence;

import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.effect.api.EffectInstance;
import io.github.naimjeg.combatnexus.combat.internal.coordination.WorldBehaviorSources.SavedBehavior;
import io.github.naimjeg.combatnexus.combat.internal.coordination.EquipmentBehaviorController.SavedEquipment;
import io.github.naimjeg.combatnexus.combat.shield.internal.DefaultShieldService.SavedShield;
import io.github.naimjeg.combatnexus.combat.regeneration.internal.DefaultRegenerationScheduler.SavedRecovery;

/** Detached data only. Runtime identities, tickets, and callbacks are never saved. */
public record CombatPersistentState(UUID target, long savedTick, Set<Identifier> persistentSources,
        List<EffectInstance> effects, List<SavedBehavior> behaviors, List<SavedShield> shields,
        List<SavedRecovery> recovery, List<SavedEquipment> equipment, List<io.github.naimjeg.combatnexus.combat.internal.coordination.SourceInstallations.Saved> sources) {
    public CombatPersistentState {
        if (savedTick < 0) throw new IllegalArgumentException("Invalid saved tick");
        Objects.requireNonNull(target); sources = List.copyOf(sources);
        if (sources.size() > 64) throw new IllegalArgumentException("Source capacity exceeded");
        persistentSources = Set.copyOf(persistentSources); effects = List.copyOf(effects); behaviors = List.copyOf(behaviors);
        shields = List.copyOf(shields); recovery = List.copyOf(recovery); equipment = List.copyOf(equipment);
        if (persistentSources.size() > 64 || effects.size() > 256 || behaviors.size() > 64
                || shields.size() > 256 || recovery.size() > 256 || equipment.size() > 64)
            throw new IllegalArgumentException("Combat save capacity exceeded");
        if (effects.stream().anyMatch(e -> !e.snapshot().target().equals(target) || e.ownership() != EffectInstance.Ownership.MANAGED)
                || recovery.stream().anyMatch(e -> !e.plan().target().equals(target)))
            throw new IllegalArgumentException("Foreign or observed persistent state");
    }
}
