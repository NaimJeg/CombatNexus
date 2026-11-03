package io.github.naimjeg.combatnexus.combat.internal.coordination;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
/** Samples belong to an actual native action/launch lifetime, never a guessed tick grouping. */
public final class DecisionSamples {
    private record Key(Identifier policy,Identifier decision,Optional<UUID> target) {}
    private final Map<UUID,Map<Key,Float>> actions=new HashMap<>();
    public void open(UUID action){if(actions.size()>=4096)throw new IllegalStateException("Active decision action capacity");if(actions.putIfAbsent(action,new HashMap<>())!=null)throw new IllegalArgumentException("Action already active");}
    public void close(UUID action){actions.remove(action);}
    public float sample(UUID action,Identifier policy,Identifier decision,Optional<UUID> target,Supplier<Float> random){
        var values=actions.get(action);if(values==null)throw new IllegalStateException("Shared decision requires an active native action or launch");
        var key=new Key(policy,decision,target);var previous=values.get(key);if(previous!=null)return previous;
        if(values.size()>=256)throw new IllegalStateException("Shared decision capacity");float sample=random.get();values.put(key,sample);return sample;
    }
}
