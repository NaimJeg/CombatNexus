(function(){
    function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
    var level=AutoJS.server().overworld(),System=Java.loadClass("java.lang.System"),key="combatnexus.sources.natural";
    var Pos=Java.loadClass("net.minecraft.core.BlockPos"),Blocks=Java.loadClass("net.minecraft.world.level.block.Blocks"),Effect=Java.loadClass("net.minecraft.world.effect.MobEffects");
    var checks=[],saved=[],start=level.getGameTime(),bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS;
    function check(v,label){if(!v)throw new Error(label);checks.push(label);}
    function block(pos,state){saved.push([pos,level.getBlockState(pos)]);level.setBlock(pos,state,3);}
    var beaconPos=new Pos(4,300,4),conduitPos=new Pos(10,300,10);
    for(var x=-1;x<=1;x++)for(var z=-1;z<=1;z++)block(beaconPos.offset(x,-1,z),Blocks.IRON_BLOCK.defaultBlockState());
    block(beaconPos,Blocks.BEACON.defaultBlockState());
    var beacon=level.getBlockEntity(beaconPos),tag=new (Java.loadClass("net.minecraft.nbt.CompoundTag"))();tag.putString("primary_effect","minecraft:resistance");
    beacon.loadCustomOnly(Java.loadClass("net.minecraft.world.level.storage.TagValueInput")["create(net.minecraft.util.ProblemReporter,net.minecraft.core.HolderLookup$Provider,net.minecraft.nbt.CompoundTag)"](Java.loadClass("net.minecraft.util.ProblemReporter").DISCARDING,level.registryAccess(),tag));
    for(var x=-2;x<=2;x++)for(var y=-2;y<=2;y++)for(var z=-2;z<=2;z++)block(conduitPos.offset(x,y,z),(Math.abs(x)===2||Math.abs(y)===2||Math.abs(z)===2?Blocks.PRISMARINE:Blocks.WATER).defaultBlockState());
    block(conduitPos,Blocks.CONDUIT.defaultBlockState());
    var conduit=level.getBlockEntity(conduitPos),player=Java.loadClass("net.neoforged.neoforge.common.util.FakePlayerFactory").get(level,new (Java.loadClass("com.mojang.authlib.GameProfile"))(Java.loadClass("java.util.UUID").randomUUID(),"CNBlockSources"));
    player.setPos(10.5,300,10.5);player.setNoGravity(true);level.addNewPlayer(player);
    var api=cls("combat.api.CombatAccess").find(level).orElseThrow();System.setProperty(key,JSON.stringify({running:true}));
    var listener=new (Java.loadClass("java.util.function.Consumer"))({accept:function(event){
        if(event.getLevel()!==level)return;
        try{
            player.setPos(10.5,300,10.5);player.setDeltaMovement(0,0,0);player.baseTick();
            Java.loadClass("net.minecraft.world.level.block.entity.BeaconBlockEntity").tick(level,beaconPos,level.getBlockState(beaconPos),beacon);
            Java.loadClass("net.minecraft.world.level.block.entity.ConduitBlockEntity").serverTick(level,conduitPos,level.getBlockState(conduitPos),conduit);
            if(level.getGameTime()-start>=90){
                check(player.hasEffect(Effect.RESISTANCE),"actual active beacon supplies combat resistance");
                check(player.hasEffect(Effect.CONDUIT_POWER),"actual underwater conduit supplies its effect");
                var states=api.effects().snapshot(player.getUUID()),found=0;
                for(var state of states){var id=String(state.definition());if(id!=="minecraft:resistance")continue;
                    var origin=state.provenance(),pos=id==="minecraft:resistance"?beaconPos:conduitPos;
                    check(origin.entity()===null&&origin.block()!==null&&String(origin.block().dimension())===String(level.dimension().identifier())&&origin.block().x()===pos.getX()&&origin.block().y()===pos.getY()&&origin.block().z()===pos.getZ(),id+" records dimension and actual block position without inventing owner");found++;
                }
                check(found===1,"only combat beacon effect enters managed block provenance");
                check(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(level).effects().snapshot(player.getUUID()).stream().noneMatch(function(s){return String(s.snapshot().effect())==="minecraft:conduit_power";}),"conduit power stays native");
                System.setProperty(key,JSON.stringify({passed:checks.length,checks:checks}));cleanup();
            }
        }catch(error){System.setProperty(key,JSON.stringify({error:String(error),checks:checks}));cleanup();}
    }});
    function cleanup(){bus.unregister(listener);player.discard();for(var i=saved.length-1;i>=0;i--)level.setBlock(saved[i][0],saved[i][1],3);}
    bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").LOWEST,false,Java.loadClass("net.neoforged.neoforge.event.tick.LevelTickEvent$Post"),listener);
    return JSON.stringify({installed:true,key:key});
})()
