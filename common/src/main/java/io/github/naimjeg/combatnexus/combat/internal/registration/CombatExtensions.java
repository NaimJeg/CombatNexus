package io.github.naimjeg.combatnexus.combat.internal.registration;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatListenerRegistry;

import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.pipeline.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.*;
import io.github.naimjeg.combatnexus.combat.healing.api.*;
import io.github.naimjeg.combatnexus.combat.shield.api.ShieldSettlement;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatDispatchScope;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;

/** Immutable extension tables shared by world sessions, built at loader startup. */
public final class CombatExtensions {
    private static final Object OWNER = new Object();
    private static boolean initialized;
    private static List<io.github.naimjeg.combatnexus.combat.api.behavior.SettlementPolicy> settlementPolicies=List.of();
    public static List<io.github.naimjeg.combatnexus.combat.api.behavior.SettlementPolicy> settlementPolicies(){return settlementPolicies;}
    private static Map<Identifier,Consumer<DefinitionView>> definitionObservers=Map.of();
    public static void definitions(Object server,DefinitionView view) {
        definitionObservers.forEach((id,observer)->{try(var scope=CombatDispatchScope.observation(OWNER,view,server)){observer.accept(view);}catch(Exception error){CombatFailures.rethrowFatal(error);}});
    }
    private record MomentObserver<T extends io.github.naimjeg.combatnexus.combat.api.action.CombatMoment>(Identifier id,Class<T> type,Consumer<T> observer) {
        void deliver(Object server,io.github.naimjeg.combatnexus.combat.api.action.CombatMoment moment) {
            if(!type.isInstance(moment))return;
            try(var scope=CombatDispatchScope.observation(OWNER,moment,server)) { observer.accept(type.cast(moment)); }
            catch(Exception error) { CombatFailures.rethrowFatal(error); }
        }
    }
    private static List<MomentObserver<?>> moments=List.of();
    public static void moment(net.minecraft.server.level.ServerLevel level,io.github.naimjeg.combatnexus.combat.api.action.CombatMoment moment) {
        if(moments.stream().noneMatch(m->m.type().isInstance(moment)))return;
        var kernel=io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.existing(level.getServer());
        if(kernel.isEmpty())return;
        kernel.get().rejectionCompletion().ifPresent(slot->{try(slot){slot.publish(()->moments.forEach(observer->observer.deliver(level.getServer(),moment)));}});
    }
    private static Map<Identifier,java.util.function.Function<io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame,List<io.github.naimjeg.combatnexus.combat.api.source.SourceDescription>>> descriptions=Map.of();
    public static List<io.github.naimjeg.combatnexus.combat.api.source.SourceDescription> describe(io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame frame) {
        var result=new ArrayList<io.github.naimjeg.combatnexus.combat.api.source.SourceDescription>();
        descriptions.forEach((id,provider)->{
            try(var read=CombatReadScope.open()) { var entries=List.copyOf(provider.apply(frame)); if(entries.size()>128)throw new IllegalArgumentException("Source description capacity");result.addAll(entries); }
            catch(Exception failure) { CombatFailures.rethrowFatal(failure); }
        });
        return List.copyOf(result);
    }
    private static Map<Identifier, io.github.naimjeg.combatnexus.combat.api.bridge.DamageFormulaPolicy> damageFormulas = Map.of();
    public static io.github.naimjeg.combatnexus.combat.api.bridge.DamageFormulaPolicy damageFormula(Identifier id) {
        return Objects.requireNonNull(damageFormulas.get(id), "Unregistered damage formula: " + id);
    }
    private static Map<Identifier, Consumer<io.github.naimjeg.combatnexus.combat.api.source.RestoredSource>> restorationOwners = Map.of();
    public static void restoredSource(net.minecraft.server.MinecraftServer server, io.github.naimjeg.combatnexus.combat.api.source.RestoredSource restored) {
        var owner = restorationOwners.get(restored.source().source().provider());
        if (owner == null) return;
        try (var observation = CombatDispatchScope.observation(OWNER, restored, server)) {
            owner.accept(restored);
        } catch (Exception exception) { CombatFailures.rethrowFatal(exception); }
    }
    private record Collector<T>(io.github.naimjeg.combatnexus.combat.api.input.FactKey<T> key,
            java.util.function.Function<net.minecraft.world.entity.LivingEntity, T> capture) {
        String encode(net.minecraft.world.entity.LivingEntity entity) {
            return key.codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE, capture.apply(entity)).getOrThrow().toString();
        }
    }
    private static List<Collector<?>> collectors = List.of();
    public static io.github.naimjeg.combatnexus.combat.api.input.FrozenFacts captureFacts(net.minecraft.world.entity.LivingEntity entity) {
        var values = new LinkedHashMap<io.github.naimjeg.combatnexus.combat.api.input.FactKey<?>, String>();
        var incomplete = new HashSet<Identifier>();
        for (var collector : collectors) {
            try (var read = io.github.naimjeg.combatnexus.combat.api.transaction.CombatReadScope.open()) {
                values.put(collector.key(), collector.encode(entity));
            }
            catch (Exception exception) {
                incomplete.add(collector.key().id());
                CombatFailures.rethrowFatal(exception);
            }
        }
        return new io.github.naimjeg.combatnexus.combat.api.input.FrozenFacts(values, incomplete);
    }
    private static List<io.github.naimjeg.combatnexus.combat.api.bridge.BridgeCapability> bridges = List.of();
    public static List<io.github.naimjeg.combatnexus.combat.api.bridge.BridgeCapability> bridgeCoverage() { return bridges; }
    private static List<CombatListenerRegistry.Entry<io.github.naimjeg.combatnexus.combat.api.source.SourceProvider>> providers = List.of();
    public static List<CombatListenerRegistry.Entry<io.github.naimjeg.combatnexus.combat.api.source.SourceProvider>> sourceProviders() { return providers; }
    private static DomainPipeline<HealingPhase, Consumer<HealingCalculation>> healing = new DomainPipeline.Builder<HealingPhase, Consumer<HealingCalculation>>(HealingPhase.class).freeze();
    private static List<Reaction> healingReactions = List.of();
    private static List<CombatListenerRegistry.Entry<Consumer<HealingSettlement>>> healingObservers = List.of();
    private static List<CombatListenerRegistry.Entry<Consumer<ShieldSettlement>>> shieldObservers = List.of();
    private static List<CombatListenerRegistry.Entry<Consumer<CommandResult<?>>>> stateObservers = List.of();
    private record Reaction(Identifier id, int priority, Set<CombatOperation> permissions, Consumer<CombatCallback<HealingSettlement>> handler) {}
    private CombatExtensions() {}
    public static synchronized void initialize(Consumer<CombatRegistrar> registration) {
        if (initialized) throw new IllegalStateException("Combat extensions already registered");
        IntentTypes.builtins();
        var session = new Session();
        session.settlementPolicy(io.github.naimjeg.combatnexus.combat.internal.coordination.LifestealRequests.policy());
        try { registration.accept(session); }
        finally { session.open = false; }
        var validatedBridges = session.bridges.freeze();
        session.attributePolicies.freeze();
        healing = session.pipeline.freeze();
        session.providers.freeze(); providers = session.providers.entries();
        session.healObservers.freeze(); healingObservers = session.healObservers.entries();
        session.shieldObservers.freeze(); shieldObservers = session.shieldObservers.entries();
        session.stateObservers.freeze(); stateObservers = session.stateObservers.entries();
        healingReactions = session.reactions.values().stream().sorted(Comparator.comparingInt(Reaction::priority).reversed().thenComparing(r -> r.id().toString())).toList();
        BehaviorTypes.freeze();
        io.github.naimjeg.combatnexus.combat.damage.api.display.DamageDescriptions.registration().freeze();
        io.github.naimjeg.combatnexus.effect.internal.EffectBehaviorDescriptors.freeze();
        io.github.naimjeg.combatnexus.effect.internal.EffectCapabilities.freeze();
        io.github.naimjeg.combatnexus.effect.internal.EffectStackingStrategies.freeze();
        io.github.naimjeg.combatnexus.effect.internal.EnvironmentEffects.freeze();
        io.github.naimjeg.combatnexus.effect.internal.EffectMappings.freeze();
        IntentTypes.freeze();
        settlementPolicies=session.settlementPolicies.values().stream().sorted(Comparator.comparingInt(io.github.naimjeg.combatnexus.combat.api.behavior.SettlementPolicy::priority).reversed().thenComparing(v->v.id().toString())).toList();
        io.github.naimjeg.combatnexus.combat.state.internal.RuntimeStateTypes.install(session.stateTypes);
        bridges = validatedBridges;
        damageFormulas = Map.copyOf(session.damageFormulas);
        restorationOwners = Map.copyOf(session.restorationOwners);
        collectors = List.copyOf(session.collectors.values());
        io.github.naimjeg.combatnexus.presentation.internal.ItemComponentDescriptions.install(session.itemDescriptors);
        moments=List.copyOf(session.moments.values());
        descriptions=Collections.unmodifiableMap(new LinkedHashMap<>(session.descriptions));
        definitionObservers=Collections.unmodifiableMap(new LinkedHashMap<>(session.definitionObservers));
        io.github.naimjeg.combatnexus.attribute.internal.AttributeCapturePolicies.install(
                session.attributePolicies.entries().stream().map(CombatListenerRegistry.Entry::listener).toList());
        initialized = true;
    }
    private static final class Session implements CombatRegistrar, HealingRegistrar {
        private final Map<Identifier, io.github.naimjeg.combatnexus.presentation.internal.ItemComponentDescriptions.Entry<?>> itemDescriptors = new LinkedHashMap<>();
        public <T> void itemDescriptor(Identifier id, net.minecraft.core.component.DataComponentType<T> component,
                io.github.naimjeg.combatnexus.presentation.api.ItemComponentDescriptor<T> descriptor) {
            check();
            if (itemDescriptors.putIfAbsent(Objects.requireNonNull(id), new io.github.naimjeg.combatnexus.presentation.internal.ItemComponentDescriptions.Entry<>(component, descriptor)) != null)
                throw new IllegalArgumentException("Duplicate item descriptor: " + id);
        }
        public io.github.naimjeg.combatnexus.combat.api.platform.EquipmentDamageAccess equipmentDamage() {
            check();
            return io.github.naimjeg.combatnexus.combat.damage.core.settlement.DamageSettlementTracker::applyEquipment;
        }
        public io.github.naimjeg.combatnexus.combat.api.platform.NativeDamageAccess nativeDamage() {
            check();
            return io.github.naimjeg.combatnexus.combat.damage.core.settlement.NativeDamageAdapter.INSTANCE;
        }
        public io.github.naimjeg.combatnexus.combat.api.platform.EffectOriginAccess effectOrigins() {
            check(); return io.github.naimjeg.combatnexus.effect.internal.NativeEffectOrigins.INSTANCE;
        }
        public io.github.naimjeg.combatnexus.combat.api.platform.WorldLifecycleAccess worldLifecycle() {
            check(); return io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle.INSTANCE;
        }
        public io.github.naimjeg.combatnexus.combat.api.platform.NativeEffectsAccess nativeEffects() {
            check(); return io.github.naimjeg.combatnexus.effect.internal.PlatformEffects.INSTANCE;
        }
        boolean open = true;
        final Map<Identifier,io.github.naimjeg.combatnexus.combat.api.behavior.SettlementPolicy> settlementPolicies=new LinkedHashMap<>();
        public void settlementPolicy(io.github.naimjeg.combatnexus.combat.api.behavior.SettlementPolicy policy){check();if(settlementPolicies.putIfAbsent(policy.id(),policy)!=null)throw new IllegalArgumentException("Duplicate settlement policy: "+policy.id());}
        public <T extends io.github.naimjeg.combatnexus.combat.api.scheduling.ScheduledIntent> void intent(io.github.naimjeg.combatnexus.combat.api.scheduling.IntentType<T> type) { check(); IntentTypes.register(type); }
        final Map<Identifier,io.github.naimjeg.combatnexus.combat.state.api.StateKey<?>> stateTypes=new LinkedHashMap<>();
        public <T> void runtimeState(io.github.naimjeg.combatnexus.combat.state.api.StateKey<T> key) { check();if(stateTypes.putIfAbsent(key.id(),key)!=null)throw new IllegalArgumentException("Duplicate runtime state: "+key.id()); }
        final Map<Identifier,Consumer<DefinitionView>> definitionObservers=new LinkedHashMap<>();
        public void observeDefinitions(Identifier id,Consumer<DefinitionView> observer) { check();Objects.requireNonNull(id);Objects.requireNonNull(observer);if(definitionObservers.putIfAbsent(id,observer)!=null)throw new IllegalArgumentException("Duplicate definition observer"); }
        final Map<Identifier,MomentObserver<?>> moments=new LinkedHashMap<>();
        public <T extends io.github.naimjeg.combatnexus.combat.api.action.CombatMoment> void observeMoment(Identifier id,Class<T> type,Consumer<T> observer) {
            check();Objects.requireNonNull(type);Objects.requireNonNull(observer);
            if(moments.putIfAbsent(id,new MomentObserver<>(Objects.requireNonNull(id),type,observer))!=null)throw new IllegalArgumentException("Duplicate moment observer: "+id);
        }
        final Map<Identifier,java.util.function.Function<io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame,List<io.github.naimjeg.combatnexus.combat.api.source.SourceDescription>>> descriptions=new LinkedHashMap<>();
        public void sourceDescriptions(Identifier id,java.util.function.Function<io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame,List<io.github.naimjeg.combatnexus.combat.api.source.SourceDescription>> provider) {
            check();Objects.requireNonNull(id);Objects.requireNonNull(provider);if(descriptions.putIfAbsent(id,provider)!=null)throw new IllegalArgumentException("Duplicate source descriptor: "+id);
        }
        final Map<Identifier, io.github.naimjeg.combatnexus.combat.api.bridge.DamageFormulaPolicy> damageFormulas = new LinkedHashMap<>();
        public void damageFormula(Identifier id, io.github.naimjeg.combatnexus.combat.api.bridge.DamageFormulaPolicy policy) {
            check(); Objects.requireNonNull(id); Objects.requireNonNull(policy);
            if (damageFormulas.putIfAbsent(id, policy) != null) throw new IllegalArgumentException("Duplicate damage formula: " + id);
        }
        final Map<Identifier, Consumer<io.github.naimjeg.combatnexus.combat.api.source.RestoredSource>> restorationOwners = new LinkedHashMap<>();
        public void restoredSource(Identifier provider, Consumer<io.github.naimjeg.combatnexus.combat.api.source.RestoredSource> owner) {
            check(); Objects.requireNonNull(provider); Objects.requireNonNull(owner);
            if (restorationOwners.putIfAbsent(provider, owner) != null) throw new IllegalArgumentException("Duplicate restoration owner: " + provider);
        }
        final Map<Identifier, Collector<?>> collectors = new LinkedHashMap<>();
        public <T> void facts(io.github.naimjeg.combatnexus.combat.api.input.FactKey<T> key,
                java.util.function.Function<net.minecraft.world.entity.LivingEntity, T> collector) {
            check(); Objects.requireNonNull(collector);
            if (collectors.putIfAbsent(key.id(), new Collector<>(key, collector)) != null)
                throw new IllegalArgumentException("Duplicate fact collector: " + key.id());
        }
        final BridgeRegistry bridges = new BridgeRegistry();
        final CombatListenerRegistry<io.github.naimjeg.combatnexus.attribute.api.EffectiveAttributePolicy> attributePolicies = new CombatListenerRegistry<>();
        public io.github.naimjeg.combatnexus.combat.api.bridge.BridgeRegistrar bridges() {
            check(); return capability -> { check(); bridges.register(capability); };
        }
        public void effectiveAttribute(Identifier id, int priority, io.github.naimjeg.combatnexus.attribute.api.EffectiveAttributePolicy policy) {
            check(); attributePolicies.register(id, priority, policy);
        }
        final CombatListenerRegistry<io.github.naimjeg.combatnexus.combat.api.source.SourceProvider> providers = new CombatListenerRegistry<>();
        public void sourceProvider(Identifier id, int priority, io.github.naimjeg.combatnexus.combat.api.source.SourceProvider provider) { check(); providers.register(id, priority, provider); }
        final DomainPipeline.Builder<HealingPhase, Consumer<HealingCalculation>> pipeline = new DomainPipeline.Builder<>(HealingPhase.class);
        final CombatListenerRegistry<Consumer<HealingSettlement>> healObservers = new CombatListenerRegistry<>();
        final CombatListenerRegistry<Consumer<ShieldSettlement>> shieldObservers = new CombatListenerRegistry<>();
        final CombatListenerRegistry<Consumer<CommandResult<?>>> stateObservers = new CombatListenerRegistry<>();
        final Map<Identifier, Reaction> reactions = new HashMap<>();
        void check() { if (!open) throw new IllegalStateException("Registration callback expired"); }
        public <T extends io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent> void behavior(io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorType<T> type) { check(); BehaviorTypes.register(type); }
        public void effectCapability(Identifier id, io.github.naimjeg.combatnexus.effect.api.EffectCapability capability) { check(); io.github.naimjeg.combatnexus.effect.internal.EffectCapabilities.register(id, capability); }
        public void effectStacking(Identifier id, io.github.naimjeg.combatnexus.effect.api.EffectStackingStrategy strategy) { check(); io.github.naimjeg.combatnexus.effect.internal.EffectStackingStrategies.register(id, strategy); }
        public void environmentEffect(io.github.naimjeg.combatnexus.effect.api.EnvironmentEffectDefinition definition) { check(); io.github.naimjeg.combatnexus.effect.internal.EnvironmentEffects.register(definition); }
        public <T extends io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent> void effectDescriptor(Identifier id, Class<T> type, io.github.naimjeg.combatnexus.effect.api.EffectBehaviorDescriptor<T> descriptor) { check(); io.github.naimjeg.combatnexus.effect.internal.EffectBehaviorDescriptors.register(id, type, descriptor); }
        public io.github.naimjeg.combatnexus.combat.damage.api.display.phrase.RulePhraseRegistry ruleDescriptions() { check(); return io.github.naimjeg.combatnexus.combat.damage.api.display.DamageDescriptions.registration(); }
        public void effectDisplay(Identifier definition, io.github.naimjeg.combatnexus.effect.api.EffectDisplay display) { check(); io.github.naimjeg.combatnexus.effect.internal.EffectMappings.display(definition, display); }
        public void effectCondition(Identifier id, io.github.naimjeg.combatnexus.effect.api.EffectCondition condition) { check(); io.github.naimjeg.combatnexus.effect.internal.EffectMappings.condition(id, condition); }
        public HealingRegistrar healing() { check(); return this; }
        public void processor(Identifier id, HealingPhase phase, int priority, Consumer<HealingCalculation> handler) {
            check(); priority(priority); pipeline.register(id, phase, priority, true, handler);
        }
        public void observe(Identifier id, int priority, Consumer<HealingSettlement> observer) { check(); healObservers.register(id, priority, observer); }
        public void observeShield(Identifier id, int priority, Consumer<ShieldSettlement> observer) { check(); shieldObservers.register(id, priority, observer); }
        public void observeState(Identifier id, int priority, Consumer<CommandResult<?>> observer) { check(); stateObservers.register(id, priority, observer); }
        public void react(Identifier id, int priority, Set<CombatOperation> permissions, Consumer<CombatCallback<HealingSettlement>> handler) {
            check(); priority(priority);
            var entry = new Reaction(Objects.requireNonNull(id), priority, Set.copyOf(permissions), Objects.requireNonNull(handler));
            if (reactions.putIfAbsent(id, entry) != null) throw new IllegalArgumentException("Duplicate reaction: " + id);
        }
        void priority(int priority) { if (priority < -10000 || priority > 10000) throw new IllegalArgumentException("Priority out of range"); }
    }
    public static void calculate(HealingPhase phase, HealingCalculation calculation) {
        healing.execute(phase, entry -> {
            try { calculation.invoke(entry.handler()); }
            catch (Throwable failure) { CombatFailures.rethrowFatal(failure); }
        }, () -> false);
    }
    public static void registerDamageContributions(io.github.naimjeg.combatnexus.combat.damage.api.event.DamageModuleRegistrar registrar) {
        for (var phase : io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase.values())
            registrar.registerPhaseProcessor(Identifier.fromNamespaceAndPath("combatnexus", "behavior/" + phase.name().toLowerCase(Locale.ROOT)), new io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor() {
                public io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase phase() { return phase; }
                public void apply(io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext ctx) {
                    var participants = new LinkedHashSet<UUID>(); participants.add(ctx.victim().getUUID());
                    if (ctx.logicalAttacker() != null) participants.add(ctx.logicalAttacker().getUUID());
                    for (var participant : participants) for (var source : ctx.input(participant).behaviors())
                        for (var component : source.plan().components()) if (component instanceof io.github.naimjeg.combatnexus.combat.api.behavior.NumericalBehavior contribution)
                            contribution.damage(phase, ctx, source);
                }
            });
    }

    private static <T> void observe(Object server, T result, List<CombatListenerRegistry.Entry<Consumer<T>>> entries) {
        for (var entry : entries) try (var scope = CombatDispatchScope.observation(OWNER, result, server)) { entry.listener().accept(result); }
        catch (Throwable failure) { CombatFailures.rethrowFatal(failure); }
    }
    public static void healingCompleted(Object server, CombatLineage lineage, HealingSettlement result) {
        for (var entry : healingReactions) {
            var callback = new CombatCallback<>(lineage, result, server);
            var permissions = result.actualRecovery() > 0 ? entry.permissions() : Set.<CombatOperation>of();
            try (var scope = CombatDispatchScope.callback(OWNER, callback, null, server, lineage, permissions)) { entry.handler().accept(callback); }
            catch (Throwable failure) { CombatFailures.rethrowFatal(failure); }
        }
        observe(server, result, healingObservers);
    }
    public static void shieldCompleted(Object server, ShieldSettlement result) { observe(server, result, shieldObservers); }
    public static void stateCompleted(Object server, CombatLineage lineage, CommandResult<?> result) { observe(server, result, stateObservers); }
}
