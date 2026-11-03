package io.github.naimjeg.combatnexus.effect.internal;

import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import io.github.naimjeg.combatnexus.effect.api.*;
import io.github.naimjeg.combatnexus.combat.api.*;

/** Uses vanilla application/removal; never installs another set of attribute modifiers or advances duration. */
public final class NativeEffects {
    private NativeEffects() {}
    public static CommandResult<NativeEffectReceipt> apply(LivingEntity target, NativeEffectCommand command) {
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase(
                io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.EFFECT);
        var holder = BuiltInRegistries.MOB_EFFECT.get(command.effect());
        if (holder.isEmpty()) return CommandResult.rejected(CommandRejection.Code.NOT_FOUND, "Unknown native effect");
        var effect = holder.orElseThrow();
        var before = state(target.getEffect(effect));
        boolean accepted;
        try {
            accepted = command.operation() == NativeEffectCommand.Operation.REMOVE ? target.removeEffect(effect)
                    : target.addEffect(new MobEffectInstance(effect, command.durationTicks(), command.amplifier(), command.ambient(), command.visible(), command.icon()),
                            command.source().owner().map(((net.minecraft.server.level.ServerLevel)target.level())::getEntity).orElse(null));
        } catch (RuntimeException failure) {
            return CommandResult.<NativeEffectReceipt>rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "Native effect application incomplete; do not retry")
                    .withJournal(journal(command, before, state(target.getEffect(effect)), false));
        }
        var after = state(target.getEffect(effect));
        if (!accepted) return CommandResult.<NativeEffectReceipt>rejected(CommandRejection.Code.NATIVE_REFUSED, "Native effect operation declined")
                .withJournal(journal(command, before, after, true));
        return CommandResult.applied(new NativeEffectReceipt(before, after, accepted)).withJournal(journal(command, before, after, true));
    }
    private static Optional<NativeEffectReceipt.State> state(MobEffectInstance effect) {
        return effect == null ? Optional.empty() : Optional.of(new NativeEffectReceipt.State(effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
    }
    private static ApplicationJournal journal(NativeEffectCommand command, Optional<NativeEffectReceipt.State> before,
            Optional<NativeEffectReceipt.State> after, boolean complete) {
        return new ApplicationJournal(List.of(new ApplicationJournal.Entry(ApplicationJournal.Resource.EFFECT,
                Optional.of(command.source().id()), OptionalDouble.of(before.map(s -> s.durationTicks()).orElse(0)),
                OptionalDouble.of(after.map(s -> s.durationTicks()).orElse(0)), 0,
                complete ? ApplicationJournal.Observation.WHOLE_CALL : ApplicationJournal.Observation.UNKNOWN, complete)), complete);
    }
}
