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

        var Limit=cls("combat.api.action.TriggerLimit"), Ledger=cls("combat.api.action.TriggerLedger"), Action=cls("combat.api.action.CombatAction");
        var ledger=new Ledger(4),target=UUID.randomUUID(),actor=UUID.randomUUID(),session=UUID.randomUUID();
        var action=new Action(UUID.randomUUID(),session,actor),limit=new Limit(Limit.Scope.ACTION,1);
        check(String(ledger.claim(0,1,"probe",limit,1,1,target,Optional.empty()).get())==="missing_action_evidence","missing action evidence rejected");
        check(ledger.claim(0,1,"probe",limit,1,1,target,Optional.of(action)).isEmpty(),"first real action trigger admitted");
        check(ledger.claim(0,1,"probe",limit,2,2,UUID.randomUUID(),Optional.of(action)).isPresent(),"same action across targets counts once");
        check(ledger.claim(0,1,"probe",limit,3,3,target,Optional.of(new Action(UUID.randomUUID(),session,actor))).isEmpty(),"same tick separate actions independent");
        var sourceLimit=new Limit(Limit.Scope.SOURCE,1);
        check(ledger.claim(0,2,"source",sourceLimit,1,1,target,Optional.empty()).isEmpty(),"source lifetime trigger admitted");
        check(ledger.claim(1,2,"source",sourceLimit,2,2,target,Optional.empty()).isPresent(),"source limit survives tick change");
        ledger.remove(2);check(ledger.claim(1,2,"source",sourceLimit,2,2,target,Optional.empty()).isEmpty(),"source cleanup removes counter");
        var v=cow(),a=cow();
        install(v,"action_only",new BP(0,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$Reflection"))(0.2,limit))));
        hit(v,a,1);check(world.reactions().facts().stream().anyMatch(function(f){return String(f.outcome())==="missing_action_evidence";}),"plain damage request cannot fabricate action evidence");
        var fake=new (Java.loadClass("net.neoforged.neoforge.common.util.FakePlayer"))(level,new (Java.loadClass("com.mojang.authlib.GameProfile"))(UUID.randomUUID(),"CNActionVerifier"));
        fake.setInvulnerable(false);fake.connection.markClientLoaded();fake.setPos(1,100,0);level.addNewPlayer(fake);entities.push(fake);
        fake.getAttribute(Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").ATTACK_DAMAGE).setBaseValue(3);
        var triggered=function(){return world.reactions().facts().stream().filter(function(f){return f.target().equals(v.getUUID())&&String(f.reaction())==="combatnexus:reflection"&&String(f.outcome())==="child_applied";}).count();};
        var count=triggered();v.invulnerableTime=0;v.setHealth(v.getMaxHealth());fake.attack(v);
        check(triggered()===count+1,"native player attack supplies action identity");
        v.invulnerableTime=0;v.setHealth(v.getMaxHealth());fake.invulnerableTime=0;fake.attack(v);
        check(triggered()===count+2,"two native actions in same tick have separate quotas");
    } finally {for(var i=0;i<entities.length;i++){cls("combat.spi.CombatWorldLifecycleAccess").clear(world,entities[i].getUUID(),ES.Removal.WORLD_EXIT);entities[i].discard();}}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
