(function(){
    function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
    var level=AutoJS.server().overworld(),System=Java.loadClass("java.lang.System"),key="combatnexus.effect.natural";
    var Type=Java.loadClass("net.minecraft.world.entity.EntityType"),Reason=Java.loadClass("net.minecraft.world.entity.EntitySpawnReason");
    var Effects=Java.loadClass("net.minecraft.world.effect.MobEffects"),Instance=Java.loadClass("net.minecraft.world.effect.MobEffectInstance");
    var entities=[],checks=[],start=level.getGameTime(),bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS;
    function spawn(){var e=Type.COW.create(level,Reason.COMMAND);e.setPos(entities.length,512,0);e.setNoAi(true);e.setNoGravity(true);level.addFreshEntity(e);entities.push(e);return e;}
    function check(v,label){if(!v)throw new Error(label);checks.push(label);}
    var Example=Java.loadClass("probe.EffectExtension");
    function changes(entity){
        var result=[],all=Example.moments();
        for(var i=0;i<all.size();i++){var m=all.get(i);if(m instanceof cls("combat.api.action.CombatMoment$Effect")&&m.target().equals(entity.getUUID())&&m.managedChange().isPresent())result.push(m);}
        return result;
    }
    var regen=spawn(),poison=spawn(),layers=spawn(),fire=spawn(),custom=spawn(),cancelled=spawn(),expiryAttempts=0;
    var expiryListener=new (Java.loadClass("java.util.function.Consumer"))({accept:function(event){if(event.getEntity()===cancelled){expiryAttempts++;if(expiryAttempts<=2)event.setCanceled(true);}}});
    bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").NORMAL,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.MobEffectEvent$Expired"),expiryListener);
    cancelled.addEffect(new Instance(Effects.SPEED,5,0));
    regen.setHealth(1);regen.addEffect(new Instance(Effects.REGENERATION,101,0));
    poison.setHealth(2);poison.addEffect(new Instance(Effects.POISON,101,0));
    layers.addEffect(new Instance(Effects.SPEED,100,0));layers.addEffect(new Instance(Effects.SPEED,10,2));
    fire.setRemainingFireTicks(60);
    custom.setHealth(1);
    var List=Java.loadClass("java.util.List"),Set=Java.loadClass("java.util.Set"),Id=Java.loadClass("net.minecraft.resources.Identifier");
    var id=Id.parse("combatnexus_effect_probe:condition"),api=cls("combat.api.CombatAccess").find(level).orElseThrow();
    var def=new (cls("effect.api.EffectDefinition"))(0,cls("effect.api.EffectLifetime").permanent(),cls("effect.api.EffectService$Stacking").REPLACE,
        new (cls("effect.api.EffectPolicy"))(cls("effect.api.EffectPolicy$Category").BENEFICIAL,false),List.of(),
        new (cls("combat.api.behavior.BehaviorPlan"))(0,List.of(new (Java.loadClass("probe.EffectExtension$Pulse"))(1,20))),Set.of());
    api.effects().apply(new (cls("effect.api.EffectApplication"))(Java.loadClass("java.util.UUID").randomUUID(),def.instantiate(custom.getUUID(),id,id,start),cls("effect.api.EffectService$Stacking").REPLACE));
    var initialLayerChanges=changes(layers),initialCancelledCount=changes(cancelled).length;
    var directChanges=changes(custom);
    check(directChanges.length===1&&String(directChanges[0].change())==="ADDED","direct public apply publishes exactly one committed event");
    check(initialLayerChanges.length===2,"native add and upgrade each publish once");
    System.setProperty(key,JSON.stringify({running:true}));
    var listener=new (Java.loadClass("java.util.function.Consumer"))({accept:function(event){
        if(event.getLevel()!==level)return;
        try{
            var elapsed=level.getGameTime()-start;
            if(elapsed===6){
                check(cancelled.hasEffect(Effects.SPEED),"native expiry cancellation retains authoritative state");
                check(changes(cancelled).length===initialCancelledCount,"cancelled expiry publishes no removal event");
                check(changes(layers).length===initialLayerChanges.length,"ordinary duration countdown publishes no change event");
            }
            if(elapsed===11){check(layers.getEffect(Effects.SPEED).getAmplifier()===0,"hidden weak layer restores on natural tick");check(layers.getEffect(Effects.SPEED).getDuration()===89,"hidden duration continues while suppressed");}
            if(elapsed===11){
                var restored=changes(layers);
                check(restored.length===3&&restored[2].before().get().amplifier()===2&&restored[2].after().get().amplifier()===0,"hidden restoration publishes one before/after event");
                check(initialLayerChanges[0].managedChange().get().after().get(0).snapshot().level()===0&&initialLayerChanges[1].managedChange().get().after().get(0).snapshot().level()===2,"retained committed snapshots remain immutable after restoration");
            }
            if(elapsed===21){check(fire.getRemainingFireTicks()===39,"burning decrements exactly once per world tick");check(custom.getHealth()===2,"registered custom periodic callback fires once");}
            if(elapsed>=102){
                check(regen.getHealth()===3,"remaining-duration regeneration triggers at 100 and 50, excludes expiry zero");
                check(poison.getHealth()===1,"natural poison never crosses minimum health");
                check(!regen.hasEffect(Effects.REGENERATION)&&!poison.hasEffect(Effects.POISON)&&!layers.hasEffect(Effects.SPEED),"timed projections expire naturally");
                check(fire.getRemainingFireTicks()<=0,"managed fire expires naturally");
                check(custom.getHealth()===6,"custom permanent effect executes five pulses without dedicated mixin");
                check(expiryAttempts===3&&!cancelled.hasEffect(Effects.SPEED),"cancelled expiry retries lifecycle only and removes once accepted");
                var ended=changes(layers),cancelledChanges=changes(cancelled);
                check(ended.length===4&&String(ended[3].managedChange().get().removal().get())==="EXPIRED","natural expiry publishes exactly one removal with reason");
                check(cancelledChanges.length===initialCancelledCount+1&&String(cancelledChanges[cancelledChanges.length-1].managedChange().get().removal().get())==="EXPIRED","retried expiry publishes only successful commit");
                System.setProperty(key,JSON.stringify({passed:checks.length,checks:checks}));cleanup();
            }
        }catch(error){System.setProperty(key,JSON.stringify({error:String(error),checks:checks,regen:regen.getHealth(),custom:custom.getHealth()}));cleanup();}
    }});
    function cleanup(){bus.unregister(listener);bus.unregister(expiryListener);for(var e of entities)e.discard();}
    bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").LOWEST,false,Java.loadClass("net.neoforged.neoforge.event.tick.LevelTickEvent$Post"),listener);
    return JSON.stringify({installed:true,key:key});
})()
