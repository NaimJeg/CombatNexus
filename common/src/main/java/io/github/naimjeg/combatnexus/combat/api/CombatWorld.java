package io.github.naimjeg.combatnexus.combat.api;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame;
import io.github.naimjeg.combatnexus.combat.api.source.SourceService;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.*;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingService;
import io.github.naimjeg.combatnexus.combat.shield.api.ShieldCommands;
import io.github.naimjeg.combatnexus.combat.regeneration.api.RegenerationCommands;
import io.github.naimjeg.combatnexus.effect.api.EffectCommands;
/** Server-thread business capabilities for an existing world session. No lifecycle or application protocol. */
public interface CombatWorld {
    UUID session();
    Optional<CombatInputFrame> facts(UUID target);
    Optional<io.github.naimjeg.combatnexus.combat.api.action.CombatAction> currentAction(UUID actor);
    List<io.github.naimjeg.combatnexus.combat.api.source.SourceDescription> sourceDescriptions(CombatInputFrame input);
    DefinitionView definitions();
    Optional<DefinitionDescription> definition(net.minecraft.resources.Identifier id, DefinitionDescription.Kind kind);
    io.github.naimjeg.combatnexus.combat.api.source.SourceResult install(io.github.naimjeg.combatnexus.combat.api.source.DefinitionInstallation command);
    DamageResult damage(DamageRequest request);
    HealingService healing();
    io.github.naimjeg.combatnexus.combat.api.scheduling.CombatScheduler scheduler();
    io.github.naimjeg.combatnexus.combat.state.api.RuntimeStates states();
    io.github.naimjeg.combatnexus.combat.action.api.EntityCommands entities();
    io.github.naimjeg.combatnexus.combat.api.bridge.BridgeCalls bridges();
    io.github.naimjeg.combatnexus.combat.state.api.CooldownCommands cooldowns();
    io.github.naimjeg.combatnexus.combat.projectile.api.ProjectileCommands projectiles();
    Optional<BeforeApplicationPlan.Result> beforeApplication(UUID invocation, UUID target, BeforeApplicationPlan plan);
    io.github.naimjeg.combatnexus.combat.absorption.api.AbsorptionCommands absorption();
    io.github.naimjeg.combatnexus.combat.api.bridge.DamageFormulaResult damageFormula(io.github.naimjeg.combatnexus.combat.api.bridge.DamageFormulaRequest request);
    io.github.naimjeg.combatnexus.combat.health.api.HealthCommands health();
    EffectCommands effects();
    ShieldCommands shields();
    RegenerationCommands regeneration();
    SourceService sources();
}
