package io.github.naimjeg.combatnexus.combat.health.platform.mixin;

import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import io.github.naimjeg.combatnexus.combat.state.api.CooldownStamp;
import io.github.naimjeg.combatnexus.combat.state.spi.NativeCooldownTarget;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCooldownMixin implements NativeCooldownTarget {
    @Shadow protected boolean dead;
    @Unique private static final Codec<Map<Identifier,CooldownStamp>> combatnexus$codec = Codec.unboundedMap(Identifier.CODEC, CooldownStamp.CODEC);
    @Unique private final Map<Identifier,CooldownStamp> combatnexus$cooldowns = new LinkedHashMap<>();
    @Unique private String combatnexus$invalidCooldowns;
    public Map<Identifier,CooldownStamp> combatnexus$cooldowns() {
        if (combatnexus$invalidCooldowns != null) throw new IllegalStateException("Cooldown restoration failed");
        return combatnexus$cooldowns;
    }
    @Inject(method="die", at=@At("TAIL"), require=1)
    private void combatnexus$deathCooldowns(net.minecraft.world.damagesource.DamageSource source, CallbackInfo ci) {
        if(dead && combatnexus$invalidCooldowns==null) combatnexus$cooldowns.values().removeIf(s -> s.deathPolicy()==CooldownStamp.DeathPolicy.CLEAR);
    }
    @Inject(method="addAdditionalSaveData", at=@At("TAIL"), require=1)
    private void combatnexus$saveCooldowns(ValueOutput output, CallbackInfo ci) {
        output.putString("combatnexus_cooldowns", combatnexus$invalidCooldowns != null ? combatnexus$invalidCooldowns : combatnexus$codec.encodeStart(JsonOps.INSTANCE, combatnexus$cooldowns).getOrThrow().toString());
    }
    @Inject(method="readAdditionalSaveData", at=@At("HEAD"), require=1)
    private void combatnexus$readCooldowns(ValueInput input, CallbackInfo ci) {
        String encoded=input.getString("combatnexus_cooldowns").orElse("{}");
        combatnexus$cooldowns.clear(); combatnexus$invalidCooldowns=null;
        try { combatnexus$cooldowns.putAll(combatnexus$codec.parse(JsonOps.INSTANCE, JsonParser.parseString(encoded)).getOrThrow()); }
        catch (RuntimeException failure) {
            combatnexus$invalidCooldowns=encoded;
        }
    }
}
