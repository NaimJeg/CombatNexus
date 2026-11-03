// Disposable server only. Uses the external SDK example registered at startup; no runtime listener registration.
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var prefix = "io.github.naimjeg.combatnexus.";
    function cls(n) { return Java.loadClass(prefix + n); }
    var Id = Java.loadClass("net.minecraft.resources.Identifier"), Optional = Java.loadClass("java.util.Optional");
    var UUID = Java.loadClass("java.util.UUID"), Consumer = Java.loadClass("java.util.function.Consumer");
    var level = AutoJS.server().overworld(), Lifecycle = cls("combat.platform.CombatWorldLifecycle");
    var Load = Java.loadClass("net.neoforged.neoforge.event.level.LevelEvent$Load");
    var Unload = Java.loadClass("net.neoforged.neoforge.event.level.LevelEvent$Unload");
    Lifecycle.unload(new Unload(level)); Lifecycle.load(new Load(level));
    var world = cls("combat.internal.platform.PlatformWorldLifecycle").existing(level), entities = [], results = [], lineages = [], checks = [], mode = "normal", eventCalls = 0;
    var Source = cls("combat.healing.api.NativeHealingSource"), Sources = cls("combat.healing.api.NativeHealingSources");
    var Reason = cls("combat.healing.api.HealingRequest$Reason");
    var target = null, inNested = false, notifyNested = false;
    function check(ok, label) { if (!ok) throw new Error(label); checks.push(label); }
    function rejects(f) { try { f(); return false; } catch (e) { return true; } }
    function cow() {
        var e = Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level, Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        e.setPos(0, 100, 0); level.addFreshEntity(e); entities.push(e);
        sync(); sync(); check(level.getEntity(e.getUUID()) !== null, "native UUID lookup ready"); return e;
    }
    var Example=Java.loadClass("example.combatnexus.SdkExample"), baseline=null;
    function sync() {
        var log=Example.healingLog(); results=[];lineages=[];
        for(var k=0;k<log.size();k++) {
            var entry=log.get(k);
            if((baseline===null||entry.lineage().id()>baseline)&&target!==null&&entry.result().request().target().equals(target.getUUID())) {
                results.push(entry.result());lineages.push(entry.lineage());
            }
        }
    }
    var bus = Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS;
    var Event = Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingHealEvent");
    var listener = new Consumer({accept: function (event) {
        if (target === null || !event.getEntity().getUUID().equals(target.getUUID())) return;
        eventCalls++;
        if (mode === "cancel") event.setCanceled(true);
        if (mode === "modify") event.setAmount(3);
        if (mode === "kill") target.setHealth(0);
        if (mode === "remove") target.discard();
        if (mode === "recursive") target.heal(0.01);
        if (mode === "siblings" && !inNested) {
            inNested = true; try { for (var n = 0; n < 4; n++) target.heal(0.01); } finally { inNested = false; }
        }
        if ((mode === "nested" || mode === "cancel_parent") && !inNested) {
            inNested = true; try { target.heal(1); } finally { inNested = false; }
            if (mode === "cancel_parent") event.setCanceled(true);
        }
    }});
    bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").LOWEST, false, Event, listener);
    var damageListener = new Consumer({accept: function (event) {
        if (target !== null && event.getEntity().getUUID().equals(target.getUUID()) && mode === "damage_nested") target.heal(1);
    }});
    bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").NORMAL, false,
        Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent"), damageListener);
    function last() { sync(); return results[results.length - 1]; }
    function reset() { var log=Example.healingLog(); baseline=0; for(var k=0;k<log.size();k++)baseline=Math.max(baseline,log.get(k).lineage().id()); mode = "normal"; target.setHealth(2); results.length = 0; lineages.length = 0; eventCalls = 0; }
    function source(id) { return new Source(new (cls("combat.api.CombatSource"))(Id.parse(id), Optional.empty(), Optional.empty()), Reason.ITEM); }
    try {
        target = cow(); var other = cow();
        target.getAttribute(cls("attribute.api.CombatAttributes").healingReceived()).setBaseValue(2);
        reset(); target.heal(2);
        sync(); sync(); check(target.getHealth() === 6 && eventCalls === 1 && results.length === 1, "native heal applies received once and fires one event");
        sync(); sync(); check(last().actualRecovery() === 4 && String(last().request().reason()) === "UNKNOWN_EXTERNAL" && !last().request().source().owner().isPresent(), "unknown external source remains unknown");
        var n = results.length; target.setHealth(3); sync(); check(results.length === n, "direct setHealth stays outside healing");
        reset(); mode = "cancel"; target.heal(2);
        sync(); sync(); check(target.getHealth() === 2 && eventCalls === 1 && String(last().outcome()) === "CANCELLED" && last().overflow() === 0, "native cancellation does not retry or grant overflow");
        reset(); mode = "modify"; target.heal(2);
        sync(); sync(); check(target.getHealth() === 5 && last().actualRecovery() === 3, "native event modifies the already calculated amount");
        reset(); mode = "nested"; target.heal(2);
        sync(); sync(); check(target.getHealth() === 8 && eventCalls === 2 && results.length === 2, "nested event heal uses independent managed settlement and multiplier");
        sync(); sync(); check(last().actualRecovery() === 4 && results[0].actualRecovery() === 2, "independent child and parent receipts");
        sync(); sync(); check(lineages[0].parentId().getAsLong() === lineages[1].id() && lineages[0].rootId() === lineages[1].rootId(), "child finishes first with inherited root and immediate parent");
        reset(); mode = "cancel_parent"; target.heal(2);
        sync(); sync(); check(target.getHealth() === 4 && results.length === 2 && String(last().outcome()) === "CANCELLED", "parent cancellation preserves committed child");
        var budgets = cls("combat.internal.transaction.CombatServerRuntime"), Limits = cls("combat.api.transaction.CombatLimits");
        var Definitions=cls("combat.internal.definition.CombatDefinitions"),savedGeneration=Definitions.forServer(AutoJS.server());
        var limits=function(depth,derived,perTick){Definitions.publish(AutoJS.server(),new (cls("combat.internal.definition.DefinitionGeneration"))(Definitions.nextGeneration(),
            new (cls("combat.config.CombatServerSettings"))(savedGeneration.settings().formulas(),savedGeneration.settings().vanillaCompatibility(),
                new (cls("combat.config.CombatSafetySettings"))(depth,derived,perTick)),savedGeneration.damage(),savedGeneration.behaviors(),savedGeneration.domains()));};
        try {
            limits(2,64,2048);
            reset(); mode = "recursive"; target.heal(0.01);
            sync(); sync(); check(eventCalls === 3 && results.length === 3 && lineages[0].depth() === 2, "shared depth stops synchronous recursion before another event");
            sync(); sync(); check(lineages[0].rootId() === lineages[2].rootId(), "recursive heal cannot create new roots");
            limits(5,2,2048);
            reset(); mode = "siblings"; target.heal(0.01);
            sync(); sync(); check(eventCalls === 3 && results.length === 3 && lineages[2].derivedCount() === 2, "siblings consume one shared root budget");
        } finally { Definitions.publish(AutoJS.server(),new (cls("combat.internal.definition.DefinitionGeneration"))(Definitions.nextGeneration(),savedGeneration.settings(),savedGeneration.damage(),savedGeneration.behaviors(),savedGeneration.domains())); }

        reset(); var echo=Sources.open(target, source("combatnexus_sdk_example:echo")); try { target.heal(2); } finally { echo.close(); }
        sync(); sync(); check(target.getHealth() === 8 && results.length === 2 && eventCalls === 2, "settlement callback heal joins lineage and queues delivery");
        reset(); mode = "damage_nested"; target.invulnerableTime = 0;
        target.hurtServer(level, target.damageSources().generic(), 0.5);
        sync(); sync(); check(results.length === 1 && eventCalls === 1, "native damage callback healing joins managed transactions");
        reset(); target.setHealth(0); target.heal(1);
        sync(); sync(); check(target.getHealth() === 0 && eventCalls === 0 && String(last().outcome()) === "DEAD_TARGET", "dead native target cannot resurrect");
        reset(); mode = "kill"; target.heal(2);
        sync(); sync(); check(target.getHealth() === 0 && String(last().outcome()) === "DEAD_TARGET", "event death prevents healing write");
        reset(); target.heal(0); target.heal(-1); target.heal(NaN); target.heal(Infinity);
        sync(); sync(); check(target.getHealth() === 2 && results.length === 0 && eventCalls === 0, "invalid native amounts cannot create additive healing or events");
        var outer = Sources.open(target, source("combatnexus:outer"));
        try {
            var inner = Sources.open(other, source("combatnexus:inner"));
            try { target.heal(1); sync(); check(String(last().request().reason()) === "UNKNOWN_EXTERNAL", "inner target mismatch does not inherit outer attribution"); }
            finally { inner.close(); }
            target.heal(1); sync(); check(String(last().request().source().id()) === "combatnexus:outer", "closing nested scope restores unconsumed outer source");
            target.heal(1); sync(); check(String(last().request().reason()) === "UNKNOWN_EXTERNAL", "source consumed only once");
        } finally { outer.close(); }
        var scope = Sources.open(target, source("combatnexus:exception"));
        try { throw new Error("controlled source scope failure"); } catch (expected) {} finally { scope.close(); }
        target.heal(1); sync(); check(String(last().request().reason()) === "UNKNOWN_EXTERNAL", "exception cleanup does not leak attribution");
        reset(); Java.loadClass("net.minecraft.world.effect.MobEffects").REGENERATION.value().applyEffectTick(level, target, 0);
        sync(); sync(); check(target.getHealth() === 4 && String(last().request().source().id()) === "minecraft:regeneration", "actual regeneration callsite supplies effect source");
        reset(); Java.loadClass("net.minecraft.world.effect.MobEffects").INSTANT_HEALTH.value().applyInstantenousEffect(level, other, other, target, 0, 1);
        sync(); sync(); check(target.getHealth() === 10 && String(last().request().source().id()) === "minecraft:instant_health" && last().request().source().owner().get().equals(other.getUUID()), "instant effect supplies its actual owner and applies one multiplier");
        reset(); target.heal(100);
        sync(); sync(); check(last().actualRecovery() === 8 && last().overflow() === 192, "overheal separates confirmed recovery and overflow");
        var EI = cls("effect.api.EffectInstance"), ES = cls("effect.api.EffectService"), BP = cls("combat.api.behavior.BehaviorPlan");
        var sid = Id.parse("combatnexus:native_overflow");
        var effect = new EI(new (cls("effect.api.EffectSnapshot"))(target.getUUID(), sid, sid, 0,
            Java.loadClass("java.util.OptionalLong").of(level.getGameTime() + 100), Java.loadClass("java.util.Set").of()),
            Java.loadClass("java.util.List").of(), EI.Ownership.MANAGED, new BP(0,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$OverflowShield"))(1,20,100,(Java.loadClass("io.github.naimjeg.combatnexus.combat.api.action.TriggerLimit")).ONCE))));
        sync(); sync(); check(world.effects().apply(effect, ES.Stacking.REPLACE), "install overflow behavior");
        target.heal(2);
        sync(); sync(); check(world.shields().snapshot(target.getUUID()).stream().mapToDouble(function(p){return p.balance();}).sum() === 4, "native full-health heal grants modified overflow shield once");
        var kernel = cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server());
        var lineage = cls("combat.api.transaction.CombatLineage").root(cls("combat.api.transaction.CombatOperation").HEALING);
        sync(); sync(); check(kernel.admit(lineage).admitted(), "admit controlled compute probe");
        kernel.ticket(lineage).bindSession(level, target.getUUID());
        var compute = kernel.ticket(lineage).enter(cls("combat.internal.transaction.CombatExecutionScope").Phase.COMPUTE);
        try {
            sync(); sync(); check(rejects(function(){target.heal(1);}), "COMPUTE native heal cannot bypass world-write guard");
            sync(); sync(); check(rejects(function(){target.setHealth(1);}), "COMPUTE direct health write rejected");
            sync(); sync(); check(rejects(function(){target.setAbsorptionAmount(1);}), "COMPUTE absorption write rejected");
            sync(); sync(); check(rejects(function(){target.hurtServer(level,target.damageSources().generic(),1);}), "COMPUTE native hurt rejected");
        }
        finally { compute.close(); kernel.finish(lineage); kernel.drain(); }
        reset(); mode = "remove"; target.heal(1);
        sync(); sync(); check(String(last().outcome()) === "DEAD_TARGET" && last().actualRecovery() === 0, "removed during native event cannot receive the planned write");
        mode = "normal"; results.length = 0; eventCalls = 0; target.heal(1);
        sync(); sync(); check(String(last().outcome()) === "REJECTED" && eventCalls === 0, "removed target does not use initialization bypass");
        sync(); sync(); check(cls("combat.internal.transaction.CombatCompletionQueue").pendingCount() === 0, "native healing leaves no pending delivery");
    } finally {
        bus.unregister(listener); bus.unregister(damageListener);
        for (var i = 0; i < entities.length; i++) entities[i].discard();
        Lifecycle.unload(new Unload(level)); Lifecycle.load(new Load(level));
    }
    return JSON.stringify({passed:checks.length, checks:checks});
})()
