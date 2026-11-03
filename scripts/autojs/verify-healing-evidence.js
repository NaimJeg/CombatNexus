// Controlled native-port evidence and exception paths on an isolated server.
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var prefix = "io.github.naimjeg.combatnexus.";
    function cls(name) { return Java.loadClass(prefix + name); }
    var UUID = Java.loadClass("java.util.UUID"), Optional = Java.loadClass("java.util.Optional");
    var Id = Java.loadClass("net.minecraft.resources.Identifier");
    var Port = cls("combat.healing.spi.HealingApplicationPort");
    var Receipt = cls("combat.healing.spi.HealingApplicationPort$Receipt");
    var State = cls("combat.healing.spi.HealingApplicationPort$State");
    var Request = cls("combat.healing.api.HealingRequest");
    var level = AutoJS.server().overworld();
    var cow = Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,
        Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    cow.setPos(0,100,0); level.addFreshEntity(cow);
    var mode = "partial", delivered = [];
    var port = new Port({
        target: function () { return cow; },
        apply: function () {
            if (mode === "throw") throw new (Java.loadClass("java.lang.IllegalStateException"))("injected native failure");
            return new Receipt(State.WRITTEN, 2, 5, 10, 6, false);
        }
    });
    var server = cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server());
    var service = new (cls("combat.healing.internal.DefaultHealingService"))(cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server()), cls("combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()), port, function(registry) { registry.register(Id.parse("combatnexus:evidence_check"), 0, function(callback) { delivered.push(String(callback.result().outcome())); }); });
    var source = new (cls("combat.api.CombatSource"))(Id.parse("combatnexus:evidence_check"), Optional.empty(), Optional.empty());
    var target = cow.getUUID();

    var checks = [];
    function check(ok, label) { if (!ok) throw new Error(label); checks.push(label); }
    function request() { return new Request(UUID.randomUUID(), target, source, Request.Reason.LIFESTEAL, 2); }
    try {
        var partial = service.submit(request());
        check(String(partial.outcome()) === "PARTIAL" && partial.actualRecovery() === 1, "confirmed partial native write preserved");
        mode = "throw";
        var failed = false;
        try { service.submit(request()); } catch (error) { failed = true; }
        check(failed, "native exception propagates");
        check(delivered.length === 2 && delivered[0] === "PARTIAL" && delivered[1] === "UNKNOWN", "failed admitted operation terminates and notifies once");
        check(cls("combat.internal.transaction.CombatCompletionQueue").pendingCount() === 0, "failure drains FIFO");
    } finally { service.close(); cow.discard(); }
    return JSON.stringify({passed: checks.length, checks: checks});
})()
