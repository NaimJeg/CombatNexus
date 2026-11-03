// Client-side controlled delivery probe. Requires an isolated connected client.
// Reflection is test-only: production generation/network paths use explicit contracts.
(function () {
    var root = "io.github.naimjeg.combatnexus.", Class = Java.loadClass("java.lang.Class");
    var Array = Java.loadClass("java.lang.reflect.Array"), ObjectClass = Class.forName("java.lang.Object");
    var Long = Java.loadClass("java.lang.Long"), Integer = Java.loadClass("java.lang.Integer");
    var UUID = Java.loadClass("java.util.UUID"), Stack = Java.loadClass("net.minecraft.world.item.ItemStack");
    var mc = Java.loadClass("net.minecraft.client.Minecraft").getInstance();
    if (mc.level === null || mc.player === null) throw new Error("Connect an isolated client first");
    var client = Class.forName(root + "presentation.platform.client.CombatPresentationClient");
    var pendingClass = Class.forName(root + "presentation.platform.client.CombatPresentationClient$Pending");
    var Payload = Java.loadClass(root + "presentation.network.CombatPresentationPayload");
    function args(values) {
        var result = Array.newInstance(ObjectClass, values.length);
        for (var i = 0; i < values.length; i++) Array.set(result, i, values[i]);
        return result;
    }
    function call(name, values) {
        for (var method of client.getDeclaredMethods()) if (String(method.getName()) === name) {
            method.setAccessible(true); return method.invoke(null, args(values));
        }
        throw new Error("Missing method: " + name);
    }
    function field(name) { var f = client.getDeclaredField(name); f.setAccessible(true); return f; }
    function pending(nonce, generation) {
        var constructor = pendingClass.getDeclaredConstructors()[0]; constructor.setAccessible(true);
        return constructor.newInstance(args([nonce, mc.level, Integer.valueOf(mc.player.containerMenu.containerId),
            Integer.valueOf(-1), Stack.EMPTY, Long.valueOf(Java.loadClass("java.lang.System").nanoTime()), Long.valueOf(generation)]));
    }
    function reply(nonce, session, generation) {
        return new Payload(nonce, session, mc.level.dimension().identifier(), generation, -1, Stack.EMPTY, '{"rows":["probe"]}');
    }
    var checks = [], generation = field("generation").getLong(null), session = UUID.randomUUID();
    if (generation < 1) throw new Error("Wait for login generation notification");
    function check(ok, label) { if (!ok) throw new Error(label); checks.push(label); }
    try {
        call("reconcile", []);
        var nonce = UUID.randomUUID(); field("panel").set(null, pending(nonce, generation));
        field("session").set(null, session);
        call("accept", [reply(nonce, session, generation + 1)]);
        check(field("panel").get(null) !== null, "unnotified generation rejected even with matching nonce and session");
        call("adoptGeneration", [Long.valueOf(generation + 1)]);
        check(field("panel").get(null) === null && field("item").get(null) === null, "new generation cancels all pending projections");
        call("accept", [reply(nonce, session, generation)]);
        check(field("generation").getLong(null) === generation + 1, "in-flight old reply cannot roll back notified generation");
        call("adoptGeneration", [Long.valueOf(generation)]);
        check(field("generation").getLong(null) === generation + 1, "stale notification ignored");
        call("clearWorld", []);
        check(field("generation").getLong(null) === generation + 1 && field("session").get(null) === null, "world transfer retains connection generation and drops world identity");
        call("clear", []);
        check(field("generation").getLong(null) === 0 && field("panel").get(null) === null, "disconnect clears connection generation and pending state");
        call("adoptGeneration", [Long.valueOf(generation)]);
        check(field("generation").getLong(null) === generation, "reconnect accepts a lower generation from a new server session");
    } finally {
        call("clear", []); call("adoptGeneration", [Long.valueOf(generation)]); call("reconcile", []);
    }
    return JSON.stringify({passed: checks.length, checks: checks, scope: "controlled client delivery; not real reload or reconnect transport"});
})()
