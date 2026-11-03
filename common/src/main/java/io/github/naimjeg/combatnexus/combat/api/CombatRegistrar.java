package io.github.naimjeg.combatnexus.combat.api;

import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingRegistrar;
import io.github.naimjeg.combatnexus.combat.shield.api.ShieldSettlement;

/** Startup-only registration capability; retaining it does not extend registration lifetime. */
public interface CombatRegistrar {
    <T> void itemDescriptor(Identifier id, net.minecraft.core.component.DataComponentType<T> component,
            io.github.naimjeg.combatnexus.presentation.api.ItemComponentDescriptor<T> descriptor);
    io.github.naimjeg.combatnexus.combat.api.platform.EquipmentDamageAccess equipmentDamage();
    io.github.naimjeg.combatnexus.combat.api.platform.NativeDamageAccess nativeDamage();
    io.github.naimjeg.combatnexus.combat.api.platform.EffectOriginAccess effectOrigins();
    io.github.naimjeg.combatnexus.combat.api.platform.WorldLifecycleAccess worldLifecycle();
    io.github.naimjeg.combatnexus.combat.api.platform.NativeEffectsAccess nativeEffects();
    io.github.naimjeg.combatnexus.combat.damage.api.display.phrase.RulePhraseRegistry ruleDescriptions();
    <T extends io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent> void effectDescriptor(Identifier behaviorId, Class<T> type, io.github.naimjeg.combatnexus.effect.api.EffectBehaviorDescriptor<T> descriptor);
    void effectCapability(Identifier id, io.github.naimjeg.combatnexus.effect.api.EffectCapability capability);
    void effectStacking(Identifier id, io.github.naimjeg.combatnexus.effect.api.EffectStackingStrategy strategy);
    void environmentEffect(io.github.naimjeg.combatnexus.effect.api.EnvironmentEffectDefinition definition);
    void effectDisplay(Identifier definition, io.github.naimjeg.combatnexus.effect.api.EffectDisplay display);
    void effectCondition(Identifier id, io.github.naimjeg.combatnexus.effect.api.EffectCondition condition);
    void settlementPolicy(io.github.naimjeg.combatnexus.combat.api.behavior.SettlementPolicy policy);
    <T extends io.github.naimjeg.combatnexus.combat.api.scheduling.ScheduledIntent> void intent(io.github.naimjeg.combatnexus.combat.api.scheduling.IntentType<T> type);
    <T> void runtimeState(io.github.naimjeg.combatnexus.combat.state.api.StateKey<T> key);
    void observeDefinitions(Identifier id, Consumer<DefinitionView> observer);
    <T extends io.github.naimjeg.combatnexus.combat.api.action.CombatMoment> void observeMoment(Identifier id, Class<T> type, Consumer<T> observer);
    void sourceDescriptions(Identifier id, java.util.function.Function<io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame,java.util.List<io.github.naimjeg.combatnexus.combat.api.source.SourceDescription>> descriptions);
    io.github.naimjeg.combatnexus.combat.api.bridge.BridgeRegistrar bridges();
    void damageFormula(Identifier id, io.github.naimjeg.combatnexus.combat.api.bridge.DamageFormulaPolicy policy);
    <T> void facts(io.github.naimjeg.combatnexus.combat.api.input.FactKey<T> key,
            java.util.function.Function<net.minecraft.world.entity.LivingEntity, T> collector);
    void effectiveAttribute(Identifier id, int priority, io.github.naimjeg.combatnexus.attribute.api.EffectiveAttributePolicy policy);
    HealingRegistrar healing();
    void restoredSource(Identifier provider, Consumer<io.github.naimjeg.combatnexus.combat.api.source.RestoredSource> owner);
    void sourceProvider(Identifier id, int priority, io.github.naimjeg.combatnexus.combat.api.source.SourceProvider provider);
    <T extends io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent> void behavior(io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorType<T> type);
    void observeShield(Identifier id, int priority, Consumer<ShieldSettlement> observer);
    void observeState(Identifier id, int priority, Consumer<CommandResult<?>> observer);
}
