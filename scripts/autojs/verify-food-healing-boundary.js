(function(){
 function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
 var level=AutoJS.server().overworld(),UUID=Java.loadClass("java.util.UUID"),p=new (Java.loadClass("net.neoforged.neoforge.common.util.FakePlayer"))(level,new (Java.loadClass("com.mojang.authlib.GameProfile"))(UUID.randomUUID(),"FoodBoundary"));
 p.connection.markClientLoaded();p.setPos(0,512,0);level.addNewPlayer(p);
 var Food=Java.loadClass("net.minecraft.world.food.FoodData"),attr=p.getAttribute(cls("attribute.api.CombatAttributes").healingReceived()),calls=0,cancel=false,checks=[];
 var bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS;
 var listener=new (Java.loadClass("java.util.function.Consumer"))({accept:function(e){if(e.getEntity()!==p)return;calls++;if(cancel)e.setCanceled(true);}});
 bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").NORMAL,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingHealEvent"),listener);
 function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
 function exhaustion(food){var out=Java.loadClass("net.minecraft.world.level.storage.TagValueOutput").createWithoutContext(Java.loadClass("net.minecraft.util.ProblemReporter").DISCARDING);food.addAdditionalSaveData(out);return Number(out.buildResult().getFloat("foodExhaustionLevel").orElseThrow());}
 try {
  attr.setBaseValue(2);p.setHealth(2);var food=new Food();
  for(var i=0;i<9;i++)food.tick(p);
  check(p.getHealth()===2&&calls===0,"saturation healing waits for native tenth tick");food.tick(p);
  check(Math.abs(p.getHealth()-(2+5/3))<0.0001&&calls===1,"native saturation heal enters healing multiplier once");
  check(food.getFoodLevel()===20&&food.getSaturationLevel()===5&&exhaustion(food)===5,"native healing keeps original food and exhaustion bookkeeping");food.tick(p);
  check(food.getFoodLevel()===20&&food.getSaturationLevel()===4&&exhaustion(food)===1,"native exhaustion consumes saturation on next tick");
  p.setHealth(2);calls=0;var slow=new Food();slow.setFoodLevel(18);slow.setSaturation(0);
  for(var i=0;i<79;i++)slow.tick(p);check(p.getHealth()===2&&calls===0,"unsaturated healing waits native eighty ticks");slow.tick(p);
  check(p.getHealth()===4&&calls===1&&exhaustion(slow)===6,"unsaturated native healing retains amount and exhaustion");
  p.setHealth(2);calls=0;cancel=true;var refused=new Food();for(var i=0;i<10;i++)refused.tick(p);
  check(p.getHealth()===2&&calls===1&&exhaustion(refused)===5,"cancelled healing retains native exhaustion without healing replay");cancel=false;
  p.setHealth(p.getMaxHealth());calls=0;var full=new Food();for(var i=0;i<80;i++)full.tick(p);
  check(calls===0&&exhaustion(full)===0&&full.getSaturationLevel()===5,"full health does not invent regeneration or food consumption");
  return JSON.stringify({passed:checks.length,checks:checks});
 }finally{bus.unregister(listener);p.discard();}
})()
