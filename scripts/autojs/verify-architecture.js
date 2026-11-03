// Isolated NeoForge 26.1.2 server; run using scripts/run-autojs.py.
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var prefix = "io.github.naimjeg.combatnexus.";
    function cls(name) { return Java.loadClass(prefix + name); }
    var Id = Java.loadClass("net.minecraft.resources.Identifier");
    var UUID = Java.loadClass("java.util.UUID");
    var Optional = Java.loadClass("java.util.Optional");
    var Lists = Java.loadClass("java.util.List");
    var level = AutoJS.server().overworld();
    // prepare-architecture.js loads the native entity lookup area on an earlier tick.
    var entity = Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(
        level, Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    entity.setPos(0, 100, 0); level.addFreshEntity(entity);
    var checks = [];
    function check(ok, label) { if (!ok) throw new Error(label); checks.push(label); }
    var runtime = cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    var Api = cls("combat.damage.api.DamageModuleApi");
    var Request = cls("combat.damage.api.damage.DamageRequest");
    var Source = cls("combat.damage.api.damage.DamageSourceDescriptor");
    var Scope = cls("combat.internal.transaction.CombatExecutionScope");
    var source = Source.from(entity.damageSources().generic());
    try {
        var rejected = Request.builder(level, entity, source, 2).build();
        var kernel = cls("combat.internal.transaction.CombatServerRuntime").of(level.getServer());
        var lineage = cls("combat.api.transaction.CombatLineage").root(cls("combat.api.transaction.CombatOperation").HEALING);
        kernel.admit(lineage);
        var scope = kernel.ticket(lineage).enter(Scope.Phase.COMPUTE);
        try { check(String(Api.submitDamage(rejected).status()) === "REJECTED", "compute submission rejected"); }
        finally { scope.close(); kernel.finish(lineage); }
        check(String(Api.submitDamage(rejected).status()) === "REJECTED", "first rejected submission consumes identity");
        var before = entity.getHealth();
        var hit = Api.submitDamage(Request.builder(level, entity, source, 2).build());
        check(String(hit.status()) === "APPLIED" && entity.getHealth() < before, "damage applies synchronously");
        check(!Scope.isActive(), "scope cleared after native damage");
        check(level.getEntity(entity.getUUID()) !== null, "native entity lookup");
        var CombatSource = cls("combat.api.CombatSource");
        var origin = new CombatSource(Id.parse("combatnexus:architecture_check"), Optional.empty(), Optional.empty());
        var HealingRequest = cls("combat.healing.api.HealingRequest");
        var heal = new HealingRequest(UUID.randomUUID(), entity.getUUID(), origin, HealingRequest.Reason.LIFESTEAL, 1);
        before = entity.getHealth();
        var result = runtime.healing().submit(heal);
        check(String(result.outcome()) === "APPLIED" && entity.getHealth() > before, "native healing and common classification");
        check(String(runtime.healing().submit(heal).outcome()) === "REJECTED", "healing duplicate rejected");
        var service = runtime.effects();
        var effectSource = Id.parse("combatnexus:architecture_check");
        var effect = cls("effect.api.ManagedEffects").agility(entity.getUUID(), effectSource, 0.5, level.getGameTime() + 100);
        var speed = entity.getAttribute(Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").MOVEMENT_SPEED);
        var base = speed.getValue();
        check(service.apply(effect, cls("effect.api.EffectService").Stacking.REPLACE), "managed contribution applied");
        check(Math.abs(speed.getValue() - base * 1.5) < 0.0001, "managed contribution applies once");
        service.remove(entity.getUUID(), Id.parse("combatnexus:agility"), effectSource, cls("effect.api.EffectService").Removal.REMOVED);
        check(Math.abs(speed.getValue() - base) < 0.0001, "source contribution revoked");
        var queue = new (cls("combat.internal.transaction.CombatCompletionQueue"))();
        var calls = 0, slot = queue.reserve();
        slot.publish(function () { calls++; });
        var duplicate = false;
        try { slot.publish(function () { calls++; }); } catch (error) { duplicate = true; }
        cls("combat.internal.transaction.CombatCompletionQueue").drainIfSafe();
        check(calls === 1 && duplicate, "reserved completion publishes once");
        check(cls("combat.internal.transaction.CombatCompletionQueue").pendingCount() === 0, "completion FIFO empty");
        var AttributeReader = cls("attribute.api.AttributeReader");
        var Registry = Java.loadClass("net.minecraft.core.registries.BuiltInRegistries");
        var capture = runtime.captureAttributes(entity, Registry.ATTRIBUTE.listElements().toList());
        check(capture.valueFrame().revision() === capture.provenance().revision(), "attribute views share capture revision");
        var numeric = capture.valueFrame().values().get(Id.parse("minecraft:movement_speed")).effective();
        speed.setBaseValue(speed.getBaseValue() + 0.1);
        var later = runtime.captureAttributes(entity, Registry.ATTRIBUTE.listElements().toList());
        check(later.revision() > capture.revision() && capture.valueFrame().values().get(Id.parse("minecraft:movement_speed")).effective() === numeric,
            "same tick recapture preserves old numeric frame");
        var budget = new (cls("presentation.api.PresentationBudget"))(1, 100);
        var limited = false;
        try { AttributeReader.capture(entity, 1, Registry.ATTRIBUTE.listElements().toList(), function (count) { budget.entry(count); }); }
        catch (error) { limited = true; }
        check(limited, "presentation budget refuses capture during traversal");
    } finally { cls("combat.spi.CombatWorldLifecycleAccess").clear(runtime, entity.getUUID(), cls("effect.api.EffectService").Removal.WORLD_EXIT); entity.discard(); }
    return JSON.stringify({passed: checks.length, checks: checks});
})()
