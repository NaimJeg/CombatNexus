package io.github.naimjeg.combatnexus.combat.health.internal;

import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.health.api.*;
import io.github.naimjeg.combatnexus.combat.api.platform.NativeHealthTarget;
import io.github.naimjeg.combatnexus.combat.spi.NativeResourceWrites;
import io.github.naimjeg.combatnexus.combat.spi.CombatBehaviorAccess.ProtectionDecision;

/** Vanilla application only; policy and transaction admission are owned by the coordinator. */
public final class NativeHealthApplication {
    private NativeHealthApplication() {}
    public static CommandResult<Float> correct(LivingEntity target, HealthCorrection command) {
        float before = target.getHealth();
        if (command.health() <= 0 || command.reason() == HealthCorrection.Reason.LIFE_COST && command.health() > before)
            return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT, "Correction must be nonlethal; a life cost cannot increase health");
        var writes = new ArrayList<ApplicationJournal.Entry>();
        boolean complete = write(target, command.source(), before, (float)Math.min(target.getMaxHealth(), command.health()), writes);
        return (complete ? CommandResult.applied(target.getHealth()) : CommandResult.<Float>rejected(
                CommandRejection.Code.INCOMPLETE_APPLICATION, "Health correction incomplete; do not retry"))
                .withJournal(new ApplicationJournal(writes, complete));
    }
    public static CommandResult<LethalReceipt> execute(LivingEntity target, LethalAttempt command,
            DamageSource damageSource, ProtectionDecision managed) {
        float before = target.getHealth();
        var writes = new ArrayList<ApplicationJournal.Entry>();
        managed.consumption().ifPresent(writes::add);
        try {
            if (managed.consumption().isPresent()) {
                if (!write(target, command.source(), before, managed.health(), writes)) return incomplete(writes);
                return result(target, before, LethalReceipt.Outcome.MANAGED_PROTECTED, writes);
            }
            var nativeTarget = (NativeHealthTarget)target;
            // The native protection boundary owns item consumption, effects and its own health write.
            boolean protectedByNative;
            try { protectedByNative = nativeTarget.combatnexus$nativeProtection(damageSource); }
            finally { writes.addAll(nativeTarget.combatnexus$protectionEvidence()); }
            if (protectedByNative) {
                writes.add(new ApplicationJournal.Entry(ApplicationJournal.Resource.HEALTH, Optional.empty(),
                        OptionalDouble.of(before), OptionalDouble.of(target.getHealth()), 0, ApplicationJournal.Observation.WHOLE_CALL, true));
                return result(target, before, LethalReceipt.Outcome.NATIVE_PROTECTED, writes);
            }
            if (target.isRemoved() || !target.isAlive()) return result(target, before, LethalReceipt.Outcome.DEATH_REFUSED, writes);
            nativeTarget.combatnexus$deathSound();
            if (damageSource.getEntity() instanceof LivingEntity attacker) {
                target.setLastHurtByMob(attacker);
                if (attacker instanceof Player player) target.setLastHurtByPlayer(player, 100);
            }
            target.getCombatTracker().recordDamage(damageSource, before);
            if (!write(target, command.source(), before, 0, writes)) return incomplete(writes);
            target.die(damageSource);
            return result(target, before, nativeTarget.combatnexus$deathConfirmed()
                    ? LethalReceipt.Outcome.DEATH_CONFIRMED : LethalReceipt.Outcome.DEATH_REFUSED, writes);
        } catch (RuntimeException failure) {
            return incomplete(writes);
        }
    }
    private static CommandResult<LethalReceipt> incomplete(List<ApplicationJournal.Entry> writes) {
        return CommandResult.<LethalReceipt>rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "Lethal application incomplete; do not retry")
                .withJournal(new ApplicationJournal(writes, false));
    }
    private static CommandResult<LethalReceipt> result(LivingEntity target, float before, LethalReceipt.Outcome outcome, List<ApplicationJournal.Entry> writes) {
        return CommandResult.applied(new LethalReceipt(outcome, before, target.getHealth())).withJournal(new ApplicationJournal(writes, true));
    }
    private static boolean write(LivingEntity target, CombatSource source, float before, float next, List<ApplicationJournal.Entry> writes) {
        int index = writes.size();
        writes.add(new ApplicationJournal.Entry(ApplicationJournal.Resource.HEALTH, Optional.of(source.id()), OptionalDouble.of(before),
                OptionalDouble.empty(), 0, ApplicationJournal.Observation.UNKNOWN, false));
        try (var token = NativeResourceWrites.health(target)) {
            boolean complete = false;
            try { target.setHealth(next); complete = true; }
            catch (RuntimeException failure) {}
            if (token.uncontended()) writes.set(index, ApplicationJournal.Entry.write(ApplicationJournal.Resource.HEALTH,
                    source.id(), before, target.getHealth(), Math.abs(before - target.getHealth()), complete));
            return complete && token.uncontended();
        }
    }
}
