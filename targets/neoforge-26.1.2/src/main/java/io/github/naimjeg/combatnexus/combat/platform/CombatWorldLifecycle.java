package io.github.naimjeg.combatnexus.combat.platform;

import io.github.naimjeg.combatnexus.combat.api.platform.WorldLifecycleAccess;
import io.github.naimjeg.combatnexus.effect.api.EffectService;
import io.github.naimjeg.combatnexus.presentation.platform.CombatPresentationNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.IdentityHashMap;

/** Owns world sessions and invalidates all retained service references at unload. */
@EventBusSubscriber(modid = "combatnexus")
public final class CombatWorldLifecycle {
    private static final java.util.Set<ServerLevel> WORLDS = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private static WorldLifecycleAccess access;
    private CombatWorldLifecycle() {}
    public static WorldLifecycleAccess access() { return java.util.Objects.requireNonNull(access, "World adapter not registered"); }
    @SubscribeEvent public static void platform(io.github.naimjeg.combatnexus.combat.platform.api.CombatRegisterEvent event) {
        access = event.registrar().worldLifecycle();
    }
    @SubscribeEvent public static void register(io.github.naimjeg.combatnexus.combat.damage.platform.api.event.DamageModuleRegisterEvent event) {
        event.registrar().registerSettlementListener(net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus", "lifesteal"), 0, callback -> {
            access().damageSettled(callback);
        });
    }

    @SubscribeEvent public static void equipment(net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive() || !WORLDS.contains(level)) return;
        try {
            access().equipmentChanged(level, entity.getUUID(),
                    event.getSlot(), event.getTo());
        } catch (RuntimeException failure) {
        }
    }
    @SubscribeEvent public static void joined(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living && event.getLevel() instanceof ServerLevel level && WORLDS.contains(level)) {
            CombatPersistence.joined(living);
            access().trackEquipment(level, living.getUUID());
        }
    }
    @SubscribeEvent public static void load(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (WORLDS.contains(level)) return;
        access().load(level, new io.github.naimjeg.combatnexus.combat.api.platform.NativeWorldHooks() {
            public io.github.naimjeg.combatnexus.combat.api.platform.NativeHealingApplication healing() { return new WorldHealingPort(level); }
            public boolean mayExpire(io.github.naimjeg.combatnexus.effect.api.EffectInstance effect) {
                return io.github.naimjeg.combatnexus.effect.platform.NativeEffectBridge.mayExpire(level, effect);
            }
            public void effectCommitted(EffectService.Change change) {
                io.github.naimjeg.combatnexus.effect.platform.NativeEffectBridge.committed(level, change);
            }
        });
        WORLDS.add(level);
        for (var entity : level.getAllEntities()) if (entity instanceof LivingEntity living) {
            CombatPersistence.joined(living);
            access().trackEquipment(level, entity.getUUID());
        }
        access().beginTick(level);
    }
    /** Before scheduled commands can cause gameplay writes at the newly incremented time. */
    public static void clockAdvanced(ServerLevel level) {
        access().beginTick(level);
    }
    /** After native time advancement, before block/entity ticks; also runs for frozen dimensions. */
    public static void tickReady(ServerLevel level) {
            access().beginTick(level);
            CombatPersistence.tick(level);
            access().tickRecovery(level);
            CombatPresentationNetwork.flushControls(level);
            CombatPresentationNetwork.flushPanels(level);
    }
    private static void clear(net.minecraft.world.entity.Entity entity, EffectService.Removal reason) {
        if (entity.level() instanceof ServerLevel level) clear(entity, reason, level);
    }
    private static void clear(net.minecraft.world.entity.Entity entity, EffectService.Removal reason, ServerLevel level) {
        if (entity instanceof LivingEntity living) CombatPersistence.leaving(living, level, reason);
        if (entity instanceof net.minecraft.server.level.ServerPlayer player) CombatPresentationNetwork.clear(player);
        access().clear(level, entity.getUUID(), reason);
    }
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            var reason = event.getEntity().getRemovalReason() == net.minecraft.world.entity.Entity.RemovalReason.CHANGED_DIMENSION
                ? EffectService.Removal.DIMENSION_CHANGE : EffectService.Removal.WORLD_EXIT;
            clear(event.getEntity(), reason, level);
        }
    }
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) { if (!event.isCanceled()) clear(event.getEntity(), EffectService.Removal.DEATH); }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            CombatPersistence.copyTransfer(event.getOriginal(), event.getEntity());
            return;
        }
        clear(event.getOriginal(), EffectService.Removal.RESPAWN);
        clear(event.getEntity(), EffectService.Removal.RESPAWN);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) CombatPresentationNetwork.clear(player);
        if (event.getEntity().level() instanceof ServerLevel level && WORLDS.contains(level)) {
            CombatPersistence.joined(event.getEntity());
            access().trackEquipment(level, event.getEntity().getUUID());
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { clear(event.getEntity(), EffectService.Removal.WORLD_EXIT); }
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        for (var level : java.util.List.copyOf(WORLDS)) {
            if (level.getServer() == event.getServer()) {
                CombatPersistence.close(level);
                CombatPresentationNetwork.clear(level);
                WORLDS.remove(level);
                access().close(level);
            }
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            CombatPersistence.close(level);
            CombatPresentationNetwork.clear(level);
            WORLDS.remove(level);
            access().close(level);
        }
    }
}
