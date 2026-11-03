(function(){
 function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
 var level=AutoJS.server().overworld(),UUID=Java.loadClass("java.util.UUID"),D=Java.loadClass("net.minecraft.world.Difficulty"),difficulty=level.getDifficulty();
 var p=new (Java.loadClass("net.neoforged.neoforge.common.util.FakePlayer"))(level,new (Java.loadClass("com.mojang.authlib.GameProfile"))(UUID.randomUUID(),"StarvationClock"));
 p.connection.markClientLoaded();p.setPos(0,512,0);p.setNoGravity(true);p.setInvulnerable(false);level.addNewPlayer(p);p.setHealth(20);
 var env=cls("effect.internal.EnvironmentEffects"),System=Java.loadClass("java.lang.System"),key="combatnexus.starvation.cadence",bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS;
 var checks=[],attempts=[],start=Number(level.getGameTime()),listener=null;
 function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
 function states(){return cls("combat.api.CombatWorldSessions").managedEffects(level,p.getUUID());}
 function starvation(){for(var state of states())if(String(state.snapshot().effect())==="combatnexus:starvation")return state;return null;}
 var damage=new (Java.loadClass("java.util.function.Consumer"))({accept:function(e){if(e.getEntity()!==p||String(e.getSource().typeHolder().unwrapKey().orElseThrow().identifier())!=="minecraft:starve")return;
  var time=Number(level.getGameTime())-start;attempts.push(time);if(time===160)e.setCanceled(true);
 }});
 function cleanup(){if(listener!==null)bus.unregister(listener);bus.unregister(damage);p.discard();AutoJS.server().setDifficulty(difficulty,true);}
 try{
  AutoJS.server().setDifficulty(D.NORMAL,true);p.getFoodData().setFoodLevel(0);p.getFoodData().setSaturation(0);env.sample(p);
  var state=starvation(),behavior=state.behavior().components().get(0),params=behavior.parameters();
  check(state.snapshot().expiresAtTick().isEmpty()&&behavior.intervalTicks()===80&&behavior.initialDelayTicks()===80,"long-lived hunger owns eighty-tick period and initial delay");
  check(params.amount()===1&&params.minimumHealth()===1,"normal difficulty adopts one damage and one health floor");
  var codec=cls("combat.internal.registration.BehaviorTypes"),decoded=codec.decode(codec.encode(behavior));
  check(decoded.intervalTicks()===80&&decoded.initialDelayTicks()===80&&decoded.parameters().amount()===1,"period and delay round-trip through standard behavior codec");
  var Definition=cls("effect.api.EffectDefinition"),Lifetime=cls("effect.api.EffectLifetime"),block=cls("effect.api.EffectTooltip").nativeInstance(state.snapshot().effect(),new Definition(0,Lifetime.permanent(),state.stacking(),state.policy(),state.attributes(),state.behavior(),state.snapshot().restrictions(),state.display()));
  check(block.summary().toString().indexOf("periodic_base_damage")>=0&&block.details().toString().indexOf("first_delay")>=0&&block.details().toString().indexOf("minimum_health")>=0,"standard text includes adopted damage period delay and health floor");
  bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").LOWEST,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent"),damage);
  for(var i=0;i<80;i++)p.getFoodData().tick(p);
  check(p.getHealth()===20&&attempts.length===0,"actual FoodData starvation attempt is intercepted before damage event");
  System.setProperty(key,JSON.stringify({running:true}));
  listener=new (Java.loadClass("java.util.function.Consumer"))({accept:function(event){if(event.getLevel()!==level)return;
   try{var elapsed=Number(level.getGameTime())-start;
    p.getFoodData().tick(p);env.sample(p);
    if(elapsed===79)check(p.getHealth()===20&&attempts.length===0,"no damage before first scheduled period");
    if(elapsed===80)check(p.getHealth()===19&&attempts.join(",")==="80","one scheduled pulse replaces native starvation");
    if(elapsed===160)check(p.getHealth()===19&&attempts.join(",")==="80,160","cancelled periodic pulse consumes cadence without immediate retry");
    if(elapsed===170){p.getFoodData().setFoodLevel(1);env.sample(p);check(starvation()===null,"eating immediately releases long-lived hunger on sampling");}
    if(elapsed===200){p.getFoodData().setFoodLevel(0);env.sample(p);check(starvation()!==null,"new starvation rebinds its own clock");}
    if(elapsed===279)check(p.getHealth()===19&&attempts.length===2,"reentry waits a complete period");
    if(elapsed>=280){check(p.getHealth()===18&&attempts.join(",")==="80,160,280","reentry has one pulse with no old clock replay");
     bus.unregister(damage);
     for(var row of [[D.EASY,10],[D.NORMAL,1]]){AutoJS.server().setDifficulty(row[0],true);env.sample(p);p.setHealth(row[1]+0.5);env.scheduled(p,starvation().snapshot().effect(),starvation().behavior().components().get(0).parameters());check(p.getHealth()===row[1],"managed periodic damage respects difficulty floor: "+row[0]);}
     AutoJS.server().setDifficulty(D.PEACEFUL,true);env.sample(p);p.setHealth(20);env.scheduled(p,starvation().snapshot().effect(),starvation().behavior().components().get(0).parameters());check(p.getHealth()===20&&starvation()!==null,"peaceful keeps hunger state without damage");
     System.setProperty(key,JSON.stringify({passed:checks.length,checks:checks,attempts:attempts}));cleanup();}
   }catch(error){System.setProperty(key,JSON.stringify({error:String(error),checks:checks,attempts:attempts,health:p.getHealth()}));cleanup();}
  }});
  bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").LOWEST,false,Java.loadClass("net.neoforged.neoforge.event.tick.LevelTickEvent$Post"),listener);
  return JSON.stringify({installed:true,key:key});
 }catch(error){cleanup();throw error;}
})()
