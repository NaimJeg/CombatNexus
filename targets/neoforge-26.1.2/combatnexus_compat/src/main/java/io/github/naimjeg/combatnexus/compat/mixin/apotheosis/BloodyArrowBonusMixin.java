package io.github.naimjeg.combatnexus.compat.mixin.apotheosis;

import java.util.*;
import dev.shadowsoffire.apotheosis.socket.gem.*;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.special.BloodyArrowBonus;
import dev.shadowsoffire.apothic_attributes.api.AbilityCooldowns;
import io.github.naimjeg.combatnexus.compat.apotheosis.GemSources;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.*;
import io.github.naimjeg.combatnexus.combat.projectile.api.ProjectileChange;
import io.github.naimjeg.combatnexus.combat.state.api.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=BloodyArrowBonus.class, remap=false)
public abstract class BloodyArrowBonusMixin {
    @Shadow @Final protected Map<Purity,BloodyArrowBonus.Data> values;
    @Inject(method="onProjectileFired",at=@At("HEAD"),cancellable=true,require=1)
    private void combatnexus$launch(GemInstance instance, LivingEntity user, Projectile projectile, CallbackInfo callback) {
        if(!(user.level() instanceof ServerLevel level) || !(projectile instanceof AbstractArrow)) return;
        var found=CombatAccess.find(level); if(found.isEmpty()) return;
        callback.cancel();
        var world=found.orElseThrow();
        try(var call=io.github.naimjeg.combatnexus.compat.BridgeCalls.replacing(world,"apotheosis:bloody_arrow",user.getUUID(),projectile.getUUID())) {
        var data=values.get(instance.purity());
        var source=new CombatSource(GemSources.identity(instance),Optional.of(user.getUUID()),Optional.empty());
        int cooldown=AbilityCooldowns.applyCDR(user,data.cooldown());
        if(AbilityCooldowns.isOnCooldown(user,source.id(),data.cooldown()) || world.cooldowns().remaining(user.getUUID(),source.id(),cooldown)>0) { call.returnedNormally(); return; }
        var capture=world.projectiles().capture(UUID.randomUUID(),user.getUUID(),projectile);
        if(!capture.applied()) return;
        var launch=capture.value().orElseThrow();
        float cost=launch.shooter().facts().maxHealth()*data.healthCost();
        var damage=level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(BloodyArrowBonus.CORRUPTED)
                .map(DamageSource::new).orElseGet(()->user.damageSources().magic());
        // Cost, arrow write and cooldown share the launch parent's admission and derivation budget.
        var plan=new BeforeApplicationPlan(List.of(
                new BeforeApplicationPlan.Step(new DomainCommand.Damage(UUID.randomUUID(),DamageRequest.builder(level,user,DamageSourceDescriptor.from(damage),cost).build()),BeforeApplicationPlan.RejectionPolicy.CONTINUE),
                new BeforeApplicationPlan.Step(new DomainCommand.Projectile(new ProjectileChange(UUID.randomUUID(),user.getUUID(),source,projectile,launch.action(),data.dmgMultiplier())),BeforeApplicationPlan.RejectionPolicy.ABORT_PARENT),
                new BeforeApplicationPlan.Step(new DomainCommand.Cooldown(new CooldownCommand(UUID.randomUUID(),user.getUUID(),source,CooldownCommand.Operation.START,CooldownStamp.DeathPolicy.RETAIN)),BeforeApplicationPlan.RejectionPolicy.ABORT_PARENT)));
        world.projectiles().execute(UUID.randomUUID(),user.getUUID(),projectile,launch.action(),plan);
        call.returnedNormally();
        }
    }
}
