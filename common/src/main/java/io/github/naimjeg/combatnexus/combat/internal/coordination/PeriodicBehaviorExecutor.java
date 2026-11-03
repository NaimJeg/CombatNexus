package io.github.naimjeg.combatnexus.combat.internal.coordination;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;

import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.behavior.*;
import io.github.naimjeg.combatnexus.combat.api.scheduling.DueQueue;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatFailures;
import io.github.naimjeg.combatnexus.combat.healing.api.*;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.*;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/** All periodic behaviors share installation identity, deadlines, cancellation and saved remaining time. */
final class PeriodicBehaviorExecutor {
    private record Key(long installation, Identifier type) {}
    private record Work(BehaviorSource source, PeriodicBehavior behavior) { Key key() { return new Key(source.installation(), behavior.type()); } }
    private final CombatRuntime world;
    private final Map<Key, Long> tasks = new HashMap<>(), deadlines = new HashMap<>();
    private final DueQueue<Work> queue = new DueQueue<>(4096);
    PeriodicBehaviorExecutor(CombatRuntime world) { this.world = world; }
    void validate(Collection<BehaviorSource> sources, long now) {
        long count = 0;
        for (var source : sources) for (var component : source.plan().components()) if (component instanceof PeriodicBehavior periodic) {
            if (periodic.intervalTicks() < 1 || periodic.intervalTicks() > 1200000) throw new IllegalArgumentException("Periodic interval out of range");
            if (periodic.initialDelayTicks() < 0 || periodic.initialDelayTicks() > 1200000) throw new IllegalArgumentException("Initial delay out of range");
            Math.addExact(now, periodic.intervalTicks()); count++;
        }
        if (count > 4096) throw new IllegalStateException("Behavior task capacity exceeded");
    }
    private void schedule(Work work, long due) {
        if (tasks.containsKey(work.key()) || work.source().expiresAtTick().isPresent() && due >= work.source().expiresAtTick().getAsLong()) return;
        deadlines.put(work.key(), due); tasks.put(work.key(), queue.schedule(due, work));
    }
    void install(BehaviorSource source, long now) {
        for (var component : source.plan().components()) if (component instanceof PeriodicBehavior periodic)
            schedule(new Work(source, periodic), Math.addExact(now,
                    periodic.phase().firstDelay(now, source.expiresAtTick(), periodic.intervalTicks(), periodic.initialDelayTicks())));
    }
    void refresh(BehaviorSource source, Map<Identifier, Long> remaining, long now, boolean preserveAll) {
        for (var component : source.plan().components()) if (component instanceof PeriodicBehavior periodic
                && (preserveAll || periodic.phase() == PeriodicPhase.PRESERVE) && remaining.containsKey(component.type())) {
            var work = new Work(source, periodic);
            var task = tasks.remove(work.key());
            if (task != null) queue.cancel(task);
            deadlines.remove(work.key());
            schedule(work, Math.addExact(now, remaining.get(component.type())));
        }
    }
    void cancel(long installation) {
        for (var key : List.copyOf(tasks.keySet())) if (key.installation() == installation) { queue.cancel(tasks.remove(key)); deadlines.remove(key); }
    }
    void tick(long now, Predicate<BehaviorSource> valid) {
        for (var task : queue.poll(now)) {
            var work = task.value(); var source = work.source(); tasks.remove(work.key()); deadlines.remove(work.key());
            if (!valid.test(source) || !(world.level().getEntity(source.target()) instanceof LivingEntity living) || !living.isAlive()) continue;
            long delay = work.behavior().intervalTicks();
            if (work.behavior().phase() == PeriodicPhase.REMAINING_DURATION) {
                delay = work.behavior().phase().firstDelay(now, source.expiresAtTick(), work.behavior().intervalTicks(), (int) delay);
                if (delay == 0) delay = work.behavior().intervalTicks();
            }
            schedule(work, Math.addExact(now, delay));
            // A skipped world tick consumes the missed modulo trigger without moving
            // the effect onto a different phase or issuing a catch-up command.
            if (work.behavior().phase() == PeriodicPhase.REMAINING_DURATION && source.expiresAtTick().isPresent()
                    && (source.expiresAtTick().getAsLong() - now) % work.behavior().intervalTicks() != 0) continue;
            var execution = new Execution(source, valid);
            try { work.behavior().execute(execution); }
            catch (Throwable failure) {
                CombatFailures.rethrowFatal(failure);
            } finally { execution.open = false; }
        }
    }
    private final class Execution implements PeriodicExecution {
        final BehaviorSource source; final Predicate<BehaviorSource> valid; boolean open = true;
        Execution(BehaviorSource source, Predicate<BehaviorSource> valid) { this.source = source; this.valid = valid; }
        private void check() { world.level(); if (!open || !valid.test(source)) throw new IllegalStateException("Periodic command capability expired"); }
        public BehaviorSource source() { check(); return source; }
        public void environmentDamage(io.github.naimjeg.combatnexus.effect.api.EnvironmentDamage parameters) {
            check();
            io.github.naimjeg.combatnexus.effect.internal.EnvironmentEffects.scheduled(
                (LivingEntity)world.level().getEntity(source.target()), source.effect(), parameters);
        }
        public void effectCapability(Identifier capability) {
            check();
            var effect = world.effects().snapshot(source.target()).stream().filter(e -> e.snapshot().effect().equals(source.effect())
                    && e.snapshot().source().equals(source.source())).findFirst().orElseThrow();
            var entity = (LivingEntity) world.level().getEntity(source.target());
            if (!io.github.naimjeg.combatnexus.effect.internal.EffectCapabilities.execute(capability,
                    new io.github.naimjeg.combatnexus.effect.api.EffectCapability.Context(entity, effect,
                        io.github.naimjeg.combatnexus.effect.api.EffectCapability.Trigger.TICK, null, 0)))
                world.effects().remove(source.target(), source.effect(), source.source(), io.github.naimjeg.combatnexus.effect.api.EffectService.Removal.CONDITION_ENDED);
        }
        public io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame input() {
            check();
            return new io.github.naimjeg.combatnexus.combat.internal.WorldServices(world).facts(source.target()).orElseThrow();
        }
        public io.github.naimjeg.combatnexus.combat.api.CommandOutcome<?> execute(io.github.naimjeg.combatnexus.combat.api.scheduling.ScheduledIntent intent) {
            check();return io.github.naimjeg.combatnexus.combat.internal.transaction.DomainCommandExecutor.execute(world,
                new io.github.naimjeg.combatnexus.combat.api.DomainCommand.Intent(UUID.randomUUID(),source.target(),
                    new io.github.naimjeg.combatnexus.combat.api.CombatSource(source.source(),source.owner(),Optional.empty()),intent));
        }
        public HealingSettlement heal(double amount) {
            check();
            var request=new HealingRequest(UUID.randomUUID(),source.target(),
                new io.github.naimjeg.combatnexus.combat.api.CombatSource(source.source(),source.owner(),Optional.empty()),HealingRequest.Reason.EFFECT,amount);
            return io.github.naimjeg.combatnexus.combat.internal.transaction.DomainCommandExecutor.execute(world,
                new io.github.naimjeg.combatnexus.combat.api.DomainCommand.Heal(request)).result(HealingSettlement.class).orElseThrow();
        }
        public DamageResult damage(double amount, Identifier damageType) {
            return damage(amount, damageType, 0);
        }
        public DamageResult damage(double amount, Identifier damageType, float minimumHealth) {
            check();
            return io.github.naimjeg.combatnexus.combat.internal.transaction.DomainCommandExecutor.damage(world,source.target(),
                new io.github.naimjeg.combatnexus.combat.api.CombatSource(source.source(),source.owner(),Optional.empty()),amount,damageType,minimumHealth).result(DamageResult.class).orElseThrow();
        }
    }

