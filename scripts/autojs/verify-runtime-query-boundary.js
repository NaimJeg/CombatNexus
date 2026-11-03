// Server-side probe; no lifecycle mutation should be caused by obtaining services.
(function () {
    var root = "io.github.naimjeg.combatnexus.";
    function cls(name) { return Java.loadClass(root + name); }
    var level = AutoJS.server().overworld(), lifecycle = cls("combat.platform.CombatWorldLifecycle");
    var runtime = cls("combat.internal.platform.PlatformWorldLifecycle").existing(level), session = runtime.sessionId(), checks = [];
    function check(ok, label) { if (!ok) throw new Error(label); checks.push(label); }
    for (var i = 0; i < 10; i++) {
        check(cls("combat.internal.platform.PlatformWorldLifecycle").existing(level).sessionId().equals(session), "same-tick lookup retains session " + i);
        cls("combat.spi.CombatWorldLifecycleAccess").beginTick(runtime);
    }
    for (var service of [runtime.effects(), runtime.shields(), runtime.healing(), runtime.regeneration(), runtime.panels()]) {
        for (var name of ["tick", "clear", "reset", "close", "drainChanges", "recordDamage"]) {
            check(typeof service[name] === "undefined", "business view excludes owner method: " + name);
        }
    }
    var isolated = new (cls("effect.internal.DefaultEffectService"))(function () { return false; });
    var retained = isolated.businessView(); isolated.tick(level.getGameTime()); isolated.close();
    var rejected = false;
    try { retained.snapshot(Java.loadClass("java.util.UUID").randomUUID()); } catch (error) { rejected = true; }
    check(rejected, "retained business service refuses queries after lifecycle close");
    return JSON.stringify({passed: checks.length, checks: checks});
})()
