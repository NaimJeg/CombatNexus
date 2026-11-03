(function(){
    function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
    var level=AutoJS.server().overworld(),env=cls("effect.internal.EnvironmentEffects"),System=Java.loadClass("java.lang.System"),key="combatnexus.environment.cadence";
    var Pos=Java.loadClass("net.minecraft.core.BlockPos"),Blocks=Java.loadClass("net.minecraft.world.level.block.Blocks"),pos=new Pos(0,300,0),old=level.getBlockState(pos);
    var cow=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    cow.setPos(0.5,300,0.5);cow.setNoGravity(true);cow.setNoAi(true);level.addFreshEntity(cow);
    cow.getAttribute(Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").MAX_HEALTH).setBaseValue(100);cow.setHealth(100);
    var checks=[],attempts=[],start=Number(level.getGameTime()),bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS;
    function check(v,l){if(!v)throw new Error(l);checks.push(l);}
    var damageListener=new (Java.loadClass("java.util.function.Consumer"))({accept:function(event){
        if(event.getEntity()!==cow||String(event.getSource().typeHolder().unwrapKey().orElseThrow().identifier())!=="minecraft:cactus")return;
        var elapsed=Number(level.getGameTime())-start;attempts.push(elapsed);if(elapsed===10)event.setCanceled(true);
    }});
    bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").LOWEST,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent"),damageListener);
    var listener=null;
    function cleanup(){if(listener!==null)bus.unregister(listener);bus.unregister(damageListener);cow.discard();level.setBlock(pos,old,2);}
    try{
        cow.hurtServer(level,cow.damageSources().generic(),2);check(cow.getHealth()===98,"ordinary hit establishes shared hurt cooldown");
        level.setBlock(pos,Blocks.CACTUS.defaultBlockState(),2);env.sample(cow);
        check(cow.getHealth()===97&&cow.invulnerableTime===20,"scheduled first pulse bypasses shared cooldown without overwriting it");
        cow.hurtServer(level,cow.damageSources().generic(),1);check(cow.getHealth()===97,"ordinary smaller hit remains blocked by original cooldown");
        cow.hurtServer(level,cow.damageSources().generic(),3);check(cow.getHealth()===96,"scheduled damage preserves native lastHurt difference semantics");
        for(var i=0;i<20;i++){env.sample(cow);cow.hurtServer(level,cow.damageSources().cactus(),1);}
        check(cow.getHealth()===96&&attempts.length===1,"duplicate facts and native attempts cannot duplicate the scheduled pulse");
        var state=cls("combat.api.CombatWorldSessions").managedEffects(level,cow.getUUID()).get(0);
        var encoded=cls("combat.internal.registration.BehaviorTypes").encode(state.behavior().components().get(0));
        var restored=cls("combat.internal.registration.BehaviorTypes").decode(encoded);
        check(restored.intervalTicks()===10&&String(restored.phase())==="PRESERVE","adopted scheduled cadence survives behavior codec");
        var count=0;for(var definition of env.definitions())if(definition.scheduled()&&String(definition.id())!=="combatnexus:starvation"){check(definition.intervalTicks()===10,"scheduled environment definition: "+definition.id());count++;}
        check(count===6,"six continuous contact states use explicit scheduling");
        System.setProperty(key,JSON.stringify({running:true}));
        listener=new (Java.loadClass("java.util.function.Consumer"))({accept:function(event){if(event.getLevel()!==level)return;
            try{
                var elapsed=Number(level.getGameTime())-start;
                cow.invulnerableTime=100;env.sample(cow);
                if(elapsed===11)check(cow.getHealth()===96&&attempts.join(",")==="0,10","cancelled pulse consumes interval without immediate retry");
                if(elapsed===20)check(cow.getHealth()===95&&attempts.join(",")==="0,10,20","next scheduled pulse runs at tick twenty despite active shared cooldown");
                if(elapsed===21){level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);env.sample(cow);}
                if(elapsed===30)check(cow.getHealth()===95&&attempts.length===3,"condition exit cancels upcoming pulse");
                if(elapsed===31){level.setBlock(pos,Blocks.CACTUS.defaultBlockState(),2);env.sample(cow);check(cow.getHealth()===94,"reentry installs a new cadence with one immediate pulse");}
                if(elapsed===35)cow.setInvulnerable(true);
                if(elapsed===42){check(cow.getHealth()===94&&attempts.length===4,"actual invulnerability still rejects scheduled damage");cow.setInvulnerable(false);}
                if(elapsed===43)check(cow.getHealth()===94,"immunity removal does not replay rejected pulse");
                if(elapsed>=52){check(cow.getHealth()===93&&attempts.join(",")==="0,10,20,31,51","scheduled damage resumes on next phase after immunity ends");System.setProperty(key,JSON.stringify({passed:checks.length,checks:checks,attempts:attempts}));cleanup();}
            }catch(error){System.setProperty(key,JSON.stringify({error:String(error),checks:checks,attempts:attempts,health:cow.getHealth()}));cleanup();}
        }});
        bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").LOWEST,false,Java.loadClass("net.neoforged.neoforge.event.tick.LevelTickEvent$Post"),listener);
        return JSON.stringify({installed:true,key:key});
    }catch(error){cleanup();throw error;}
})()
