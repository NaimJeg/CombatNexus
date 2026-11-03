package io.github.naimjeg.combatnexus.combat.platform;

import io.github.naimjeg.combatnexus.combat.api.action.CombatActionGate;
import static io.github.naimjeg.combatnexus.effect.api.EffectSnapshot.RestrictedAction.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

/** Native gameplay origins; command teleport and dimension transfer keep their native authority. */
@EventBusSubscriber(modid = "combatnexus")
public final class CombatControlEvents {
    private CombatControlEvents() {}
    @SubscribeEvent public static void projectileLeft(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event){
        if(!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level))return;
        CombatWorldLifecycle.access().projectileLeft(level, event.getEntity());
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void collision(net.neoforged.neoforge.event.entity.ProjectileImpactEvent event) {
        if(event.isCanceled() || !(event.getProjectile().level() instanceof net.minecraft.server.level.ServerLevel level))return;
        var hit = event.getRayTraceResult();
        var subject = hit instanceof net.minecraft.world.phys.EntityHitResult entityHit
            ? java.util.Optional.of(entityHit.getEntity() instanceof net.neoforged.neoforge.entity.PartEntity<?> part
                ? part.getParent().getUUID() : entityHit.getEntity().getUUID()) : java.util.Optional.<java.util.UUID>empty();
        CombatWorldLifecycle.access().collision(level, event.getProjectile(), hit, subject);
    }
    @SubscribeEvent public static void projectile(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && !event.loadedFromDisk()
                && event.getEntity() instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner
                && !CombatActionGate.allows(owner, ATTACK)) event.setCanceled(true);
    }
    @SubscribeEvent public static void teleport(EntityTeleportEvent event) {
        if ((event instanceof EntityTeleportEvent.EnderEntity || event instanceof EntityTeleportEvent.EnderPearl
                || event instanceof EntityTeleportEvent.ItemConsumption)
                && event.getEntity() instanceof LivingEntity living && !CombatActionGate.allows(living, SKILL_MOVEMENT))
            event.setCanceled(true);
    }
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void captureLaunch(EntityJoinLevelEvent event) {
        if (!event.isCanceled() && !event.loadedFromDisk() && event.getLevel() instanceof net.minecraft.server.level.ServerLevel level
                && event.getEntity() instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner)
            io.github.naimjeg.combatnexus.combat.api.CombatAccess.find(level).ifPresent(world ->
                    world.projectiles().capture(java.util.UUID.randomUUID(), owner.getUUID(), projectile));
    }
    @SubscribeEvent public static void cloneCooldowns(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof net.minecraft.server.level.ServerPlayer original
                && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer replacement)
            CombatWorldLifecycle.access().cloneState(original, replacement, event.isWasDeath());
    }
    @SubscribeEvent public static void stateDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
            CombatWorldLifecycle.access().changedDimension(player);
    }
}