    CommandResult<Boolean> instant(BehaviorSource source) {
        var results = new ArrayList<CommandOutcome<?>>();
        var execution = new InstantExecution() {
            boolean open = true;
            private void check() { world.level(); if (!open) throw new IllegalStateException("Instant capability expired"); }
            public BehaviorSource source() { check(); return source; }
            public CommandOutcome<?> damage(double amount, Identifier type, Optional<UUID> direct) {
                check();
                var entity = (LivingEntity)world.level().getEntity(source.target());
                var holder = world.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, type));
                var nativeSource = new net.minecraft.world.damagesource.DamageSource(holder,
                    direct.map(world.level()::getEntity).orElse(null), source.owner().map(world.level()::getEntity).orElse(null));
                return submit(new DomainCommand.Damage(UUID.randomUUID(), DamageRequest.builder(world.level(), entity,
                    DamageSourceDescriptor.from(nativeSource), (float)amount).build()));
            }
            public CommandOutcome<?> submit(DomainCommand command) {
                check();
                var result = io.github.naimjeg.combatnexus.combat.internal.transaction.DomainCommandExecutor.execute(world, command);
                results.add(result);
                if (!result.completed()) { open = false; throw new IllegalStateException("Instant command incomplete"); }
                return result;
            }
        };
        boolean complete = true;
        try {
            for (var component : source.plan().components()) ((InstantBehavior) component).execute(execution);
        } catch (RuntimeException failure) {
            complete = false;
        } finally { execution.open = false; }
        var journal = new ApplicationJournal(results.stream().flatMap(r -> r.journal().entries().stream()).toList(),
                complete && results.stream().allMatch(CommandOutcome::completed));
        var outcome = complete && results.stream().allMatch(CommandOutcome::applied) ? CommandResult.applied(true)
                : CommandResult.<Boolean>rejected(complete ? CommandRejection.Code.NATIVE_REFUSED : CommandRejection.Code.INCOMPLETE_APPLICATION,
                    "Instant effect child did not apply");
        return outcome.withJournal(journal);
    }

    Map<Identifier, Long> remaining(long installation, long now) {
        var result = new HashMap<Identifier, Long>();
        deadlines.forEach((key, deadline) -> { if (key.installation() == installation) result.put(key.type(), Math.max(0, deadline - now)); });
        return Map.copyOf(result);
    }
    void restore(BehaviorSource source, Map<Identifier, Long> remaining, long now) {
        cancel(source.installation());
        for (var component : source.plan().components()) if (component instanceof PeriodicBehavior periodic && remaining.containsKey(component.type())) {
            long delay = remaining.get(component.type());
            if (delay < 0 || delay > 1200000) throw new IllegalArgumentException("Invalid saved periodic deadline");
            schedule(new Work(source, periodic), Math.addExact(now, delay));
        }
    }
    void clear() { deadlines.clear(); tasks.clear(); queue.clear(); }
}
