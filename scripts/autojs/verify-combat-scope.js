// Combat ownership boundaries on real native entities; run by effect-foundation.
(function(){
    function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
    var level=AutoJS.server().overworld(),UUID=Java.loadClass("java.util.UUID"),E=Java.loadClass("net.minecraft.world.effect.MobEffects"),I=Java.loadClass("net.minecraft.world.effect.MobEffectInstance");
    var p=new (Java.loadClass("net.neoforged.neoforge.common.util.FakePlayer"))(level,new (Java.loadClass("com.mojang.authlib.GameProfile"))(UUID.randomUUID(),"ScopeProbe"));
    p.connection.markClientLoaded();p.setPos(0,512,0);p.setNoGravity(true);level.addNewPlayer(p);
    var api=cls("combat.api.CombatAccess").find(level).orElseThrow(),checks=[],bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS,listener=null;
    function check(ok,name){if(!ok)throw new Error(name);checks.push(name);}
    try{
        var Attr=Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes"),base=p.getAttributeValue(Attr.ATTACK_SPEED);
        for(var row of [[E.HASTE,1.2],[E.MINING_FATIGUE,0.8]]){
            p.addEffect(new I(row[0],100,1));check(Math.abs(p.getAttributeValue(Attr.ATTACK_SPEED)-base*row[1])<0.00001,"level II attack speed once: "+row[0]);
            check(cls("combat.internal.platform.PlatformWorldLifecycle").existing(level).effects().snapshot(p.getUUID()).size()===1,"attack speed effect remains managed");p.removeAllEffects();
            check(Math.abs(p.getAttributeValue(Attr.ATTACK_SPEED)-base)<0.00001,"attack speed restored after removal");
        }
        for(var effect of [E.NIGHT_VISION,E.HUNGER,E.SATURATION,E.LUCK,E.UNLUCK,E.HERO_OF_THE_VILLAGE,E.BAD_OMEN,E.RAID_OMEN,E.TRIAL_OMEN,E.CONDUIT_POWER,E.WATER_BREATHING,E.DOLPHINS_GRACE,E.NAUSEA]){
            check(!cls("effect.platform.NativeEffectBridge").managed(effect),"native owner: "+effect);
        }
        p.addEffect(new I(E.NIGHT_VISION,100,0));
        var native=p.getEffect(E.NIGHT_VISION);native.update(new I(E.NIGHT_VISION,120,1));
        check(p.getEffect(E.NIGHT_VISION).getAmplifier()===1,"native getEffect returns live instance");
        var map=p.getActiveEffectsMap(),view=p.getActiveEffects();
        check(map.get(E.NIGHT_VISION).equals(native)&&view.contains(native),"native map and collection expose original instance");
        p.baseTick();check(native.getDuration()===119&&cls("combat.internal.platform.PlatformWorldLifecycle").existing(level).effects().snapshot(p.getUUID()).isEmpty(),"native timer advances without managed state");
        var denied=0;
        listener=new (Java.loadClass("java.util.function.Consumer"))({accept:function(event){if(event.getEntity()!==p)return;denied++;event.setCanceled(true);}});
        bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").NORMAL,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.MobEffectEvent$Remove"),listener);
        p.addEffect(new I(E.SPEED,100,0));p.removeAllEffects();
        check(denied===2&&p.hasEffect(E.SPEED)&&p.hasEffect(E.NIGHT_VISION),"mixed clear respects each owner cancellation once");
        bus.unregister(listener);listener=null;p.removeAllEffects();
        check(!p.hasEffect(E.SPEED)&&!p.hasEffect(E.NIGHT_VISION)&&cls("combat.internal.platform.PlatformWorldLifecycle").existing(level).effects().snapshot(p.getUUID()).isEmpty(),"mixed clear removes both owners without resurrection");
        p.addEffect(new I(E.NIGHT_VISION,100,0));map.remove(E.NIGHT_VISION);check(!p.hasEffect(E.NIGHT_VISION),"native map mutation remains native");
        var env=cls("effect.internal.EnvironmentEffects"),D=Java.loadClass("net.minecraft.world.Difficulty"),difficulty=level.getDifficulty();
        try {
            for(var d of [D.PEACEFUL,D.EASY,D.NORMAL,D.HARD]){
                AutoJS.server().setDifficulty(d,true);p.getFoodData().setFoodLevel(0);env.sample(p);
                var states=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level).effects().snapshot(p.getUUID()),starved=null;
                for(var state of states)if(String(state.snapshot().effect())==="combatnexus:starvation")starved=state;
                check(starved!==null&&starved.snapshot().expiresAtTick().isEmpty(),"zero food binds long-lived starvation: "+d);
                p.removeAllEffects();check(p.hasEffect(Java.loadClass("net.minecraft.core.registries.BuiltInRegistries").MOB_EFFECT.get(Java.loadClass("net.minecraft.resources.Identifier").parse("combatnexus:starvation")).orElseThrow()),"ordinary dispel cannot end hunger state: "+d);
                p.getFoodData().setFoodLevel(1);env.sample(p);
                check(cls("combat.internal.platform.PlatformWorldLifecycle").existing(level).effects().snapshot(p.getUUID()).isEmpty(),"food recovery ends starvation: "+d);
            }
        }finally{AutoJS.server().setDifficulty(difficulty,true);p.getFoodData().setFoodLevel(20);env.sample(p);}
        var Set=Java.loadClass("java.util.Set"),List=Java.loadClass("java.util.List"),OL=Java.loadClass("java.util.OptionalLong"),Id=Java.loadClass("net.minecraft.resources.Identifier"),Action=cls("effect.api.EffectSnapshot$RestrictedAction");
        var id=Id.parse("combatnexus:scope_control"),effect=new (cls("effect.api.EffectInstance"))(new (cls("effect.api.EffectSnapshot"))(p.getUUID(),id,id,0,OL.of(level.getGameTime()+200),Set.of(Action.EXTERNAL_MOVEMENT,Action.WALK)),List.of(),cls("effect.api.EffectInstance").Ownership.MANAGED);
        cls("combat.internal.platform.PlatformWorldLifecycle").existing(level).effects().apply(effect,cls("effect.api.EffectService").Stacking.REPLACE);
        p.setDeltaMovement(0,0,0);p.push(1,0,0);check(p.getDeltaMovement().x()>0,"ordinary push is native under combat control");
        p.setDeltaMovement(0,0,0);p.knockback(1,1,0);check(p.getDeltaMovement().length()===0,"combat knockback restriction retained");
        var x=p.getX();p.move(Java.loadClass("net.minecraft.world.entity.MoverType").PISTON,new (Java.loadClass("net.minecraft.world.phys.Vec3"))(0.25,0,0));check(p.getX()>x,"piston movement is native under combat control");
        check(p.isPushedByFluid(),"fluid movement is native under combat control");
        var Hand=Java.loadClass("net.minecraft.world.InteractionHand"),Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Items=Java.loadClass("net.minecraft.world.item.Items");
        p.setItemInHand(Hand.MAIN_HAND,new Stack(Items.APPLE));p.getFoodData().setFoodLevel(1);p.startUsingItem(Hand.MAIN_HAND);check(p.isUsingItem(),"ordinary item use retained under combat control");
        var obsolete=false;for(var action of Action.values())if(String(action)==="USE_ITEM")obsolete=true;check(!obsolete,"removed global use-item restriction is not advertised");
        return JSON.stringify({passed:checks.length,checks:checks});
    }finally{if(listener!==null)bus.unregister(listener);p.discard();}
})()
