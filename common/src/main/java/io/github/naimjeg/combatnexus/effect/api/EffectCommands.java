package io.github.naimjeg.combatnexus.effect.api;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent;
public interface EffectCommands {
    EffectResult fact(UUID request, EffectFact fact);
    io.github.naimjeg.combatnexus.combat.api.CommandResult<Integer> clear(UUID request, UUID target);
    io.github.naimjeg.combatnexus.combat.api.CommandResult<Boolean> remove(UUID request, UUID target, net.minecraft.resources.Identifier effect,
            net.minecraft.resources.Identifier source, EffectService.Removal reason);
    io.github.naimjeg.combatnexus.combat.api.CommandResult<NativeEffectReceipt> nativeEffect(NativeEffectCommand command);
    io.github.naimjeg.combatnexus.combat.api.CommandResult<NativeEffectReceipt> nativeEffect(NativeEffectCommand command, CombatParent parent);
    List<EffectView> snapshot(UUID target);
    EffectResult apply(EffectApplication application);
    EffectResult apply(EffectApplication application, CombatParent parent);
    EffectResult bind(UUID request, EffectBinding binding);
    EffectResult bind(UUID request, EffectBinding binding, CombatParent parent);
    EffectResult dispel(UUID request, EffectView effect);
    EffectResult dispel(UUID request, EffectView effect, CombatParent parent);
}
