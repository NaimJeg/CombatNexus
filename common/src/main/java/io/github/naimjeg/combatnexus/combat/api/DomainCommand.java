package io.github.naimjeg.combatnexus.combat.api;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest;
/** Immediate typed requests. Serializable composition is provided by registered intents. */
public sealed interface DomainCommand {
    default net.minecraft.resources.Identifier type() {
        return switch(this) {
            case Intent i -> io.github.naimjeg.combatnexus.combat.internal.registration.IntentTypes.type(i.value()).id();
            case Heal ignored -> net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus","healing");
            case Damage ignored -> net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus","damage");
            case Effect ignored -> net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus","native_effect");
            case Absorption ignored -> net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus","absorption");
            case CorrectHealth ignored -> net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus","health_correction");
            case Cooldown ignored -> net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus","cooldown");
            case Projectile ignored -> net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus","projectile");
            case Entity ignored -> net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus","entity");
            case ManagedEffect ignored -> net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus","managed_effect");
            case GrantShield ignored -> net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus","shield");
            case Regeneration ignored -> net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus","regeneration");
            case InstallSource ignored -> net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus","source_install");
            case RevokeSource ignored -> net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus","source_revoke");
            case Lethal ignored -> net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus","lethal");
        };
    }

    public record Entity(io.github.naimjeg.combatnexus.combat.action.api.EntityCommand request) implements DomainCommand { public Entity { Objects.requireNonNull(request); } }
    public record Damage(UUID requestId, io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageRequest request) implements DomainCommand { public Damage { Objects.requireNonNull(requestId);Objects.requireNonNull(request); } }
    public record Projectile(io.github.naimjeg.combatnexus.combat.projectile.api.ProjectileChange request) implements DomainCommand { public Projectile { Objects.requireNonNull(request); } }
    public record Effect(io.github.naimjeg.combatnexus.effect.api.NativeEffectCommand request) implements DomainCommand { public Effect { Objects.requireNonNull(request); } }
    public record Absorption(io.github.naimjeg.combatnexus.combat.absorption.api.AbsorptionCommand request) implements DomainCommand { public Absorption { Objects.requireNonNull(request); } }
    public record CorrectHealth(io.github.naimjeg.combatnexus.combat.health.api.HealthCorrection request) implements DomainCommand { public CorrectHealth { Objects.requireNonNull(request); } }
    public record Cooldown(io.github.naimjeg.combatnexus.combat.state.api.CooldownCommand request) implements DomainCommand { public Cooldown { Objects.requireNonNull(request); } }
    public record Heal(HealingRequest request) implements DomainCommand {
        public Heal { Objects.requireNonNull(request); }
    }
    record ManagedEffect(io.github.naimjeg.combatnexus.effect.api.EffectApplication request) implements DomainCommand { public ManagedEffect { Objects.requireNonNull(request); } }
    record GrantShield(UUID requestId, UUID target, io.github.naimjeg.combatnexus.combat.shield.api.ShieldGrant grant) implements DomainCommand { public GrantShield { Objects.requireNonNull(requestId); Objects.requireNonNull(target); Objects.requireNonNull(grant); } }
    record Regeneration(UUID requestId, io.github.naimjeg.combatnexus.combat.regeneration.api.RegenerationPlan plan) implements DomainCommand { public Regeneration { Objects.requireNonNull(requestId); Objects.requireNonNull(plan); } }
    record InstallSource(io.github.naimjeg.combatnexus.combat.api.source.SourceInstallation request) implements DomainCommand { public InstallSource { Objects.requireNonNull(request); } }
    record RevokeSource(UUID requestId, io.github.naimjeg.combatnexus.combat.api.source.InstallationHandle handle) implements DomainCommand { public RevokeSource { Objects.requireNonNull(requestId); Objects.requireNonNull(handle); } }
    record Lethal(io.github.naimjeg.combatnexus.combat.health.api.LethalAttempt request) implements DomainCommand { public Lethal { Objects.requireNonNull(request); } }
    record Intent(UUID requestId, UUID target, CombatSource source, io.github.naimjeg.combatnexus.combat.api.scheduling.ScheduledIntent value) implements DomainCommand { public Intent { Objects.requireNonNull(requestId); Objects.requireNonNull(target); Objects.requireNonNull(source); Objects.requireNonNull(value); } }
}
