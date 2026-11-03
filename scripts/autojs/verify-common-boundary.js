// Run with AutoJS eval_js on a disposable NeoForge 26.1.2 server (server side).
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var root = "io.github.naimjeg.combatnexus.combat.damage.";
    var Api = Java.loadClass(root + "api.DamageModuleApi");
    var Request = Java.loadClass(root + "api.damage.DamageRequest");
    var Source = Java.loadClass(root + "api.damage.DamageSourceDescriptor");
    var Tracker = Java.loadClass(root + "core.settlement.DamageSettlementTracker");
    var Queue = Java.loadClass(root + "core.settlement.DamageSettlementCoordinator");
    var Content = Java.loadClass(root + "platform.DamageContent");
    var Builtins = Java.loadClass(root + "builtin.BuiltinDamageProcessors");
    var EntityType = Java.loadClass("net.minecraft.world.entity.EntityType");
    var Reason = Java.loadClass("net.minecraft.world.entity.EntitySpawnReason");
    var level = AutoJS.server().overworld();
    var checks = [];
    function check(value, label) {
        if (!value) throw new Error(label);
        checks.push(label);
    }
    function idle() {
        check(!Tracker.hasActiveHurt() && Queue.pendingCountForTests() === 0,
              "hurt scope and completion FIFO cleaned");
    }
    check(Content.block() !== null && Content.blockEntity() !== null &&
          Content.entity() !== null && Content.menu() !== null, "lazy content handles resolve");
    check(Builtins.builtInProcessors().size() === 21, "21 built-in processors");
    var victim = EntityType.COW.create(level, Reason.COMMAND);
    victim.setPos(0, 100, 0);
    level.addFreshEntity(victim);
    try {
        var source = Source.from(victim.damageSources().generic());
        var request = Request.builder(level, victim, source, 4).build();
        var before = victim.getHealth();
        var result = Api.submitDamage(request);
        check(String(result.status()) === "APPLIED", "normal damage applied");
        check(result.settlement().isPresent() && victim.getHealth() < before,
              "synchronous completed settlement and health loss");
        idle();
        var replay = Api.submitDamage(request);
        check(String(replay.status()) === "REJECTED", "duplicate request rejected");
        idle();
        victim.invulnerableTime = 0;
        var zero = Api.submitDamage(Request.builder(level, victim, source, 0).build());
        check(String(zero.status()) !== "APPLIED", "zero damage boundary");
        idle();
        var invalid = false;
        try { Request.builder(level, victim, source, -1).build(); }
        catch (error) { invalid = true; }
        check(invalid, "negative damage rejected");
        idle();
    } finally { victim.discard(); }
    return JSON.stringify({passed: checks.length, checks: checks});
})()
