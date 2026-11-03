// Run only on an isolated AutoJS server. Exercises the real service with a controlled contribution port.
(function () {
    function cls(name) { return Java.loadClass("io.github.naimjeg.combatnexus." + name); }
    var List = Java.loadClass("java.util.List"), Set = Java.loadClass("java.util.Set"), UUID = Java.loadClass("java.util.UUID");
    var Id = Java.loadClass("net.minecraft.resources.Identifier"), Map = Java.loadClass("java.util.Map");
    var Lifetime = cls("effect.api.EffectLifetime"), Definition = cls("effect.api.EffectDefinition");
    var Stacking = cls("effect.api.EffectService$Stacking"), Policy = cls("effect.api.EffectPolicy");
    var Plan = cls("combat.api.behavior.BehaviorPlan"), Binding = cls("effect.api.EffectBinding");
    var Port = cls("effect.spi.EffectContributionPort"), Prepared = cls("effect.spi.EffectContributionPort$Prepared");
    var Receipt = cls("effect.spi.EffectContributionPort$Receipt"), Failure = Java.loadClass("java.lang.IllegalStateException");
    var checks = [], installs = 0;
    function check(value, label) { if (!value) throw new Error(label); checks.push(label); }
    function rejects(f) { try { f(); return false; } catch (e) { return true; } }
    var port = new Port({prepare:function(change) { return new Prepared({
        apply:function() { installs++; return new Receipt(List.of(), true, null); },
        reconcile:function() { return new Receipt(List.of(), true, null); }
    }); }});
    var service = new (cls("effect.internal.DefaultEffectService"))(function () { return false; }, port);
    var target = UUID.randomUUID(), id = Id.parse("combatnexus:lifetime_probe"), source = Id.parse("combatnexus:binding_probe");
    function definition(level, lifetime) { return new Definition(level, lifetime, Stacking.STRONGER_WITH_HIDDEN, Policy.DEFAULT, List.of(), Plan.NONE, Set.of()); }
    function instance(level, ticks, now) { return definition(level, Lifetime.timed(ticks)).instantiate(target, id, source, now); }
    service.tick(100);
    try {
        check(rejects(function () { Lifetime.timed(0); }), "zero timed lifetime is rejected");
        check(rejects(function () { new Lifetime(Lifetime.Kind.PERMANENT, 20); }), "permanent lifetime rejects an implicit timer");
        var permanent = definition(0, Lifetime.permanent()).instantiate(target, id, source, 100);
        service.bind(new Binding(permanent, true, Stacking.REPLACE));
        var before = installs;
        service.bind(new Binding(permanent, true, Stacking.REPLACE));
        check(installs === before, "unchanged condition does not reinstall contributions");
        service.tick(1000);
        check(service.snapshot(target).size() === 1, "permanent effect does not expire with elapsed time");
        service.bind(new Binding(permanent, false, Stacking.REPLACE));
        check(service.snapshot(target).isEmpty(), "condition end removes its permanent effect");
        service.bind(new Binding(permanent, false, Stacking.REPLACE));
        check(service.snapshot(target).isEmpty(), "repeated condition end is idempotent");
        var environmental = new Definition(0, Lifetime.permanent(), Stacking.REPLACE,
            new Policy(Policy.Category.NEUTRAL, false), List.of(), Plan.NONE, Set.of())
            .instantiate(target, id, source, 1000);
        var other = definition(0, Lifetime.timed(20)).instantiate(target, id, Id.parse("combatnexus:dispellable_probe"), 1000);
        service.apply(environmental, Stacking.REPLACE); service.apply(other, Stacking.REPLACE);
        service.clear(target, cls("effect.api.EffectService$Removal").DISPELLED);
        check(service.snapshot(target).size() === 1 && service.snapshot(target).get(0).equals(environmental),
            "bulk dispel skips condition-owned effects and clears ordinary effects");
        service.bind(new Binding(environmental, false, Stacking.REPLACE));
        service.apply(instance(0, 100, 1000), Stacking.STRONGER_WITH_HIDDEN);
        service.apply(instance(2, 20, 1000), Stacking.STRONGER_WITH_HIDDEN);
        check(service.snapshot(target).get(0).snapshot().level() === 2 && service.snapshot(target).get(0).hidden().size() === 1,
            "short strong effect suppresses the longer weak effect");
        service.tick(1020);
        check(service.snapshot(target).get(0).snapshot().level() === 0 && service.snapshot(target).get(0).snapshot().expiresAtTick().getAsLong() === 1100,
            "weak layer resumes with its original deadline");
        service.tick(1100);
        check(service.snapshot(target).isEmpty(), "restored layer expires at its own deadline");
        service.apply(instance(2, 100, 1100), Stacking.STRONGER_WITH_HIDDEN);
        service.apply(instance(0, 20, 1100), Stacking.STRONGER_WITH_HIDDEN);
        check(service.snapshot(target).get(0).hidden().isEmpty(), "fully covered weak effect cannot resurface");
        service.apply(instance(0, 200, 1100), Stacking.STRONGER_WITH_HIDDEN);
        var shifted = cls("effect.api.EffectLayers").rebase(service.snapshot(target).get(0), 1110, 2000);
        check(shifted.snapshot().expiresAtTick().getAsLong() === 2090 && shifted.hidden().get(0).snapshot().expiresAtTick().getAsLong() === 2190,
            "save restore shifts both active and hidden deadlines");
        var saved = new (cls("combat.internal.persistence.CombatPersistentState"))(target, 1110, Set.of(source),
            service.snapshot(target), List.of(), List.of(), List.of(), List.of(), List.of());
        var Codec = cls("combat.internal.persistence.CombatStateCodec");
        var decodedState = Codec.decode(Codec.encode(saved));
        check(decodedState.effects().get(0).equals(service.snapshot(target).get(0)), "storage codec round-trips lifetimes and hidden layers");
        service.tick(1300);
        check(service.snapshot(target).isEmpty(), "skipped ticks do not resurrect expired hidden layers");
        var instant = definition(0, Lifetime.instant()).instantiate(target, id, source, 1300);
        check(!service.applyInstant(instant).applied(), "unsupported instant port returns an explicit rejection");
        service.tick(1400);
        check(service.snapshot(target).isEmpty(), "rejected instant is not retained or replayed by ticking");
        check(rejects(function () { new Binding(instant, true, Stacking.REPLACE); }), "instant cannot masquerade as a condition-bound state");
        var periodic = new Plan(0, List.of(new (cls("combat.api.behavior.BehaviorComponent$PeriodicHealing"))(1, 20)));
        check(rejects(function () { new Definition(0, Lifetime.instant(), Stacking.REPLACE, Policy.DEFAULT, List.of(), periodic, Set.of()); }),
            "instant definitions reject recurring behaviors before installation");
        var Json = Java.loadClass("com.google.gson.JsonParser");
        var template = {level:0,lifetime:{kind:"PERMANENT"},stacking:"combatnexus:replace",category:"NEUTRAL",dispellable:false,attributes:[],behaviors:[],priority:0,restrictions:[]};
        var decoded = Definition.decode(Json.parseString(JSON.stringify(template)).getAsJsonObject(), Map.of());
        check(String(decoded.lifetime().kind()) === "PERMANENT", "datapack definition decodes explicit permanent lifetime");
        return JSON.stringify({passed:checks.length,checks:checks,scope:"common lifecycle and controlled contribution port; not native takeover acceptance",
            java:String(Java.loadClass("java.lang.System").getProperty("java.version")),neoforge:"26.1.2.126"});
    } finally { service.close(); }
})()
