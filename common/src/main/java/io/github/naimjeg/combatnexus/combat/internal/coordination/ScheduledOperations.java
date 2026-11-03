package io.github.naimjeg.combatnexus.combat.internal.coordination;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.scheduling.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.*;
import io.github.naimjeg.combatnexus.combat.internal.transaction.*;
import io.github.naimjeg.combatnexus.combat.state.spi.NativeSchedules;
/** Called only by the native entity tick. Uses the shared kernel and completion FIFO. */
public final class ScheduledOperations {
    private ScheduledOperations() {}
    public static void tick(LivingEntity entity) {
        if(!entity.isAlive()||!(entity.level() instanceof ServerLevel level))return;
        var kernel=CombatServerRuntime.existing(level.getServer());if(kernel.isEmpty())return;
        var world=kernel.get().world(level);if(world.isEmpty())return;
        var store=((NativeSchedules)entity).combatnexus$schedules();
        for(var task:store.snapshot()) {
            if(task.nextTick()>level.getGameTime())continue;
            StateCommands.submit(world.get(),UUID.randomUUID(),entity.getUUID(),CombatOperation.STATE,Optional.of(task.source()),null,()->{
                if(!store.remove(task.id()))return CommandResult.rejected(CommandRejection.Code.NOT_FOUND,"Schedule already consumed");

                var result=BeforeApplicationExecutor.execute(world.get(),new BeforeApplicationPlan(List.of(new BeforeApplicationPlan.Step(new DomainCommand.Intent(UUID.randomUUID(),entity.getUUID(),task.source(),task.intent()),BeforeApplicationPlan.RejectionPolicy.CONTINUE))));
                if(result.completed()&&task.repetitions()>1&&entity.isAlive())store.add(new ScheduledTask(task.id(),task.source(),task.installation(),Math.addExact(level.getGameTime(),task.interval()),task.interval(),task.repetitions()-1,task.intent(),task.clearOn(),task.persistent()));
                return CommandResult.applied(result).withJournal(new ApplicationJournal(result.steps().stream().flatMap(s->s.journal().entries().stream()).toList(),result.completed()));
            });
        }
    }
}
