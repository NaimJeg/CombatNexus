package io.github.naimjeg.combatnexus.compat.mixin.apotheosis;

import java.util.*;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.special.MageSlayerBonus;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.Tags;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest;

@Mixin(value = MageSlayerBonus.class, remap = false)
public abstract class MageSlayerBonusMixin {
    @Shadow @Final protected Map<Purity, Float> values;
    @Inject(method = "onHurt", at = @At("HEAD"), cancellable = true, require = 1)
    private void combatnexus$mageSlayer(GemInstance instance, DamageSource source, LivingEntity user, float amount, CallbackInfoReturnable<Float> callback) {
        if (!(user.level() instanceof ServerLevel level) || !source.is(Tags.DamageTypes.IS_MAGIC)) return;
        var world = CombatAccess.find(level); if (world.isEmpty()) return;
        float ratio = values.get(instance.purity());
        float healing = amount * ratio, remaining = amount * (1 - ratio);
        var provenance = new CombatSource(io.github.naimjeg.combatnexus.compat.apotheosis.GemSources.identity(instance),
                Optional.of(user.getUUID()), Optional.empty());
        var plan = new BeforeApplicationPlan(List.of(new BeforeApplicationPlan.Step(new DomainCommand.Heal(
                new HealingRequest(UUID.randomUUID(), user.getUUID(), provenance, HealingRequest.Reason.EFFECT, healing)),
                BeforeApplicationPlan.RejectionPolicy.CONTINUE)));
        var declaration=CombatAccess.bridgeCoverage().stream().filter(c->c.id().equals(Identifier.parse("apotheosis:mageslayer"))).findFirst().orElseThrow();
        var action=io.github.naimjeg.combatnexus.combat.projectile.api.ProjectileFacts.launch(source.getDirectEntity()).map(l->l.action()).or(()->source.getEntity()==null?Optional.empty():world.orElseThrow().currentAction(source.getEntity().getUUID()).map(a->a.id()));
        try(var call=world.orElseThrow().bridges().open(UUID.randomUUID(),action.orElseGet(UUID::randomUUID),user.getUUID(),declaration.id(),declaration.position(),declaration.executor())) {
        var result = world.orElseThrow().beforeApplication(UUID.randomUUID(), user.getUUID(), plan);
        call.nativeContribution(result.isPresent()?io.github.naimjeg.combatnexus.combat.api.bridge.BridgeInvocation.Contribution.SUPPRESSED:io.github.naimjeg.combatnexus.combat.api.bridge.BridgeInvocation.Contribution.INSEPARABLE);
        if (result.isPresent()) callback.setReturnValue(result.orElseThrow().continueParent() ? remaining : 0F);
        call.returnedNormally();
        }
    }
}
