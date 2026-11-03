// Run on the isolated NeoForge server through scripts/run-autojs.py.
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var prefix="io.github.naimjeg.combatnexus.";
    function cls(n){return Java.loadClass(prefix+n);}
    var Id=Java.loadClass("net.minecraft.resources.Identifier"), UUID=Java.loadClass("java.util.UUID");
    var Optional=Java.loadClass("java.util.Optional"), OL=Java.loadClass("java.util.OptionalLong");
    var List=Java.loadClass("java.util.List"), Set=Java.loadClass("java.util.Set");
    var level=AutoJS.server().overworld(), world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    var ES=cls("effect.api.EffectService"), EI=cls("effect.api.EffectInstance"), BP=cls("combat.api.behavior.BehaviorPlan");
    var checks=[], entities=[];
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function rejects(f){try{f();return false;}catch(e){return true;}}
    function cow(){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        e.setPos(0,100,0);level.addFreshEntity(e);entities.push(e);return e;}
    function effect(e,name,plan){return new EI(new (cls("effect.api.EffectSnapshot"))(e.getUUID(),Id.parse("combatnexus:"+name),Id.parse("combatnexus:"+name),0,OL.of(level.getGameTime()+200),Set.of()),List.of(),EI.Ownership.MANAGED,plan);}
    function install(e,name,plan){var v=effect(e,name,plan);check(world.effects().apply(v,ES.Stacking.REPLACE),"install "+name);return v;}
    function hit(v,a,amount){v.invulnerableTime=0;return cls("combat.damage.api.DamageModuleApi").submitDamage(cls("combat.damage.api.damage.DamageRequest").builder(level,v,
        cls("combat.damage.api.damage.DamageSourceDescriptor").from(v.damageSources().mobAttack(a)),amount).logicalAttacker(a).directEntity(a).build());}
    function heal(e,amount){return world.healing().submit(new (cls("combat.healing.api.HealingRequest"))(UUID.randomUUID(),e.getUUID(),new (cls("combat.api.CombatSource"))(Id.parse("combatnexus:verify"),Optional.empty(),Optional.empty()),cls("combat.healing.api.HealingRequest").Reason.EFFECT,amount));}
    try {

        var Due=cls("combat.api.scheduling.DueQueue"),queue=new Due(2);
        var first=queue.schedule(20,"first"),second=queue.schedule(20,"second");
        check(queue.poll(19).isEmpty(),"no early execution");
        check(rejects(function(){queue.schedule(21,"full");}),"task capacity rejects before insertion");
        var due=queue.poll(20);check(String(due.get(0).value())==="first"&&String(due.get(1).value())==="second","same tick stable order");
        check(queue.poll(20).isEmpty(),"tasks delivered once");
        var removed=queue.schedule(21,"removed");check(queue.cancel(removed)&&!queue.cancel(removed),"cancellation idempotent");
        check(queue.poll(100).isEmpty(),"cancelled task absent after skip");
        var v=cow();v.setHealth(1);
        var Sources=cls("combat.internal.coordination.WorldBehaviorSources"),sources=new Sources(world);
        var Port=cls("effect.spi.EffectContributionPort"),Prepared=cls("effect.spi.EffectContributionPort$Prepared"),Receipt=cls("effect.spi.EffectContributionPort$Receipt");
        var port=new Port({prepare:function(change){return new Prepared({apply:function(){return new Receipt(List.of(),true,null);},reconcile:function(){return new Receipt(List.of(),true,null);}});}});
        var now=level.getGameTime(), e=effect(v,"periodic",new BP(0,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$PeriodicHealing"))(1,20))));
        try {
            sources.compose(port).prepare(new ES.Change(List.of(),List.of(e),Optional.empty())).apply();
            sources.tick(now+19);check(v.getHealth()===1,"periodic healing waits full interval");
            sources.tick(now+20);check(v.getHealth()===2,"first interval heals once");
            sources.tick(now+20);check(v.getHealth()===2,"same tick does not heal twice");
            sources.tick(now+100);check(v.getHealth()===3,"skipped intervals do not catch up");
            sources.tick(now+200);check(v.getHealth()===3&&sources.capture(v.getUUID()).isEmpty(),"expiry tick cancels periodic action");
        } finally {sources.close();}
    } finally {for(var i=0;i<entities.length;i++){cls("combat.spi.CombatWorldLifecycleAccess").clear(world,entities[i].getUUID(),ES.Removal.WORLD_EXIT);entities[i].discard();}}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
