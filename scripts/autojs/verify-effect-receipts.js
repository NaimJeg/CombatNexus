// Controlled contribution-port failures; does not claim native modifier fault coverage.
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var prefix = "io.github.naimjeg.combatnexus.";
    function cls(name) { return Java.loadClass(prefix + name); }
    var List = Java.loadClass("java.util.List");
    var UUID = Java.loadClass("java.util.UUID");
    var Id = Java.loadClass("net.minecraft.resources.Identifier");
    var Port = cls("effect.spi.EffectContributionPort");
    var Prepared = cls("effect.spi.EffectContributionPort$Prepared");
    var Receipt = cls("effect.spi.EffectContributionPort$Receipt");
    var Exception = Java.loadClass("java.lang.IllegalStateException");
    var fail = true, compensate = false, calls = 0, retries = 0, bypass = false;
    var port = new Port({prepare: function (change) {
        var localBypass = bypass;
        return new Prepared({
            apply: function () { calls++; return new Receipt(List.of(), localBypass || !fail, !localBypass && fail ? new Exception("injected apply failure") : null); },
            reconcile: function () { retries++; return new Receipt(List.of(), localBypass || compensate, localBypass || compensate ? null : new Exception("injected compensation failure")); }
        });
    }});
    var service = new (cls("effect.internal.DefaultEffectService"))(function () { return false; }, port);
    var tick = AutoJS.server().overworld().getGameTime();
    service.tick(tick);
    var target = UUID.randomUUID();
    var effect = cls("effect.api.ManagedEffects").agility(target, Id.parse("combatnexus:receipt_check"), 0.5, tick + 100);
    var checks = [];
    function check(value, label) { if (!value) throw new Error(label); checks.push(label); }
    try {
        var rejected = false;
        try { service.apply(effect, cls("effect.api.EffectService").Stacking.REPLACE); } catch (error) { rejected = true; }
        check(rejected && service.snapshot(target).isEmpty(), "failed installation does not commit active state");
        rejected = false;
        try { service.apply(effect, cls("effect.api.EffectService").Stacking.REPLACE); } catch (error) { rejected = true; }
        check(rejected && calls === 1, "pending reconciliation blocks another application");
        bypass = true;
        var other = cls("effect.api.ManagedEffects").agility(target, Id.parse("combatnexus:independent_source"), 0.25, tick + 100);
        check(service.apply(other, cls("effect.api.EffectService").Stacking.REPLACE), "unrelated source proceeds while another source is pending");
        service.remove(target, other.snapshot().effect(), other.snapshot().source(), cls("effect.api.EffectService").Removal.REMOVED);
        bypass = false;
        var beforeRetries = retries;
        service.snapshot(target); service.pendingSources(target); service.restrictions(target); service.tick(tick);
        check(retries === beforeRetries, "queries and repeated tick do not retry pending compensation");
        service.tick(tick + 1); service.tick(tick + 1);
        check(retries === beforeRetries + 1, "failed compensation retried once per new tick");
        compensate = true; fail = false; service.tick(tick + 2);
        check(service.pendingSources(target).isEmpty(), "next tick completes compensation without a query trigger");
        check(service.apply(effect, cls("effect.api.EffectService").Stacking.REPLACE), "retry allowed after confirmed compensation");
        check(service.snapshot(target).size() === 1, "successful receipt commits active state");
        var stranger = UUID.randomUUID();
        var unrelated = cls("effect.api.ManagedEffects").agility(stranger, Id.parse("combatnexus:other_target"), 0.25, tick + 100);
        service.apply(unrelated, cls("effect.api.EffectService").Stacking.REPLACE);
        var visits = 0;
        service.snapshot(target, function (count) { visits += count; });
        check(visits === 1 && service.snapshot(stranger).size() === 1, "target projection budget excludes other entity state");
        var limited = false;
        try { service.snapshot(target, function () { throw new Error("budget exhausted"); }); } catch (error) { limited = true; }
        check(limited && service.snapshot(target).size() === 1, "budget refusal preserves authoritative state");
        service.clear(target, cls("effect.api.EffectService").Removal.DEATH);
        check(service.snapshot(target).isEmpty() && service.snapshot(stranger).size() === 1, "clear deletes only target group");
        service.tick(tick + 100);
        check(service.snapshot(stranger).isEmpty(), "expiry removes remaining target group");
        var backwards = false;
        try { service.tick(tick + 99); } catch (error) { backwards = true; }
        check(backwards, "backwards lifecycle time rejected");
    } finally { compensate = true; fail = false; service.close(); }
    return JSON.stringify({passed: checks.length, checks: checks});
})()
