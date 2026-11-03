package io.github.naimjeg.combatnexus.combat.state.internal;
import java.util.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.naimjeg.combatnexus.combat.api.scheduling.ScheduledTask;
import io.github.naimjeg.combatnexus.combat.state.api.StateKey;
public final class EntityScheduleStore {
    private final Map<UUID,ScheduledTask> tasks=new LinkedHashMap<>();
    public List<ScheduledTask> snapshot(){return List.copyOf(tasks.values());}
    public void add(ScheduledTask task){if(tasks.containsKey(task.id())||tasks.size()>=64)throw new IllegalArgumentException("Duplicate scheduled identity/capacity");tasks.put(task.id(),task);}
    public boolean remove(UUID id){return tasks.remove(id)!=null;}
    public void clear(StateKey.Boundary boundary,Optional<UUID> installation){tasks.values().removeIf(t->t.clearOn().contains(boundary)&&(installation.isEmpty()||t.installation().equals(installation)));}
    public String encode(boolean persistentOnly){return ScheduledTask.CODEC.listOf().encodeStart(JsonOps.INSTANCE,tasks.values().stream().filter(t->!persistentOnly||t.persistent()).toList()).getOrThrow().toString();}
    public void restore(String encoded){if(encoded.length()>1048576)throw new IllegalArgumentException("Schedule save capacity");var restored=ScheduledTask.CODEC.listOf().parse(JsonOps.INSTANCE,JsonParser.parseString(encoded)).getOrThrow();var next=new EntityScheduleStore();restored.forEach(next::add);tasks.clear();tasks.putAll(next.tasks);}
}
