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

        var v=cow();var e=install(v,"source",new BP(0,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$Reflection"))(0.2,(Java.loadClass("io.github.naimjeg.combatnexus.combat.api.action.TriggerLimit")).ONCE))));
        var captured=world.behaviors().capture(v.getUUID()).get(0);
        check(world.behaviors().valid(captured),"complete installation active");
        check(rejects(function(){world.behaviors().clear(v.getUUID());}),"public source view cannot bypass Effect removal");
        check(rejects(function(){world.behaviors().tick(level.getGameTime()+200);}),"public source view cannot advance world time");
        check(rejects(function(){world.behaviors().close();}),"public source view cannot close the installation ledger");
        check(rejects(function(){world.behaviors().compose(null);}),"public source view cannot install through a substitute port");
        check(world.behaviors().valid(captured)&&world.effects().snapshot(v.getUUID()).size()===1,"rejected mutations preserve both source and Effect ownership");
        cls("combat.spi.CombatWorldLifecycleAccess").clear(world,v.getUUID(),ES.Removal.WORLD_EXIT);
        check(!world.behaviors().valid(captured),"removed captured source invalidated");
        world.effects().apply(e,ES.Stacking.REPLACE);
        check(!world.behaviors().valid(captured),"replacement cannot revive previous installation");
        check(world.behaviors().capture(v.getUUID()).size()===1,"replacement has one source");
        check(rejects(function(){new BP(0,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$Reflection"))(2,(Java.loadClass("io.github.naimjeg.combatnexus.combat.api.action.TriggerLimit")).ONCE)));}),"invalid ratio refused");
        var Sources=cls("combat.internal.coordination.WorldBehaviorSources"),isolated=new Sources(world);
        var Port=cls("effect.spi.EffectContributionPort"),Prepared=cls("effect.spi.EffectContributionPort$Prepared"),Receipt=cls("effect.spi.EffectContributionPort$Receipt");
        var fail=false,port=new Port({prepare:function(change){return new Prepared({apply:function(){return new Receipt(List.of(),!fail,null);},reconcile:function(){return new Receipt(List.of(),!fail,null);}});}});
        try {
            var prepared=isolated.compose(port).prepare(new ES.Change(List.of(),List.of(e),Optional.empty()));
            fail=true;check(!prepared.apply().completed()&&isolated.capture(v.getUUID()).isEmpty(),"partial installation not active");
            check(!prepared.reconcile().completed()&&isolated.capture(v.getUUID()).isEmpty(),"failed compensation retains no new activation");
            fail=false;check(prepared.reconcile().completed(),"compensation retry completes");
            check(prepared.apply().completed()&&isolated.capture(v.getUUID()).size()===1,"completed retry activates source");
            var old=isolated.capture(v.getUUID()).get(0);
            var refresh=isolated.compose(port).prepare(new ES.Change(List.of(e),List.of(e),Optional.empty()));
            fail=true;check(!refresh.apply().completed()&&!isolated.valid(old),"failed refresh blocks previously active behavior");
            check(!refresh.reconcile().completed()&&isolated.capture(v.getUUID()).isEmpty(),"failed compensation keeps affected source blocked");
            fail=false;var other=cow(),otherEffect=effect(other,"unrelated",new BP(0,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$Reflection"))(0.2,(Java.loadClass("io.github.naimjeg.combatnexus.combat.api.action.TriggerLimit")).ONCE))));
            check(isolated.compose(port).prepare(new ES.Change(List.of(),List.of(otherEffect),Optional.empty())).apply().completed(),"unrelated source proceeds during reconciliation");
            check(refresh.reconcile().completed()&&isolated.valid(old),"confirmed compensation restores original installation");
            isolated.clear(v.getUUID());isolated.clear(v.getUUID());check(!isolated.valid(old),"repeated source cleanup idempotent");
        } finally {isolated.close();}
        check(world.behaviorDefinitions().containsKey(Id.parse("combatnexus:periodic_healing")),"sample definitions adopted");
        var definition=world.behaviorDefinitions().get(Id.parse("combatnexus:periodic_healing"));
        check(definition.plan().components().get(0).amount()===1&&definition.durationTicks()===200,"data values match approved sample");
        var equipment=cow(),Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Items=Java.loadClass("net.minecraft.world.item.Items");
        var equipped=new Stack(Items.STICK),slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot").MAINHAND;
        var Bindings=cls("combat.api.behavior.EquipmentBindings"),Binding=cls("combat.api.behavior.EquipmentBindings$Binding");
        equipped.set(Bindings.component(),new Bindings(List.of(new Binding(Id.parse("combatnexus:guard"),Bindings.Kind.BEHAVIOR,Id.parse("combatnexus:last_stand"),List.of(slot)))));
        var Equipment=cls("combat.spi.CombatEquipmentAccess");
        Equipment.changed(world,equipment.getUUID(),slot,equipped);
        var installed=world.behaviors().capture(equipment.getUUID()).get(0);
        Equipment.changed(world,equipment.getUUID(),slot,equipped.copy());
        check(world.behaviors().valid(installed),"equivalent equipment update preserves installation");
        Equipment.changed(world,equipment.getUUID(),slot,Stack.EMPTY);
        check(!world.behaviors().valid(installed),"unequip revokes captured source");
        Equipment.changed(world,equipment.getUUID(),slot,equipped);
        check(!world.behaviors().valid(installed),"reequip creates a new installation identity");
        var malformed=Java.loadClass("com.google.gson.JsonParser").parseString('{}').getAsJsonObject();
        check(rejects(function(){cls("combat.api.behavior.BehaviorDefinition").decode(malformed);}),"missing definition fields refused");


    } finally {for(var i=0;i<entities.length;i++){cls("combat.spi.CombatWorldLifecycleAccess").clear(world,entities[i].getUUID(),ES.Removal.WORLD_EXIT);entities[i].discard();}}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
