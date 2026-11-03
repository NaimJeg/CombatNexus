package io.github.naimjeg.combatnexus.combat.internal.registration;
import java.util.*;
import com.mojang.serialization.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.api.scheduling.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation;
public final class IntentTypes {
    private static final Map<Identifier,IntentType<?>> TYPES = new LinkedHashMap<>();
    private static final Map<Class<?>,IntentType<?>> CLASSES = new HashMap<>();
    private static boolean frozen;
    public static final Codec<ScheduledIntent> CODEC = Identifier.CODEC.dispatch("type", v -> type(v).id(), id -> {
        var t=TYPES.get(id); if(t==null)throw new IllegalArgumentException("Unknown intent: "+id); return t.codec();
    });
    private IntentTypes() {}
    public static void builtins() {
        add("healing",ScheduledIntent.Heal.class,ScheduledIntent.Heal.CODEC,CombatOperation.HEALING,(v,c)->c.submit(v.command(c.target(),c.source())));
        add("effect",ScheduledIntent.Effect.class,ScheduledIntent.Effect.CODEC,CombatOperation.EFFECT,(v,c)->c.submit(v.command(c.target(),c.source())));
        add("ignition",ScheduledIntent.Ignite.class,ScheduledIntent.Ignite.CODEC,CombatOperation.IGNITION,(v,c)->c.submit(v.command(c.target(),c.source())));
        add("absorption",ScheduledIntent.Absorption.class,ScheduledIntent.Absorption.CODEC,CombatOperation.ABSORPTION,(v,c)->c.submit(v.command(c.target(),c.source())));
        add("damage",ScheduledIntent.Damage.class,ScheduledIntent.Damage.CODEC,CombatOperation.DAMAGE,(v,c)->c.damage(v.amount(),v.damageType()));
        add("shield",ScheduledIntent.Shield.class,ScheduledIntent.Shield.CODEC,CombatOperation.SHIELD,(v,c)->c.submit(new io.github.naimjeg.combatnexus.combat.api.DomainCommand.GrantShield(UUID.randomUUID(),c.target(),
            new io.github.naimjeg.combatnexus.combat.shield.api.ShieldGrant(c.source(),new io.github.naimjeg.combatnexus.combat.shield.api.ShieldDefinition(v.amount(),0,Set.of(),Set.of(),0,0,io.github.naimjeg.combatnexus.combat.shield.api.ShieldDefinition.Stacking.REPLACE_SAME_SOURCE),v.amount(),OptionalLong.of(Math.addExact(c.gameTick(),v.duration()))))));
    }
    private static <T extends ScheduledIntent> void add(String id,Class<T> cls,MapCodec<T> codec,CombatOperation permission,java.util.function.BiConsumer<T,IntentExecution> executor) {
        register(new IntentType<>(Identifier.fromNamespaceAndPath("combatnexus",id),cls,codec,Set.of(permission),executor));
    }
    public static <T extends ScheduledIntent> void register(IntentType<T> t) {
        if(frozen)throw new IllegalStateException("Intent registry frozen");
        if(TYPES.containsKey(t.id())||CLASSES.containsKey(t.implementation()))throw new IllegalArgumentException("Duplicate intent: "+t.id());
        TYPES.put(t.id(),t);CLASSES.put(t.implementation(),t);
    }
    public static void freeze(){frozen=true;}
    public static IntentType<?> type(ScheduledIntent v){return Objects.requireNonNull(CLASSES.get(v.getClass()),"Unregistered intent");}
    public static void execute(ScheduledIntent v,IntentExecution context){execute(type(v),v,context);}
    private static <T extends ScheduledIntent> void execute(IntentType<T> t,ScheduledIntent v,IntentExecution context){t.executor().accept(t.implementation().cast(v),context);}
}
