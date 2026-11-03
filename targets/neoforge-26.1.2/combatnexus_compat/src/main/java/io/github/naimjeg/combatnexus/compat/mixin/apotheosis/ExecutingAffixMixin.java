package io.github.naimjeg.combatnexus.compat.mixin.apotheosis;

import java.util.*;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.effect.ExecutingAffix;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.naimjeg.combatnexus.combat.api.CombatAccess;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;
import io.github.naimjeg.combatnexus.combat.health.api.LethalAttempt;

@Mixin(value = ExecutingAffix.class, remap = false)
public abstract class ExecutingAffixMixin {
    @Shadow private float getTrueLevel(LootRarity rarity, float level) { throw new AssertionError(); }
    @Inject(method = "doPostAttack", at = @At("HEAD"), cancellable = true, require = 1)
    private void combatnexus$execute(AffixInstance instance, LivingEntity user, Entity target, CallbackInfo callback) {
        if (!(target instanceof LivingEntity living) || !(living.level() instanceof ServerLevel level)) return;
        var world = CombatAccess.find(level);
        if (world.isEmpty()) return;
        // All optional-mod inputs are read at the actual post-attack boundary and frozen in the command.
        float threshold = getTrueLevel(instance.getRarity(), instance.level());
        double strength = ApothicAttributes.getLocalAtkStrength(user);
        boolean qualifies = !living.isDeadOrDying() && strength >= 0.98 && living.getHealth() / living.getMaxHealth() < threshold;
        // Once routed, neither refusal nor an uncertain write can fall through to another native attempt.
        callback.cancel();
        try(var call=io.github.naimjeg.combatnexus.compat.BridgeCalls.replacing(world.orElseThrow(),"apotheosis:executing",living.getUUID(),world.orElseThrow().currentAction(user.getUUID()).map(a->a.id()).orElseGet(UUID::randomUUID))) {
        if (qualifies) world.orElseThrow().health().execute(new LethalAttempt(UUID.randomUUID(), living.getUUID(),
                new CombatSource(Identifier.fromNamespaceAndPath("apotheosis", "executing"), Optional.of(user.getUUID()), Optional.empty()),
                Identifier.fromNamespaceAndPath("apotheosis", "execute"), Optional.of(user.getUUID()), false));
        call.returnedNormally();
        }
    }
}
