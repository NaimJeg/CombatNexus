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

        var Phase=cls("combat.healing.api.HealingPhase"), Builder=cls("combat.api.pipeline.DomainPipeline$Builder");
        var builder=new Builder(Phase), order=[];
        builder.register(Id.parse("probe:z"),Phase.BASE,0,false,"builtin_z");
        builder.register(Id.parse("probe:a"),Phase.BASE,0,false,"builtin_a");
        builder.register(Id.parse("probe:external_z"),Phase.BASE,0,true,"external_first");
        builder.register(Id.parse("probe:external_a"),Phase.BASE,0,true,"external_second");
        check(rejects(function(){builder.register(Id.parse("probe:a"),Phase.BASE,1,false,"duplicate");}),"duplicate ID rejected");
        check(rejects(function(){builder.register(Id.parse("probe:null"),null,1,false,"invalid");}),"invalid phase rejected");
        var handle=builder.register(Id.parse("probe:removed"),Phase.BASE,100,false,"removed");handle.close();
        var pipeline=builder.freeze();pipeline.execute(Phase.BASE,function(e){order.push(String(e.handler()));},function(){return false;});
        check(order.join(",")==="builtin_a,external_second,external_first,builtin_z","stable priority and identifier ordering");
        check(rejects(function(){handle.close();}),"unregister after freeze refused");
        check(rejects(function(){builder.register(Id.parse("probe:late"),Phase.BASE,0,false,"late");}),"late registration refused");
        var v=cow();v.setHealth(2);
        install(v,"healing_add",new BP(0,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$HealingAdditive"))(2))));
        v.getAttribute(cls("attribute.api.CombatAttributes").healingReceived()).setBaseValue(2);
        var result=heal(v,1);check(result.actualRecovery()===6,"additive contribution before received multiplier once");
        check(result.contributions().size()===4,"settlement preserves additive clamp and received facts");
        check(String(result.contributions().get(0).source())==="combatnexus:healing_add"&&result.contributions().get(0).before()===1&&result.contributions().get(0).after()===3,"actual additive contribution attributed to source");
        check(rejects(function(){result.contributions().clear();}),"settlement contribution facts immutable");
        world.effects().remove(v.getUUID(),Id.parse("combatnexus:healing_add"),Id.parse("combatnexus:healing_add"),ES.Removal.REMOVED);
        check(result.contributions().get(0).after()===3,"historical facts survive source removal");
        var mixed=cow();mixed.setHealth(1);
        install(mixed,"negative",new BP(100,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$HealingAdditive"))(-5))));
        install(mixed,"positive",new BP(0,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$HealingAdditive"))(5))));
        var mixedResult=heal(mixed,1);
        check(mixedResult.actualRecovery()===1,"opposing additives sum before the single clamp");
        check(mixedResult.contributions().get(0).after()===-4,"signed intermediate remains calculation evidence only");
        install(mixed,"positive",new BP(200,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$HealingAdditive"))(5))));mixed.setHealth(1);
        check(heal(mixed,1).actualRecovery()===1,"changing source priority does not change this additive bucket result");
        world.effects().remove(mixed.getUUID(),Id.parse("combatnexus:positive"),Id.parse("combatnexus:positive"),ES.Removal.REMOVED);
        check(heal(mixed,1).modifiedAmount()===0,"negative aggregate clamps once to zero");
        var input=world.captureInput(v,cls("combat.internal.definition.CombatDefinitions").forServer(AutoJS.server()).generation());
        var workspace=new (cls("combat.healing.api.HealingCalculation"))(result.request(),new (cls("combat.api.input.ParticipantInputs"))(Java.loadClass("java.util.Map").of(cls("combat.api.input.ParticipantInputs$Role").TARGET,input)));
        check(rejects(function(){workspace.invoke(function(c){c.amount(Id.parse("probe:bad"),8);throw new Error("injected");});}),"compute callback failure propagates");
        check(workspace.amount()===1&&workspace.contributions().isEmpty(),"failed callback restores amount and contribution facts");
        check(rejects(function(){workspace.amount(Id.parse("probe:expired"),2);}),"retained workspace mutation refused");
        check(rejects(function(){workspace.invoke(function(c){c.amount(Id.parse("probe:nan"),NaN);});}),"nonfinite contribution refused");
        check(rejects(function(){workspace.invoke(function(c){c.additive(Id.parse("probe:negative"),-5);});}),"unclamped negative stage cannot escape calculation");
        check(workspace.amount()===1&&workspace.contributions().isEmpty(),"invalid stage rolls back signed evidence");
        check(rejects(function(){workspace.invoke(function(c){for(var n=0;n<257;n++)c.amount(Id.parse("probe:bounded"),1);});}),"contribution capacity rejects before unbounded growth");
        check(workspace.contributions().isEmpty(),"capacity failure restores contribution checkpoint");
    } finally {for(var i=0;i<entities.length;i++){cls("combat.spi.CombatWorldLifecycleAccess").clear(world,entities[i].getUUID(),ES.Removal.WORLD_EXIT);entities[i].discard();}}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
