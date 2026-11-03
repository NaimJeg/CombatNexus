package io.github.naimjeg.combatnexus.combat.api.scheduling;
import java.util.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.api.*;
/** Value-only scheduled commands; no Runnable, entity, callback or execution permit can be retained. */
public interface ScheduledIntent {
    Codec<ScheduledIntent> CODEC = io.github.naimjeg.combatnexus.combat.internal.registration.IntentTypes.CODEC;
    record Heal(double amount,io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest.Reason reason) implements ScheduledIntent {
        public static final MapCodec<Heal> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.DOUBLE.fieldOf("amount").forGetter(Heal::amount),Codec.STRING.xmap(io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest.Reason::valueOf,Enum::name).fieldOf("reason").forGetter(Heal::reason)).apply(i,Heal::new));
        public Heal { if(!Double.isFinite(amount)||amount<0)throw new IllegalArgumentException("Invalid scheduled healing");Objects.requireNonNull(reason); }

        public DomainCommand command(UUID target,CombatSource source){return new DomainCommand.Heal(new io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest(UUID.randomUUID(),target,source,reason,amount));}
    }
    record Effect(Identifier effect,int duration,int amplifier) implements ScheduledIntent {
        public static final MapCodec<Effect> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Identifier.CODEC.fieldOf("effect").forGetter(Effect::effect),Codec.INT.fieldOf("duration").forGetter(Effect::duration),Codec.INT.fieldOf("amplifier").forGetter(Effect::amplifier)).apply(i,Effect::new));
        public Effect { Objects.requireNonNull(effect);if(duration<1||amplifier<0)throw new IllegalArgumentException("Invalid scheduled effect"); }

        public DomainCommand command(UUID target,CombatSource source){return new DomainCommand.Effect(new io.github.naimjeg.combatnexus.effect.api.NativeEffectCommand(UUID.randomUUID(),target,source,effect,io.github.naimjeg.combatnexus.effect.api.NativeEffectCommand.Operation.APPLY,duration,amplifier,false,true,true));}
    }
    record Ignite(int ticks) implements ScheduledIntent {
        public static final MapCodec<Ignite> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.INT.fieldOf("ticks").forGetter(Ignite::ticks)).apply(i,Ignite::new));
        public Ignite { if(ticks<0)throw new IllegalArgumentException("Invalid scheduled ignition"); }

        public DomainCommand command(UUID target,CombatSource source){return new DomainCommand.Entity(new io.github.naimjeg.combatnexus.combat.action.api.EntityCommand.Ignite(UUID.randomUUID(),target,source,ticks));}
    }
    record Absorption(double amount,double capacity) implements ScheduledIntent {
        public static final MapCodec<Absorption> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.DOUBLE.fieldOf("amount").forGetter(Absorption::amount),Codec.DOUBLE.fieldOf("capacity").forGetter(Absorption::capacity)).apply(i,Absorption::new));
        public Absorption { if(!Double.isFinite(amount)||!Double.isFinite(capacity)||amount<0||capacity<0)throw new IllegalArgumentException("Invalid scheduled absorption"); }

        public DomainCommand command(UUID target,CombatSource source){return new DomainCommand.Absorption(new io.github.naimjeg.combatnexus.combat.absorption.api.AbsorptionCommand(UUID.randomUUID(),target,source,io.github.naimjeg.combatnexus.combat.absorption.api.AbsorptionCommand.Operation.GRANT,amount,capacity));}
    }
    record Damage(double amount, Identifier damageType) implements ScheduledIntent {
        public static final MapCodec<Damage> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.DOUBLE.fieldOf("amount").forGetter(Damage::amount),Identifier.CODEC.fieldOf("damage_type").forGetter(Damage::damageType)).apply(i,Damage::new));
        public Damage { if(!Double.isFinite(amount)||amount<0||amount>io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageRequest.MAX_BASE_DAMAGE)throw new IllegalArgumentException("Invalid scheduled damage");Objects.requireNonNull(damageType); }
    }
    record Shield(double amount, int duration) implements ScheduledIntent {
        public static final MapCodec<Shield> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.DOUBLE.fieldOf("amount").forGetter(Shield::amount),Codec.INT.fieldOf("duration").forGetter(Shield::duration)).apply(i,Shield::new));
        public Shield { if(!Double.isFinite(amount)||amount<=0||duration<1)throw new IllegalArgumentException("Invalid scheduled shield"); }
    }
}
