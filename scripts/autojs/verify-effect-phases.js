// Isolated real server; a separate behavior coordinator uses a controlled scheduler clock.
(function () {
    function cls(n) { return Java.loadClass("io.github.naimjeg.combatnexus." + n); }
    var List = Java.loadClass("java.util.List"), Set = Java.loadClass("java.util.Set");
    var Optional = Java.loadClass("java.util.Optional"), Id = Java.loadClass("net.minecraft.resources.Identifier");
    var level = AutoJS.server().overworld(), world = cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    cls("combat.spi.CombatWorldLifecycleAccess").beginTick(world);
    var cow = Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level, Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    cow.setPos(0, 512, 0); cow.setNoAi(true); cow.setNoGravity(true); level.addFreshEntity(cow);
    var Phase = cls("combat.api.behavior.PeriodicPhase"), Healing = cls("combat.api.behavior.BehaviorComponent$PeriodicHealing");
    var Plan = cls("combat.api.behavior.BehaviorPlan"), Definition = cls("effect.api.EffectDefinition"), Lifetime = cls("effect.api.EffectLifetime");
    var ES = cls("effect.api.EffectService"), Policy = cls("effect.api.EffectPolicy");
    var Sources = cls("combat.internal.coordination.WorldBehaviorSources"), Port = cls("effect.spi.EffectContributionPort");
    var Prepared = cls("effect.spi.EffectContributionPort$Prepared"), Receipt = cls("effect.spi.EffectContributionPort$Receipt");
    var id = Id.parse("combatnexus:phase_verification"), checks = [], now = level.getGameTime();
    var nativePort = new Port({prepare:function(change) { return new Prepared({
        apply:function() { return new Receipt(List.of(), true, null); },
        reconcile:function() { return new Receipt(List.of(), true, null); }
    }); }});
    function check(value, label) { if (!value) throw new Error(label); checks.push(label); }
    function effect(ticks, phase, amount) {
        return new Definition(0, Lifetime.timed(ticks), ES.Stacking.REPLACE, Policy.DEFAULT, List.of(),
            new Plan(0, List.of(new Healing(amount, 20, -1, true, phase))), Set.of()).instantiate(cow.getUUID(), id, id, now);
    }
    function write(port, before, after) {
        check(port.prepare(new ES.Change(before === null ? List.of() : List.of(before), List.of(after), Optional.empty())).apply().completed(), "contribution write completes");
        port.activated(after);
    }
    try {
        var sources = new Sources(world);
        try {
            var port = sources.compose(nativePort), e = effect(45, Phase.REMAINING_DURATION, 1);
            cow.setHealth(1); write(port, null, e);
            check(cow.getHealth() === 1, "nonzero remainder does not trigger on application");
            sources.tick(now + 4); check(cow.getHealth() === 1, "remaining-duration phase waits for the first multiple");
            sources.tick(now + 5); check(cow.getHealth() === 2, "first remaining-duration multiple triggers once");
            sources.tick(now + 26); check(cow.getHealth() === 2, "missed modulo trigger is consumed without catch-up");
            sources.tick(now + 44); check(cow.getHealth() === 2, "late polling preserves original modulo phase");
            sources.tick(now + 45); check(cow.getHealth() === 2 && sources.capture(cow.getUUID()).isEmpty(), "expiry cancels the terminal zero-duration trigger");
        } finally { sources.close(); }
        sources = new Sources(world);
        try {
            var port2 = sources.compose(nativePort), initial = effect(40, Phase.REMAINING_DURATION, 1);
            cow.setHealth(1); write(port2, null, initial);
            check(cow.getHealth() === 2, "zero modulo phase triggers after commit");
            write(port2, initial, initial);
            check(cow.getHealth() === 2, "unchanged projection does not replay the initial trigger");
            sources.tick(now + 20); check(cow.getHealth() === 3, "last positive-duration multiple executes");
            sources.tick(now + 40); check(cow.getHealth() === 3, "no execution at expiration");
        } finally { sources.close(); }
        sources = new Sources(world);
        try {
            var port3 = sources.compose(nativePort), old = effect(80, Phase.PRESERVE, 1);
            write(port3, null, old);
            var saved = sources.save(cow.getUUID(), Set.of(id), now + 13);
            // Restore the pending seven-tick delay, then change both duration and payload.
            sources.restore(cow.getUUID(), saved, now);
            var refreshed = effect(120, Phase.PRESERVE, 2);
            write(port3, old, refreshed);
            var progress = sources.save(cow.getUUID(), Set.of(id), now).get(0).periodicRemaining();
            check(progress.get(new Healing(1, 20).type()) == 7, "refresh retains pending phase while updating behavior parameters");
            cow.setHealth(1); sources.tick(now + 6); check(cow.getHealth() === 1, "preserved trigger does not fire early");
            sources.tick(now + 7); check(cow.getHealth() === 3, "preserved trigger executes the updated amount once");
            var codec = cls("combat.internal.registration.BehaviorTypes");
            check(codec.decode(codec.encode(new Healing(2, 20, -1, true, Phase.PRESERVE))).phase() === Phase.PRESERVE, "behavior codec round-trips phase policy");
        } finally { sources.close(); }
        return JSON.stringify({passed:checks.length, checks:checks, scope:"real healing and contribution ports with controlled scheduler clock; no vanilla takeover"});
    } finally { cow.discard(); }
})()
