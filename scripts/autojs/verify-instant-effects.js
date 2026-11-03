// Requires prepare-architecture.js to report prepared=true on an isolated server.
(function () {
    function cls(name) { return Java.loadClass("io.github.naimjeg.combatnexus." + name); }
    var level = AutoJS.server().overworld();
    cls("combat.spi.CombatWorldLifecycleAccess").beginTick(cls("combat.internal.platform.PlatformWorldLifecycle").existing(level));
    var UUID = Java.loadClass("java.util.UUID"), Id = Java.loadClass("net.minecraft.resources.Identifier");
    var List = Java.loadClass("java.util.List"), Set = Java.loadClass("java.util.Set");
    var cow = Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level, Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    cow.setPos(0, 100, 0); cow.setNoAi(true); level.addFreshEntity(cow);
    var checks = [], api = cls("combat.api.CombatAccess").find(level).orElseThrow();
    function check(value, label) { if (!value) throw new Error(label); checks.push(label); }
    function effect(amount) {
        var plan = new (cls("combat.api.behavior.BehaviorPlan"))(0,
            List.of(new (cls("combat.api.behavior.BehaviorComponent$InstantHealing"))(amount)));
        var definition = new (cls("effect.api.EffectDefinition"))(0, cls("effect.api.EffectLifetime").instant(),
            cls("effect.api.EffectService$Stacking").REPLACE, cls("effect.api.EffectPolicy").DEFAULT, List.of(), plan, Set.of());
        return definition.instantiate(cow.getUUID(), Id.parse("combatnexus:instant_probe"), Id.parse("combatnexus:instant_probe"), level.getGameTime());
    }
    function request(value) { return new (cls("effect.api.EffectApplication"))(UUID.randomUUID(), value, cls("effect.api.EffectService$Stacking").REPLACE); }
    try {
        check(level.getEntity(cow.getUUID()) !== null, "native target is available");
        cow.setHealth(2);
        var application = request(effect(4));
        var result = api.effects().apply(application);
        check(String(result.outcome()) === "APPLIED" && cow.getHealth() === 6, "instant effect applies child healing once");
        check(!result.journal().entries().isEmpty(), "instant effect retains child resource journal");
        check(api.effects().snapshot(cow.getUUID()).isEmpty(), "instant effect leaves no activity or display state");
        var duplicate = api.effects().apply(application);
        check(String(duplicate.outcome()) === "REJECTED" && cow.getHealth() === 6, "duplicate application cannot repeat healing");
        var invalid = api.effects().apply(request(effect(0)));
        check(String(invalid.outcome()) === "REJECTED" && cow.getHealth() === 6, "refused child is not reported as a successful instant effect");
        check(api.effects().snapshot(cow.getUUID()).isEmpty(), "refused instant does not leave an active instance");
        var Definition = cls("effect.api.EffectDefinition"), Lifetime = cls("effect.api.EffectLifetime"), Binding = cls("effect.api.EffectBinding");
        var permanent = new Definition(0, Lifetime.permanent(), cls("effect.api.EffectService$Stacking").REPLACE,
            cls("effect.api.EffectPolicy").DEFAULT, List.of(), cls("combat.api.behavior.BehaviorPlan").NONE, Set.of())
            .instantiate(cow.getUUID(), Id.parse("combatnexus:condition_probe"), Id.parse("combatnexus:condition_probe"), level.getGameTime());
        check(String(api.effects().bind(UUID.randomUUID(), new Binding(permanent, true, cls("effect.api.EffectService$Stacking").REPLACE)).outcome()) === "APPLIED",
            "public condition binding installs a permanent effect");
        check(api.effects().snapshot(cow.getUUID()).size() === 1, "permanent effect is visible in shared facts");
        check(String(api.effects().bind(UUID.randomUUID(), new Binding(permanent, false, cls("effect.api.EffectService$Stacking").REPLACE)).outcome()) === "REMOVED",
            "public condition binding removes its own effect");
        var periodic = new (cls("combat.api.behavior.BehaviorPlan"))(0,
            List.of(new (cls("combat.api.behavior.BehaviorComponent$PeriodicHealing"))(1, 20)));
        var effectId = Id.parse("combatnexus:phase_probe"), stack = cls("effect.api.EffectService$Stacking").STRONGER_WITH_HIDDEN;
        var layer = function (grade, ticks) { return new Definition(grade, Lifetime.timed(ticks), stack,
            cls("effect.api.EffectPolicy").DEFAULT, List.of(), periodic, Set.of()).instantiate(cow.getUUID(), effectId, effectId, level.getGameTime()); };
        api.effects().apply(new (cls("effect.api.EffectApplication"))(UUID.randomUUID(), layer(2, 80), stack));
        var runtime = cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
        var installation = runtime.behaviors().capture(cow.getUUID()).get(0).installation();
        var weaker = api.effects().apply(new (cls("effect.api.EffectApplication"))(UUID.randomUUID(), layer(0, 200), stack));
        check(String(weaker.outcome()) === "APPLIED", "weaker longer layer is retained by the public effect API");
        check(runtime.behaviors().capture(cow.getUUID()).get(0).installation() === installation,
            "hidden-only change preserves the active behavior installation");
        var pulse = new (cls("combat.api.behavior.BehaviorPlan"))(0,
            List.of(new (cls("combat.api.behavior.BehaviorComponent$PeriodicHealing"))(1, 20, 0, true)));
        var pulseId = Id.parse("combatnexus:initial_pulse_probe");
        var pulseInstance = new Definition(0, Lifetime.timed(40), cls("effect.api.EffectService$Stacking").REPLACE,
            cls("effect.api.EffectPolicy").DEFAULT, List.of(), pulse, Set.of()).instantiate(cow.getUUID(), pulseId, pulseId, level.getGameTime());
        cow.setHealth(2);
        api.effects().apply(request(pulseInstance));
        check(cow.getHealth() === 3, "zero-delay pulse runs after the effect is committed");
        api.effects().apply(request(pulseInstance));
        check(cow.getHealth() === 3, "unchanged installation does not replay its initial pulse");
        var eventCount = 0;
        var bus = Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS;
        var observer = new (Java.loadClass("java.util.function.Consumer"))({accept:function(event) {
            if (event.getEntity().getUUID().equals(cow.getUUID())) eventCount++;
        }});
        bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").LOWEST, false,
            Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingHealEvent"), observer);
        try {
            cow.setHealth(cow.getMaxHealth());
            var fullId = Id.parse("combatnexus:full_pulse_probe");
            api.effects().apply(request(new Definition(0, Lifetime.timed(40), cls("effect.api.EffectService$Stacking").REPLACE,
                cls("effect.api.EffectPolicy").DEFAULT, List.of(), pulse, Set.of()).instantiate(cow.getUUID(), fullId, fullId, level.getGameTime())));
            check(eventCount === 0, "missing-health condition skips the heal call at full health");
        } finally { bus.unregister(observer); }
        return JSON.stringify({passed:checks.length,checks:checks,scope:"real effect API, healing child receipt and condition binding; no vanilla takeover"});
    } finally { cow.discard(); }
})()
