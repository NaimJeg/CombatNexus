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

        check(rejects(function(){world.reactions().clear();}),"public reaction view cannot erase quotas or delivery identity");
        check(rejects(function(){world.reactions().removeSource(1);}),"public reaction view cannot reset source allowance");
        check(rejects(function(){world.reactions().restore(null);}),"compensation is not a public operation");
        var v=cow(),a=cow();
        install(v,"reflect",new BP(0,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$Reflection"))(0.2,(Java.loadClass("io.github.naimjeg.combatnexus.combat.api.action.TriggerLimit")).ONCE))));
        install(a,"reflect_peer",new BP(0,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$Reflection"))(0.2,(Java.loadClass("io.github.naimjeg.combatnexus.combat.api.action.TriggerLimit")).ONCE))));
        var victimBefore=v.getHealth();
        var before=a.getHealth(),result=hit(v,a,4), loss=result.settlement().get().evidence().healthLoss();
        check(a.getHealth()<before,"reflection submitted native damage");
        check(world.reactions().facts().stream().anyMatch(function(f){return String(f.reaction())==="combatnexus:recovery_delay"&&String(f.outcome())==="executed";}),"confirmed native damage records recovery delay through settlement authority");
        check(Math.abs(victimBefore-v.getHealth()-loss)<0.0001,"reflected damage does not reflect recursively");
        check(world.reactions().facts().stream().anyMatch(function(f){return String(f.reaction())==="combatnexus:reflection"&&String(f.outcome())==="child_applied";}),"reflection recorded");
        install(a,"overflow",new BP(0,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$OverflowShield"))(1,20,100,(Java.loadClass("io.github.naimjeg.combatnexus.combat.api.action.TriggerLimit")).ONCE))));a.setHealth(a.getMaxHealth());
        var healed=heal(a,4);
        check(healed.overflow()===4,"full health has confirmed overflow");
        check(world.shields().snapshot(a.getUUID()).stream().mapToDouble(function(p){return p.balance();}).sum()===4,"full health overflow grants shield");
        heal(a,100);check(world.shields().snapshot(a.getUUID()).stream().mapToDouble(function(p){return p.balance();}).sum()===20,"overflow shield capacity bounded");
        check(cls("combat.internal.transaction.CombatCompletionQueue").pendingCount()===0,"reaction FIFO drained");
        var limited=cow(),Limit=cls("combat.api.action.TriggerLimit");
        var one=install(limited,"limited_overflow",new BP(0,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$OverflowShield"))(1,20,100,new Limit(Limit.Scope.SOURCE,1)))));
        limited.setHealth(limited.getMaxHealth());heal(limited,2);
        check(rejects(function(){world.reactions().clear();}),"attempted reset cannot erase consumed source limit");
        heal(limited,2);
        check(world.shields().snapshot(limited.getUUID()).stream().mapToDouble(function(p){return p.balance();}).sum()===2,"source allowance remains consumed across operations");
        world.effects().remove(limited.getUUID(),one.snapshot().effect(),one.snapshot().source(),ES.Removal.REMOVED);
        check(world.shields().snapshot(limited.getUUID()).isEmpty(),"source revocation removes its overflow pool");
        var saved=null, Port=cls("combat.healing.spi.HealingApplicationPort"), State=cls("combat.healing.spi.HealingApplicationPort$State"), Receipt=cls("combat.healing.spi.HealingApplicationPort$Receipt");
        var mode="cancel", port=new Port({target:function(){return a;},apply:function(){
            if(mode==="throw")throw new Error("injected native exception");
            if(mode==="partial")return new Receipt(State.WRITTEN,4,10,10,10,false);
            return new Receipt(State.CANCELLED,0,10,10,10,true);
        }}),service=new (cls("combat.healing.internal.DefaultHealingService"))(cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server()), world,port,function(registry){registry.register(Id.parse("combatnexus:reaction_verification"),0,function(c){saved=c;});});
        var request=function(){return new (cls("combat.healing.api.HealingRequest"))(UUID.randomUUID(),a.getUUID(),new (cls("combat.api.CombatSource"))(Id.parse("combatnexus:verify_failure"),Optional.empty(),Optional.empty()),cls("combat.healing.api.HealingRequest").Reason.EFFECT,4);};
        try {
            var pools=world.shields().snapshot(a.getUUID());
            var cancelled=service.submit(request());check(String(cancelled.outcome())==="CANCELLED","cancelled heal classified");
            check(cancelled.contributions().size()===3,"cancelled application retains completed calculation facts");
            mode="partial";var partial=service.submit(request());check(String(partial.outcome())==="UNKNOWN"&&partial.overflow()===0,"incomplete write cannot claim overflow");
            mode="throw";check(rejects(function(){service.submit(request());}),"native failure propagates after publishing UNKNOWN");
            check(world.shields().snapshot(a.getUUID()).equals(pools),"cancelled partial and unknown healing grant no shield");
            check(saved.result().contributions().size()===3,"UNKNOWN application retains completed calculation facts");
            check(saved.childAuthority().isEmpty(),"parent authority expires after callback");
            check(rejects(function(){cls("combat.spi.CombatBehaviorAccess").healing(world,saved);}),"retained callback cannot replay reactions");
        } finally {service.close();}


    } finally {for(var i=0;i<entities.length;i++){cls("combat.spi.CombatWorldLifecycleAccess").clear(world,entities[i].getUUID(),ES.Removal.WORLD_EXIT);entities[i].discard();}}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
