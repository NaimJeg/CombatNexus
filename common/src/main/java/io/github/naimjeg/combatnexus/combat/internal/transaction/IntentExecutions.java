package io.github.naimjeg.combatnexus.combat.internal.transaction;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.scheduling.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.*;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.*;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
import io.github.naimjeg.combatnexus.combat.internal.registration.IntentTypes;
public final class IntentExecutions {
    private IntentExecutions() {}
    public static CommandOutcome<IntentReceipt> submit(CombatRuntime world, DomainCommand.Intent command) {
        var retained = new ArrayList<CommandOutcome<?>>();
        CommandResult<Boolean> receipt = StateCommands.submit(world,command.requestId(),command.target(),CombatOperation.INTENT,Optional.of(command.source()),null,()->{
            var lineage=CombatExecutionScope.currentLineage(world.level().getServer()).orElseThrow();
            var allowed=EnumSet.noneOf(CombatOperation.class);allowed.addAll(IntentTypes.type(command.value()).permissions());allowed.add(CombatOperation.INTENT);
            lineage.restrictOperations(allowed);
            var execution=new Execution(world,command,retained);
            boolean completed=true;
            try { IntentTypes.execute(command.value(),execution); }
            catch (Stopped ignored) { completed=execution.results.stream().allMatch(CommandOutcome::completed); }
            catch (RuntimeException failure) {
                completed=false;
            } finally { execution.open=false; }
            completed &= execution.results.stream().allMatch(CommandOutcome::completed);
            var journal=new ApplicationJournal(execution.results.stream().flatMap(v->v.journal().entries().stream()).toList(),completed);
            boolean applied=completed&&execution.results.stream().allMatch(CommandOutcome::applied);
            var result=applied?CommandResult.applied(true):CommandResult.<Boolean>rejected(
                completed?CommandRejection.Code.NATIVE_REFUSED:CommandRejection.Code.INCOMPLETE_APPLICATION,"Intent child did not apply");
            return result.withJournal(journal);
        });
        return new CommandOutcome<>(IntentReceipt.class, Optional.of(new IntentReceipt(retained,receipt)), receipt.applied(),receipt.journal(),receipt.context(),receipt.rejection());
    }
    private static final class Stopped extends RuntimeException {}
    private static final class Execution implements IntentExecution {
        final CombatRuntime world;final DomainCommand.Intent command;final Thread owner=Thread.currentThread();
        final List<CommandOutcome<?>> results;boolean open=true;
        Execution(CombatRuntime world,DomainCommand.Intent command,List<CommandOutcome<?>> results){this.world=world;this.command=command;this.results=results;}
        void check(){if(!open||Thread.currentThread()!=owner)throw new IllegalStateException("Intent context expired");world.level();}
        public UUID target(){check();return command.target();}
        public long gameTick(){check();return world.level().getGameTime();}
        public CombatSource source(){check();return command.source();}
        public CommandOutcome<?> submit(DomainCommand value){
            check();var result=DomainCommandExecutor.execute(world,value);results.add(result);
            if(!result.completed()){open=false;throw new Stopped();}return result;
        }
        public CommandOutcome<?> damage(double amount,net.minecraft.resources.Identifier type){
            check();
            var result=DomainCommandExecutor.damage(world,target(),source(),amount,type);results.add(result);
            if(!result.completed()){open=false;throw new Stopped();}return result;
        }
    }
}
