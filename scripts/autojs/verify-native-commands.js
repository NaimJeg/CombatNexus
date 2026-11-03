(function(){
    function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
    var level=AutoJS.server().overworld(),api=cls("combat.api.CombatAccess").find(level).orElseThrow();
    var UUID=Java.loadClass("java.util.UUID"),Id=Java.loadClass("net.minecraft.resources.Identifier"),Optional=Java.loadClass("java.util.Optional");
    var source=new (cls("combat.api.CombatSource"))(Id.parse("combatnexus:native_probe"),Optional.empty(),Optional.empty());
    var checks=[],entities=[];
    function check(v,s){if(!v)throw new Error(s);checks.push(s);}
    function cow(){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);e.setPos(0,512,0);e.setNoAi(true);e.setNoGravity(true);level.addFreshEntity(e);entities.push(e);return e;}
    var Effect=cls("effect.api.NativeEffectCommand"),Health=cls("combat.health.api.HealthCorrection"),Lethal=cls("combat.health.api.LethalAttempt");
    function effect(target,op,duration){return api.effects().nativeEffect(new Effect(UUID.randomUUID(),target.getUUID(),source,Id.parse("minecraft:speed"),op,duration,0,false,true,true));}
    function execute(target,type){return api.health().execute(new Lethal(UUID.randomUUID(),target.getUUID(),source,Id.parse(type),Optional.empty(),false));}
    try{
        var e=cow();
        var definitions=api.definitions(),Kind=cls("combat.api.DefinitionDescription$Kind"),definitionId=definitions.shields().iterator().next();
        var described=api.definition(definitionId,Kind.SHIELD).orElseThrow();
        check(described.generation()===definitions.generation()&&described.parameters().containsKey("capacity"),"adopted definitions expose named read-only parameters");
        var installed=api.install(new (cls("combat.api.source.DefinitionInstallation"))(UUID.randomUUID(),e.getUUID(),new (cls("combat.api.source.SourceInstance"))(UUID.randomUUID(),Id.parse("combatnexus_sdk_example:definition_probe"),Optional.empty(),"test"),definitionId,Kind.SHIELD,false,100));
        check(installed.applied()&&api.shields().snapshot(e.getUUID()).size()===1,"definition ID installs adopted shield without internal definition access");
        check(api.sources().revoke(UUID.randomUUID(),installed.value().orElseThrow()).applied()&&api.shields().snapshot(e.getUUID()).isEmpty(),"adopted definition installation revoked by exact handle");
        var applied=effect(e,Effect.Operation.APPLY,100);
        check(applied.applied()&&applied.value().get().after().get().durationTicks()===100,"native effect applied by native owner");
        check(applied.context().get().source().get().id().equals(source.id())&&applied.context().get().identity().isPresent(),"state result carries operation and source identity");
        check(applied.journal().applicationCompleted(),"native effect operation has completed journal");
        var refreshed=effect(e,Effect.Operation.REFRESH,200);
        check(refreshed.applied()&&refreshed.value().get().before().get().durationTicks()===100&&refreshed.value().get().after().get().durationTicks()===200,"native effect refresh retains vanilla merge semantics");
        check(!effect(e,Effect.Operation.REFRESH,50).applied(),"native no-op returns explicit refusal");
        check(effect(e,Effect.Operation.REMOVE,0).applied(),"native effect removed");
        var Absorption=cls("combat.absorption.api.AbsorptionCommand");
        var granted=api.absorption().submit(new Absorption(UUID.randomUUID(),e.getUUID(),source,Absorption.Operation.GRANT,8,10));
        check(granted.applied()&&e.getAbsorptionAmount()===8&&api.shields().snapshot(e.getUUID()).isEmpty(),"native absorption is distinct from shield pools");
        api.absorption().submit(new Absorption(UUID.randomUUID(),e.getUUID(),source,Absorption.Operation.GRANT,8,10));
        check(e.getAbsorptionAmount()===10,"absorption grant respects cap");
        e.setAbsorptionAmount(6);
        check(api.absorption().snapshot(e.getUUID()).get(0).balance()===6&&api.absorption().snapshot(e.getUUID()).get(0).consumed()===4,"native absorption consumption attributed to source");
        api.absorption().submit(new Absorption(UUID.randomUUID(),e.getUUID(),source,Absorption.Operation.RESIZE,0,3));
        check(e.getAbsorptionAmount()===3,"capacity reduction removes only owned excess");
        api.absorption().submit(new Absorption(UUID.randomUUID(),e.getUUID(),source,Absorption.Operation.REMOVE,0,0));
        check(e.getAbsorptionAmount()===0,"source absorption can be revoked");
        e.setHealth(2);
        var StateAddress=cls("combat.state.api.StateAddress"),Example=Java.loadClass("example.combatnexus.PlanExample");
        var retained=new StateAddress(e.getUUID(),Example.RETAINED_STATE,Optional.empty()),cleared=new StateAddress(e.getUUID(),Example.DEATH_STATE,Optional.empty());
        check(api.states().write(UUID.randomUUID(),retained,source,Java.loadClass("java.lang.Integer").valueOf(7)).applied(),"typed runtime state write admitted");
        api.states().write(UUID.randomUUID(),cleared,source,Java.loadClass("java.lang.Integer").valueOf(9));
        check(api.states().read(retained).orElseThrow().intValue()===7,"typed runtime state query preserves value");
        var Fire=cls("combat.action.api.EntityCommand$Ignite"),Move=cls("combat.action.api.EntityCommand$Displace"),Vec=Java.loadClass("net.minecraft.world.phys.Vec3");
        check(api.entities().submit(new Fire(UUID.randomUUID(),e.getUUID(),source,80)).applied()&&e.getRemainingFireTicks()===80,"typed ignition writes native fire timer");
        e.clearFire();var startX=e.getX();
        var moved=api.entities().submit(new Move(UUID.randomUUID(),e.getUUID(),source,new Vec(1,0,0),Move.Mode.POSITION));
        check(moved.applied()&&e.getX()===startX+1&&moved.journal().entries().size()===3,"typed displacement records native position components");
        var correction=new Health(UUID.randomUUID(),e.getUUID(),source,8,Health.Reason.MAXIMUM_CHANGED);
        var result=api.health().correct(correction);
        check(result.applied()&&e.getHealth()===8&&result.journal().entries().get(0).confirmedAmount()===6,"health correction has exact write evidence");
        check(String(api.health().correct(correction).rejection().get().code())==="DUPLICATE","health correction cannot replay");
        var killed=execute(e,"minecraft:generic");
        check(killed.applied()&&String(killed.value().get().outcome())==="DEATH_CONFIRMED","execution confirms native death without ordinary damage");
        check(api.states().read(retained).isPresent()&&api.states().read(cleared).isEmpty(),"state death policy selectively retains and clears");
        var protectedCow=cow();
        protectedCow.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").OFFHAND,new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").TOTEM_OF_UNDYING));
        var protectedResult=execute(protectedCow,"minecraft:generic");
        check(String(protectedResult.value().get().outcome())==="NATIVE_PROTECTED"&&protectedCow.getHealth()===1,"native totem protects execution");
        check(protectedCow.getOffhandItem().isEmpty()&&protectedCow.hasEffect(Java.loadClass("net.minecraft.world.effect.MobEffects").REGENERATION),"native totem consumes item and applies effects once");
        check(protectedResult.journal().entries().stream().anyMatch(function(e){return String(e.resource())==="PROTECTION"&&e.source().isPresent()&&String(e.source().get())==="minecraft:totem_of_undying"&&e.confirmedAmount()===1;}),"native protection journal attributes exact consumed item");
        check(String(execute(protectedCow,"minecraft:generic").value().get().outcome())==="DEATH_CONFIRMED","consumed totem cannot protect twice");
        var bypass=cow();bypass.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").OFFHAND,new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").TOTEM_OF_UNDYING));
        check(String(execute(bypass,"minecraft:out_of_world").value().get().outcome())==="DEATH_CONFIRMED"&&!bypass.getOffhandItem().isEmpty(),"native bypass tag preserves unconsumed totem");
        var dragon=Java.loadClass("net.minecraft.world.entity.EntityType").ENDER_DRAGON.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        var crystal=Java.loadClass("net.minecraft.world.entity.EntityType").END_CRYSTAL.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        dragon.setPos(10,512,0);crystal.setPos(11,512,0);dragon.setNoAi(true);level.addFreshEntity(dragon);level.addFreshEntity(crystal);entities.push(dragon);entities.push(crystal);
        dragon.setHealth(100);dragon.nearestCrystal=crystal;dragon.tickCount=10;dragon.aiStep();
        check(dragon.getHealth()===101,"native crystal boundary performs real healing once");
        var healLog=Java.loadClass("example.combatnexus.SdkExample").healingLog(),crystalSeen=false;
        for(var i=0;i<healLog.size();i++){var healing=healLog.get(i).result();if(healing.request().target().equals(dragon.getUUID())&&String(healing.request().reason())==="CRYSTAL"&&healing.request().source().owner().orElseThrow().equals(crystal.getUUID()))crystalSeen=true;}
        check(crystalSeen,"crystal recovery carries frozen crystal source identity");
    } finally {for(var i=0;i<entities.length;i++)entities[i].discard();}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
