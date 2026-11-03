package io.github.naimjeg.combatnexus.combat.action.internal;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import io.github.naimjeg.combatnexus.combat.action.api.EntityCommand;
import io.github.naimjeg.combatnexus.combat.api.*;
public final class EntityApplication {
    private EntityApplication() {}
    public static CommandResult<Boolean> apply(LivingEntity target,EntityCommand command) {
        var writes=new ArrayList<ApplicationJournal.Entry>();
        var resource=command instanceof EntityCommand.Ignite?ApplicationJournal.Resource.FIRE:((EntityCommand.Displace)command).mode()==EntityCommand.Displace.Mode.POSITION?ApplicationJournal.Resource.POSITION:ApplicationJournal.Resource.VELOCITY;
        try {
            switch(command) {
                case EntityCommand.Ignite fire -> {
                    int before=target.getRemainingFireTicks();target.setRemainingFireTicks(Math.max(before,fire.ticks()));
                    writes.add(ApplicationJournal.Entry.write(ApplicationJournal.Resource.FIRE,command.source().id(),before,target.getRemainingFireTicks(),Math.max(0,target.getRemainingFireTicks()-before),true));
                }
                case EntityCommand.Displace movement -> {
                    var before=movement.mode()==EntityCommand.Displace.Mode.POSITION?target.position():target.getDeltaMovement();
                    var after=before.add(movement.delta());
                    if(!Double.isFinite(after.x)||!Double.isFinite(after.y)||!Double.isFinite(after.z))return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Displacement overflow");
                    if(movement.mode()==EntityCommand.Displace.Mode.POSITION) target.teleportTo(after.x,after.y,after.z);
                    else { target.setDeltaMovement(after);target.hurtMarked=true; }
                    var actual=movement.mode()==EntityCommand.Displace.Mode.POSITION?target.position():target.getDeltaMovement();
                    writes.add(ApplicationJournal.Entry.write(resource,command.source().id(),before.x,actual.x,Math.abs(actual.x-before.x),true));
                    writes.add(ApplicationJournal.Entry.write(resource,command.source().id(),before.y,actual.y,Math.abs(actual.y-before.y),true));
                    writes.add(ApplicationJournal.Entry.write(resource,command.source().id(),before.z,actual.z,Math.abs(actual.z-before.z),true));
                }
            }
            return CommandResult.applied(true).withJournal(new ApplicationJournal(writes,true));
        } catch(RuntimeException failure) {
            writes.add(new ApplicationJournal.Entry(resource,Optional.of(command.source().id()),OptionalDouble.empty(),OptionalDouble.empty(),0,ApplicationJournal.Observation.UNKNOWN,false));
            return CommandResult.<Boolean>rejected(CommandRejection.Code.INCOMPLETE_APPLICATION,"Entity write incomplete; no retry").withJournal(new ApplicationJournal(writes,false));
        }
    }
}
