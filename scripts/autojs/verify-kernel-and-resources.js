// Admission, native shield participation, lifesteal and bounded FIFO assertions.
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var prefix = "io.github.naimjeg.combatnexus.";
    function cls(name) { return Java.loadClass(prefix + name); }
    var Id = Java.loadClass("net.minecraft.resources.Identifier"), UUID = Java.loadClass("java.util.UUID");
    var Optional = Java.loadClass("java.util.Optional"), OptionalLong = Java.loadClass("java.util.OptionalLong");
    var Set = Java.loadClass("java.util.Set");
    var level = AutoJS.server().overworld();
    var kernel = cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server());
    var runtime = cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    var Api = cls("combat.damage.api.DamageModuleApi"), Request = cls("combat.damage.api.damage.DamageRequest");
    var Source = cls("combat.damage.api.damage.DamageSourceDescriptor");
    var Operation = cls("combat.api.transaction.CombatOperation"), Lineage = cls("combat.api.transaction.CombatLineage");
    var Queue = cls("combat.internal.transaction.CombatCompletionQueue");
    var Phase = cls("combat.internal.transaction.CombatExecutionScope").Phase;
    var entityType = Java.loadClass("net.minecraft.world.entity.EntityType"), reason = Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND;
    var victim = entityType.COW.create(level, reason), attacker = entityType.COW.create(level, reason);
    victim.setPos(0,100,0); attacker.setPos(1,100,0); level.addFreshEntity(victim); level.addFreshEntity(attacker);
    var checks = [];
    function check(ok,label) { if(!ok) throw new Error(label); checks.push(label); }
    function rejected(action) { try { action(); return false; } catch(error) { return true; } }
    var attributes = cls("attribute.api.CombatAttributes");
    var nativeAttributes = Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes");
    var source = Source.from(victim.damageSources().mobAttack(attacker));
    var poolSource = new (cls("combat.api.CombatSource"))(Id.parse("combatnexus:kernel_check"), Optional.empty(), Optional.empty());
    var Pool = cls("combat.shield.api.ShieldGrant");
    try {
        var queue = new Queue(2), first = queue.reserve(), second = queue.reserve(), delivered = [];
        check(rejected(function(){queue.reserve();}), "notification capacity rejected before resource application");
        first.publish(function(){delivered.push("first"); var child=queue.reserve(); child.publish(function(){delivered.push("child");});});
        second.publish(function(){delivered.push("second");});
        Queue.drainIfSafe();
        check(delivered.join(",") === "first,second,child", "child completions append to server FIFO");
        var stale=queue.reserve(); queue.clear();
        check(rejected(function(){stale.publish(function(){});}), "completion capacity authority expires after clear");
        var lineage=Lineage.root(Operation.HEALING); kernel.admit(lineage);
        var ticket=kernel.ticket(lineage); ticket.bindSession(level, victim.getUUID());
        check(rejected(function(){ticket.bindSession(AutoJS.server().getLevel(Java.loadClass("net.minecraft.world.level.Level").NETHER),victim.getUUID());}),
            "ticket rejects another world");
        var scope=ticket.enter(Phase.COMPUTE);
        try {
            check(rejected(function(){runtime.effects().apply(cls("effect.api.ManagedEffects").agility(victim.getUUID(),Id.parse("combatnexus:denied"),1,level.getGameTime()+10),
                    cls("effect.api.EffectService").Stacking.REPLACE);}), "effect gameplay write rejected during compute");
            check(rejected(function(){runtime.shields().remove(victim.getUUID(),poolSource.id(),(Java.loadClass("io.github.naimjeg.combatnexus.combat.shield.api.ShieldPoolSnapshot$RemovalReason")).REMOVED);}),
                "shield removal rejected during compute");
            check(rejected(function(){runtime.regeneration().tick(level.getGameTime()+1,runtime.healing());}),
                "recovery clock mutation rejected during compute");
            check(rejected(function(){runtime.healing().close();}),
                "healing lifecycle close rejected during compute");
        } finally {scope.close();kernel.finish(lineage);}
        check(rejected(function(){ticket.enter(Phase.APPLICATION);}), "closed admission ticket cannot reopen application");
        runtime.shields().replace(victim.getUUID(),new Pool(poolSource,new (Java.loadClass("io.github.naimjeg.combatnexus.combat.shield.api.ShieldDefinition"))(10,0,Set.of(),Set.of(),0,0,(Java.loadClass("io.github.naimjeg.combatnexus.combat.shield.api.ShieldDefinition$Stacking")).REPLACE_SAME_SOURCE),10,OptionalLong.empty()));
        runtime.shields().replace(attacker.getUUID(),new Pool(poolSource,new (Java.loadClass("io.github.naimjeg.combatnexus.combat.shield.api.ShieldDefinition"))(3,0,Set.of(),Set.of(),0,0,(Java.loadClass("io.github.naimjeg.combatnexus.combat.shield.api.ShieldDefinition$Stacking")).REPLACE_SAME_SOURCE),3,OptionalLong.empty()));
        var visited = 0;
        check(runtime.shields().snapshot(victim.getUUID(),function(n){visited+=n;}).size()===1 && visited===1,
            "shield projection visits only requested target group");
        check(rejected(function(){runtime.shields().snapshot(victim.getUUID(),function(){throw new Error("budget");});}),
            "shield projection refuses exhausted budget before adding rows");
        runtime.shields().replace(victim.getUUID(),new Pool(poolSource,new (Java.loadClass("io.github.naimjeg.combatnexus.combat.shield.api.ShieldDefinition"))(10,0,Set.of(),Set.of(),0,0,(Java.loadClass("io.github.naimjeg.combatnexus.combat.shield.api.ShieldDefinition$Stacking")).REPLACE_SAME_SOURCE),10,OptionalLong.empty()));
        check(runtime.shields().snapshot(victim.getUUID()).size()===1,"same source replacement does not duplicate target index");
        runtime.shields().remove(attacker.getUUID(),poolSource.id(),(Java.loadClass("io.github.naimjeg.combatnexus.combat.shield.api.ShieldPoolSnapshot$RemovalReason")).REMOVED);
        check(runtime.shields().snapshot(attacker.getUUID()).isEmpty() && runtime.shields().snapshot(victim.getUUID()).size()===1,
            "removing another target group preserves victim shield");
        var before=victim.getHealth(), count=kernel.currentTickCount();
        var shielded=Api.submitDamage(Request.builder(level,victim,source,4).logicalAttacker(attacker).build());
        check(String(shielded.status())==="APPLIED" && victim.getHealth()===before, "full custom shield remains applied with zero native health loss");
        check(shielded.settlement().get().evidence().shieldLoss()>0 && !shielded.settlement().get().evidence().journal().entries().isEmpty(),
            "shield confirmation enters damage journal");
        check(kernel.currentTickCount()===count+1, "enlisted shield reuses admitted damage allowance");
        runtime.shields().remove(victim.getUUID(),poolSource.id(),(Java.loadClass("io.github.naimjeg.combatnexus.combat.shield.api.ShieldPoolSnapshot$RemovalReason")).REMOVED);
        attacker.getAttribute(attributes.lifesteal()).setBaseValue(0.5);
        attacker.getAttribute(attributes.healingReceived()).setBaseValue(2);
        attacker.setHealth(2); victim.setHealth(victim.getMaxHealth()); victim.invulnerableTime=0;
        before=attacker.getHealth();count=kernel.currentTickCount();
        var hit=Api.submitDamage(Request.builder(level,victim,source,4).logicalAttacker(attacker).build());
        var loss=hit.settlement().get().evidence().healthLoss();
        check(Math.abs(attacker.getHealth()-before-loss)<0.001, "lifesteal uses confirmed health loss and received multiplier once");
        check(kernel.currentTickCount()===count+2, "lifesteal child shares server admission kernel");
        var nativeJournal=hit.settlement().get().evidence().journal();
        check(nativeJournal.applicationCompleted() && nativeJournal.entries().stream().anyMatch(function(entry){return String(entry.observation())==="NATIVE_WRITE";}),
            "native health write captured separately from whole-call observations");
    } finally {
        cls("combat.spi.CombatWorldLifecycleAccess").clear(runtime,victim.getUUID(),cls("effect.api.EffectService").Removal.WORLD_EXIT);
        cls("combat.spi.CombatWorldLifecycleAccess").clear(runtime,attacker.getUUID(),cls("effect.api.EffectService").Removal.WORLD_EXIT);
        victim.discard();attacker.discard();
    }
    return JSON.stringify({passed:checks.length,checks:checks});
})()
