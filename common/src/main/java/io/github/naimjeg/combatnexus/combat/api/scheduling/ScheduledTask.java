package io.github.naimjeg.combatnexus.combat.api.scheduling;
import java.util.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;
import io.github.naimjeg.combatnexus.combat.state.api.StateKey;
public record ScheduledTask(UUID id,CombatSource source,Optional<UUID> installation,long nextTick,int interval,int repetitions,ScheduledIntent intent,Set<StateKey.Boundary> clearOn,boolean persistent) {
    private static final Codec<UUID> UUID_CODEC=Codec.STRING.xmap(UUID::fromString,UUID::toString);
    private static final Codec<CombatSource> SOURCE=RecordCodecBuilder.create(i->i.group(Identifier.CODEC.fieldOf("id").forGetter(CombatSource::id),UUID_CODEC.optionalFieldOf("owner").forGetter(CombatSource::owner)).apply(i,(id,owner)->new CombatSource(id,owner,Optional.empty())));
    public static final Codec<ScheduledTask> CODEC=RecordCodecBuilder.create(i->i.group(
            UUID_CODEC.fieldOf("id").forGetter(ScheduledTask::id),SOURCE.fieldOf("source").forGetter(ScheduledTask::source),UUID_CODEC.optionalFieldOf("installation").forGetter(ScheduledTask::installation),
            Codec.LONG.fieldOf("next").forGetter(ScheduledTask::nextTick),Codec.INT.fieldOf("interval").forGetter(ScheduledTask::interval),Codec.INT.fieldOf("repetitions").forGetter(ScheduledTask::repetitions),
            ScheduledIntent.CODEC.fieldOf("intent").forGetter(ScheduledTask::intent),Codec.STRING.xmap(StateKey.Boundary::valueOf,Enum::name).listOf().xmap(Set::copyOf,List::copyOf).fieldOf("clear_on").forGetter(ScheduledTask::clearOn),Codec.BOOL.fieldOf("persistent").forGetter(ScheduledTask::persistent)
    ).apply(i,ScheduledTask::new));
    public ScheduledTask { Objects.requireNonNull(id);Objects.requireNonNull(source);Objects.requireNonNull(installation);Objects.requireNonNull(intent);clearOn=Set.copyOf(clearOn);if(nextTick<0||interval<1||repetitions<1)throw new IllegalArgumentException("Invalid task timing"); }
}
