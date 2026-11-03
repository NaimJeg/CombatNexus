package io.github.naimjeg.combatnexus.effect.platform;

import java.util.*;
import java.util.function.*;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gamerules.GameRules;
import io.github.naimjeg.combatnexus.effect.api.EnvironmentEffectDefinition;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Environmental facts, including native immunity and game-rule inputs. No timers are installed here. */
@EventBusSubscriber(modid = "combatnexus")
public final class EnvironmentEffectAdapters {
    public static final List<String> NAMES = List.of("burning", "suffocation", "cramming", "drowning", "freezing", "starvation",
        "fire_contact", "lava_contact", "campfire_contact", "hot_floor_contact", "cactus_contact", "berry_contact", "drying", "water_sensitive", "world_border");
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "combatnexus");
    static { for (var name : NAMES) EFFECTS.register(name, () -> new MobEffect(MobEffectCategory.HARMFUL, 0xCE6A42) {}); }
    private EnvironmentEffectAdapters() {}
    public static void register(IEventBus bus) { EFFECTS.register(bus); }
    private static boolean touches(LivingEntity e, Block block) {
        var box = e.getBoundingBox().inflate(0.001);
        for (var pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY - 0.05, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ)))
            if (e.level().getBlockState(pos).is(block)) {
                var state = e.level().getBlockState(pos);
                if (block instanceof CampfireBlock && !state.getValue(CampfireBlock.LIT)) continue;
                if (block instanceof SweetBerryBushBlock && state.getValue(SweetBerryBushBlock.AGE) == 0) continue;
                return true;
            }
        return false;
    }
    private static boolean fire(LivingEntity e) { return !e.fireImmune() && !e.hasEffect(MobEffects.FIRE_RESISTANCE); }
    @SubscribeEvent public static void register(io.github.naimjeg.combatnexus.combat.platform.api.CombatRegisterEvent event) {
        for (var name : NAMES) {
            var id = Identifier.fromNamespaceAndPath("combatnexus", name); NativeEffectBridge.registerProjection(id);
            event.registrar().effectDisplay(id, new io.github.naimjeg.combatnexus.effect.api.EffectDisplay(id, false, false, true, null, false, name.equals("burning"), true));
            event.registrar().effectCapability(id, context -> true);
        }
        event.registrar().environmentEffect(new EnvironmentEffectDefinition(Identifier.parse("combatnexus:burning"), Identifier.parse("minecraft:on_fire"),
            e -> !e.fireImmune() && e.getRemainingFireTicks() > 0, e -> 0, true));
        add(event, "suffocation", "in_wall", LivingEntity::isInWall, e -> 0);
        add(event, "cramming", "cramming", e -> {
            int max = ((ServerLevel)e.level()).getGameRules().get(GameRules.MAX_ENTITY_CRAMMING);
            return max > 0 && e.level().getPushableEntities(e, e.getBoundingBox()).stream().filter(v -> !v.isPassenger()).count() >= max;
        }, e -> 0);
        add(event, "water_sensitive", "drown", e -> e.isSensitiveToWater() && e.isInWaterOrRain(), e -> 0);
        add(event, "drowning", "drown", e -> e.getAirSupply() <= 0 && e.isUnderWater() && !MobEffectUtil.hasWaterBreathing(e), e -> 0);
        add(event, "freezing", "freeze", e -> e.canFreeze() && e.getTicksFrozen() > 0, e -> 0);
        add(event, "starvation", "starve", e -> e instanceof Player p && p.getFoodData().getFoodLevel() <= 0,
            e -> e.level().getDifficulty() == Difficulty.HARD ? 0 : e.level().getDifficulty() == Difficulty.NORMAL ? 1 : 10);
        add(event, "campfire_contact", "campfire", e -> fire(e) && (touches(e, Blocks.CAMPFIRE) || touches(e, Blocks.SOUL_CAMPFIRE)), e -> 0);
        add(event, "fire_contact", "in_fire", e -> fire(e) && (touches(e, Blocks.FIRE) || touches(e, Blocks.SOUL_FIRE)), e -> 0);
        add(event, "lava_contact", "lava", e -> fire(e) && e.isInLava(), e -> 0);
        add(event, "hot_floor_contact", "hot_floor", e -> fire(e) && !e.isSteppingCarefully() && touches(e, Blocks.MAGMA_BLOCK), e -> 0);
        add(event, "cactus_contact", "cactus", e -> touches(e, Blocks.CACTUS), e -> 0);
        add(event, "berry_contact", "sweet_berry_bush", e -> !e.is(EntityType.FOX) && !e.is(EntityType.BEE) && touches(e, Blocks.SWEET_BERRY_BUSH), e -> 0);
        add(event, "drying", "dry_out", e -> e instanceof net.minecraft.world.entity.animal.dolphin.Dolphin && !e.isInWaterOrRain(), e -> 0);
        add(event, "world_border", "outside_border", e -> e instanceof Player && !e.isInWall()
            && !e.level().getWorldBorder().isWithinBounds(e.getBoundingBox())
            && e.level().getWorldBorder().getDistanceToBorder(e) + e.level().getWorldBorder().getSafeZone() < 0
            && e.level().getWorldBorder().getDamagePerBlock() > 0, e -> 0);
    }
    private static void add(io.github.naimjeg.combatnexus.combat.platform.api.CombatRegisterEvent event, String name, String damage,
            Predicate<LivingEntity> condition, ToDoubleFunction<LivingEntity> floor) {
        ToDoubleFunction<LivingEntity> amount = switch (name) {
            case "starvation" -> entity -> entity.level().getDifficulty() == Difficulty.PEACEFUL ? 0 : 1;
            case "cramming" -> entity -> 6;
            case "drowning", "water_sensitive" -> entity -> 2;
            case "lava_contact" -> entity -> 4;
            case "campfire_contact" -> entity -> touches(entity, Blocks.SOUL_CAMPFIRE) ? 2 : 1;
            case "world_border" -> entity -> Math.max(1, Math.floor(-(entity.level().getWorldBorder().getDistanceToBorder(entity)
                + entity.level().getWorldBorder().getSafeZone()) * entity.level().getWorldBorder().getDamagePerBlock()));
            default -> entity -> 1;
        };
        int interval = switch (name) {
            case "suffocation", "fire_contact", "lava_contact", "campfire_contact", "hot_floor_contact", "cactus_contact" -> 10;
            case "drowning" -> 20;
            case "freezing" -> 40;
            case "starvation" -> 80;
            default -> 0;
        };
        event.registrar().environmentEffect(new EnvironmentEffectDefinition(Identifier.fromNamespaceAndPath("combatnexus", name),
            Identifier.withDefaultNamespace(damage), condition, floor, false, amount, interval, interval == 10 || name.equals("starvation"), name.equals("starvation") ? interval : 0));
    }
}
