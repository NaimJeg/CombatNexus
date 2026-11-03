package io.github.naimjeg.combatnexus.combat.internal.transaction;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.*;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.*;
import io.github.naimjeg.combatnexus.combat.healing.api.*;
import io.github.naimjeg.combatnexus.combat.internal.*;
/** Domain adapters preserve full receipts; admission stays in domain services. */
public final class DomainCommandExecutor {
    private DomainCommandExecutor() {}
    public static CommandOutcome<?> damage(CombatRuntime world,UUID targetId,CombatSource source,double amount,net.minecraft.resources.Identifier type) {
        return damage(world, targetId, source, amount, type, 0);
    }
    public static CommandOutcome<?> damage(CombatRuntime world,UUID targetId,CombatSource source,double amount,net.minecraft.resources.Identifier type,float minimumHealth) {
        if(!Double.isFinite(amount)||amount<0||amount>DamageRequest.MAX_BASE_DAMAGE)throw new IllegalArgumentException("Invalid damage");
        var target=(net.minecraft.world.entity.LivingEntity)world.level().getEntity(targetId);
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,type);
        var builder=DamageRequest.builder(world.level(),target,DamageSourceDescriptor.of(key),(float)amount).kind(DamageRequestKind.CUSTOM).triggerPolicy(DamageTriggerPolicy.NONE_ALLOWED).minimumHealth(minimumHealth);
        source.owner().map(world.level()::getEntity).filter(net.minecraft.world.entity.LivingEntity.class::isInstance).map(net.minecraft.world.entity.LivingEntity.class::cast).ifPresent(builder::logicalAttacker);
        return execute(world,new DomainCommand.Damage(UUID.randomUUID(),builder.build()));
    }
    public static CommandOutcome<?> execute(CombatRuntime world, DomainCommand command) {
        var services = new WorldServices(world);
        try {
            return switch (command) {
                case DomainCommand.Heal c -> {
                    var v = services.healing().submit(c.request());
                    var journal = v.outcome() == HealingSettlement.Outcome.PARTIAL || v.outcome() == HealingSettlement.Outcome.UNKNOWN
                        ? new ApplicationJournal(v.journal().entries(), false) : v.journal();
                    yield new CommandOutcome<>(HealingSettlement.class, Optional.of(v), v.outcome() == HealingSettlement.Outcome.APPLIED, journal, v.context(), v.rejection());
                }
                case DomainCommand.Damage c -> {
                    var v = services.damage(c.request());
                    var identity = v.lineage().transaction();
                    var context = new CommandContext(c.requestId(), c.request().target().getUUID(), CombatOperation.DAMAGE, Optional.empty(),
                        Optional.of(new CommandContext.Identity(identity.id(), identity.rootId(), identity.parentId(), identity.depth())));
                    boolean applied = v.status() == DamageSubmissionStatus.APPLIED;
                    var rejection = applied ? Optional.<CommandRejection>empty() : Optional.of(new CommandRejection(
                        v.status() == DamageSubmissionStatus.FAILED ? CommandRejection.Code.INCOMPLETE_APPLICATION : CommandRejection.Code.NATIVE_REFUSED,
                        v.failure().map(Object::toString).orElse("Damage refused")));
                    var journal = v.settlement().map(s -> s.evidence().journal()).orElse(new ApplicationJournal(List.of(), v.status() != DamageSubmissionStatus.FAILED));
                    yield new CommandOutcome<>(DamageResult.class, Optional.of(v), applied, journal, Optional.of(context), rejection);
                }
                case DomainCommand.Effect c -> state(services.effects().nativeEffect(c.request()));
                case DomainCommand.Absorption c -> state(services.absorption().submit(c.request()));
                case DomainCommand.CorrectHealth c -> state(services.health().correct(c.request()));
                case DomainCommand.Cooldown c -> state(services.cooldowns().submit(c.request()));
                case DomainCommand.Projectile c -> state(services.projectiles().modify(c.request()));
                case DomainCommand.Entity c -> state(services.entities().submit(c.request()));
                case DomainCommand.GrantShield c -> state(services.shields().grant(c.requestId(), c.target(), c.grant()));
                case DomainCommand.Regeneration c -> state(services.regeneration().replace(c.requestId(), c.plan()));
                case DomainCommand.RevokeSource c -> state(services.sources().revoke(c.requestId(), c.handle()));
                case DomainCommand.Lethal c -> state(services.health().execute(c.request()));
                case DomainCommand.ManagedEffect c -> {
                    var v = services.effects().apply(c.request());
                    yield new CommandOutcome<>(io.github.naimjeg.combatnexus.effect.api.EffectResult.class, Optional.of(v), v.rejection().isEmpty(), v.journal(), v.context(), v.rejection());
                }
                case DomainCommand.InstallSource c -> {
                    var v = services.sources().install(c.request());
                    yield new CommandOutcome<>(io.github.naimjeg.combatnexus.combat.api.source.SourceResult.class, Optional.of(v), v.applied(), v.journal(), v.context(), v.rejection());
                }
                case DomainCommand.Intent c -> IntentExecutions.submit(world, c);
            };
        } catch (RuntimeException failure) {
            return CommandOutcome.rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "Command incomplete: " + failure.getMessage());
        }
    }
    private static CommandOutcome<CommandResult> state(CommandResult<?> value) {
        return new CommandOutcome<>(CommandResult.class, Optional.of(value), value.applied(), value.journal(), value.context(), value.rejection());
    }
}
