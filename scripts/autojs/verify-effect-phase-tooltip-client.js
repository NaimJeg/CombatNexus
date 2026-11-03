// Real client resource manager, both languages; no server or personal world is needed.
(function () {
    function cls(n) { return Java.loadClass("io.github.naimjeg.combatnexus." + n); }
    var mc = Java.loadClass("net.minecraft.client.Minecraft").getInstance();
    var List = Java.loadClass("java.util.List"), Set = Java.loadClass("java.util.Set"), Optional = Java.loadClass("java.util.Optional");
    var OL = Java.loadClass("java.util.OptionalLong"), UUID = Java.loadClass("java.util.UUID"), Id = Java.loadClass("net.minecraft.resources.Identifier");
    var Component = Java.loadClass("net.minecraft.network.chat.Component"), Phase = cls("combat.api.behavior.PeriodicPhase");
    var checks = [], text = {}, previous = String(mc.getLanguageManager().getSelected());
    function check(value, label) { if (!value) throw new Error(label); checks.push(label); }
    try {
        for (var language of ["en_us", "zh_cn"]) {
            mc.getLanguageManager().setSelected(language);
            mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
            for (var phase of [Phase.RESTART, Phase.PRESERVE, Phase.REMAINING_DURATION]) {
                var id = Id.parse("combatnexus:phase_tooltip_probe");
                var definition = new (cls("effect.api.EffectDefinition"))(0, cls("effect.api.EffectLifetime").timed(45),
                    cls("effect.api.EffectService$Stacking").REPLACE, cls("effect.api.EffectPolicy").DEFAULT, List.of(),
                    new (cls("combat.api.behavior.BehaviorPlan"))(0, List.of(new (cls("combat.api.behavior.BehaviorComponent$PeriodicHealing"))(1, 20, -1, true, phase))), Set.of());
                var view = new (cls("effect.api.EffectView"))(UUID.randomUUID(), id, Optional.of(id), Optional.empty(),
                    cls("effect.api.EffectInstance$Ownership").MANAGED, Set.of(), 2, OL.of(30), Optional.empty(),
                    cls("effect.api.EffectView$Completeness").COMPLETE, Set.of());
                var block = cls("effect.api.EffectTooltip").instance(id, definition, view, Component.literal("SOURCE_PROBE"));
                var lines = [], key = "tooltip.combatnexus.effect.phase." + String(phase).toLowerCase();
                for (var line of block.details()) lines.push(String(line.getString()));
                var expected = String(Component.translatable(key).getString());
                check(expected !== key, language + " " + phase + " translation exists");
                check(lines.indexOf(expected) >= 0, language + " " + phase + " instance tooltip retains execution phase");
                check(lines[0] === String(Component.translatable("tooltip.combatnexus.effect.level", "2").getString()), language + " " + phase + " instance level replaces the level row only");
                check(lines.some(function(s) { return s.indexOf("SOURCE_PROBE") >= 0; }), language + " " + phase + " source detail retained");
                text[language + "/" + phase] = lines;
            }
        }
        return JSON.stringify({passed:checks.length, checks:checks, text:text, scope:"client localization and definition/instance tooltip generation; no native effect projection or visual layout acceptance"});
    } finally {
        mc.getLanguageManager().setSelected(previous);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
    }
})()
