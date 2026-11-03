// Controlled client-side projection/codec probe; does not verify login/reload transport or rendering.
(function () {
    var prefix = "io.github.naimjeg.combatnexus.", checks = [];
    function cls(name) { return Java.loadClass(prefix + name); }
    function check(ok, label) { if (!ok) throw new Error(label); checks.push(label); }
    var Cache = cls("combat.damage.client.ClientFormulaSettings"), saved = Cache.current();
    var Payload = cls("combat.damage.network.payload.FormulaSettingsPayload");
    var Id = Java.loadClass("net.minecraft.resources.Identifier");
    try {
        for (var bits = 0; bits < 4; bits++) {
            var payload = new Payload(bits + 1, 15, 50, 3.5, !!(bits & 1), !!(bits & 2));
            var buffer = Java.loadClass("io.netty.buffer.Unpooled").buffer();
            try {
                Payload.STREAM_CODEC.encode(buffer, payload);
                check(payload.equals(Payload.STREAM_CODEC.decode(buffer)) && buffer.readableBytes() === 0, "display ownership codec roundtrip " + bits);
            } finally { buffer.release(); }
            Cache.clear(); Cache.accept(payload);
        }
        Cache.clear();
        Cache.accept(new Payload(2, 15, 50, 3.5, false, false));
        Cache.accept(new Payload(1, 15, 50, 3.5, true, true));
        check(!Cache.current().get().managedArmor() && !Cache.current().get().managedEnchantments(), "stale payload cannot restore obsolete ownership");

        var Class = Java.loadClass("java.lang.Class"), Array = Java.loadClass("java.lang.reflect.Array");
        function args(values) {
            var result = Array.newInstance(Class.forName("java.lang.Object"), values.length);
            for (var i = 0; i < values.length; i++) Array.set(result, i, values[i]);
            return result;
        }
        var screenClass = Class.forName(prefix + "presentation.platform.client.CombatPanelScreen");
        var constructor = screenClass.getDeclaredConstructors()[0]; constructor.setAccessible(true);
        var screen = constructor.newInstance(args([]));
        var json = Java.loadClass("com.google.gson.JsonParser").parseString(JSON.stringify([
            {name:"attribute.name.combatnexus.lifesteal", value:0.25, base:0, unit:"CONVERSION_RATIO", reserved:false},
            {name:"attribute.name.combatnexus.dodge_chance", value:0.5, base:0, unit:"PROBABILITY", reserved:true}
        ])).getAsJsonArray();
        for (var method of screenClass.getDeclaredMethods()) if (String(method.getName()) === "accept") {
            method.setAccessible(true); method.invoke(screen, args([json]));
        }
        var rows = screen.rows(), Component = Java.loadClass("net.minecraft.network.chat.Component");
        var marker = String(Component.translatable("gui.combatnexus.panel.reserved").getString());
        var conversion = String(Component.translatable("gui.combatnexus.unit.conversion_ratio").getString());
        check(rows.size() === 2 && String(rows.get(0).getString()).indexOf("25.00%") >= 0
            && String(rows.get(0).getString()).indexOf(conversion) >= 0, "conversion ratio formatted as percent with correct unit");
        check(String(rows.get(1).getString()).indexOf(marker) >= 0 && String(rows.get(0).getString()).indexOf(marker) < 0,
            "only reserved attribute displays unsupported-consumer marker");
    } finally { Cache.clear(); if (saved.isPresent()) Cache.accept(saved.get()); }
    return JSON.stringify({passed:checks.length,checks:checks,scope:"controlled client projection; no real network transport"});
})()
