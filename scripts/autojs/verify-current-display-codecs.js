(function () {
    var root = "io.github.naimjeg.combatnexus.combat.damage.";
    var ops = Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE;
    var JsonParser = Java.loadClass("com.google.gson.JsonParser");
    var checks = 0;
    function check(value) { if (!value) throw new Error("Display codec check " + checks); checks++; }
    ["entry.DamageEntryDisplay", "affix.DamageAffixDisplay"].forEach(function (name) {
        var codec = Java.loadClass(root + "api.rule." + name).CODEC;
        var input = JsonParser.parseString(JSON.stringify({authored_summary:[{text:"Current summary"}],show_rule_breakdown:true}));
        var decoded = codec.parse(ops, input).getOrThrow();
        check(decoded.authoredSummary().size() === 1 && decoded.showRuleBreakdown());
        var encoded = codec.encodeStart(ops, decoded).getOrThrow();
        check(encoded.has("authored_summary") && !encoded.has("tooltip"));
        check(codec.parse(ops, JsonParser.parseString("{}")).getOrThrow().authoredSummary().isEmpty());
        var excessive = []; for (var i=0;i<33;i++) excessive.push({text:"line"});
        check(codec.parse(ops, JsonParser.parseString(JSON.stringify({authored_summary:excessive}))).error().isPresent());
    });
    var Selector = Java.loadClass(root + "command.test.TestTargetSelector");
    var Component = Java.loadClass("net.minecraft.network.chat.Component");
    check(Selector.isTestEntityName(Component.literal("[DamageModule Test] Cow")));
    check(!Selector.isTestEntityName(Component.literal("[DN-Test] Cow")));
    return JSON.stringify({passed:checks});
})()
