(function(){
    function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
    var level=AutoJS.server().overworld(),env=cls("effect.internal.EnvironmentEffects"),checks=[],entities=[],saved=[];
    var Type=Java.loadClass("net.minecraft.world.entity.EntityType"),Reason=Java.loadClass("net.minecraft.world.entity.EntitySpawnReason"),Blocks=Java.loadClass("net.minecraft.world.level.block.Blocks"),Pos=Java.loadClass("net.minecraft.core.BlockPos");
    var Effects=Java.loadClass("net.minecraft.world.effect.MobEffects"),Instance=Java.loadClass("net.minecraft.world.effect.MobEffectInstance");
    function check(v,l){if(!v)throw new Error(l);checks.push(l);}
    function spawn(type,x){var e=type.create(level,Reason.COMMAND);e.setPos(x,300,0.5);e.setNoAi(true);e.setNoGravity(true);e.setInvulnerable(true);level.addFreshEntity(e);entities.push(e);return e;}
    function has(e,name){return cls("combat.api.CombatWorldSessions").managedEffects(level,e.getUUID()).stream().anyMatch(function(s){return String(s.snapshot().effect())==="combatnexus:"+name;});}
    function fluid(block){for(var s of saved)level.setBlock(s[0],block.defaultBlockState(),2);}
    for(var x=0;x<3;x++)for(var z=-1;z<2;z++)for(var y=299;y<304;y++){var p=new Pos(x,y,z);saved.push([p,level.getBlockState(p)]);}
    try{
        var cow=spawn(Type.COW,0.5);fluid(Blocks.WATER);cow.setAirSupply(-1);cow.baseTick();cow.baseTick();cow.setAirSupply(-1);env.sample(cow);
        check(has(cow,"drowning"),"depleted oxygen under actual water activates drowning");
        cow.addEffect(new Instance(Effects.WATER_BREATHING,100,0));env.sample(cow);check(!has(cow,"drowning"),"water breathing removes drowning binding");cow.removeAllEffects();
        fluid(Blocks.AIR);cow.baseTick();cow.baseTick();cow.setAirSupply(300);env.sample(cow);check(!has(cow,"drowning"),"air and oxygen recovery remove drowning");
        fluid(Blocks.LAVA);cow.baseTick();env.sample(cow);check(has(cow,"lava_contact"),"actual lava fluid activates lava contact");
        fluid(Blocks.AIR);cow.baseTick();env.sample(cow);check(!has(cow,"lava_contact"),"leaving lava removes contact independently of burning");cow.clearFire();
        var dolphin=spawn(Type.DOLPHIN,0.5);env.sample(dolphin);check(has(dolphin,"drying"),"dry dolphin installs drying state");
        fluid(Blocks.WATER);dolphin.baseTick();env.sample(dolphin);check(!has(dolphin,"drying"),"water removes dolphin drying state");
        var blaze=spawn(Type.BLAZE,1.5);blaze.baseTick();env.sample(blaze);check(has(blaze,"water_sensitive"),"water-sensitive entity activates independent water contact state");
        fluid(Blocks.AIR);blaze.baseTick();env.sample(blaze);check(!has(blaze,"water_sensitive"),"leaving water removes water sensitivity state");
        var crowd=[];for(var n=0;n<25;n++)crowd.push(spawn(Type.COW,0.5));env.sample(cow);check(has(cow,"cramming"),"native cramming population threshold activates binding");
        for(var e of crowd)e.discard();env.sample(cow);check(!has(cow,"cramming"),"population drop removes cramming");
        var UUID=Java.loadClass("java.util.UUID"),p=new (Java.loadClass("net.neoforged.neoforge.common.util.FakePlayer"))(level,new (Java.loadClass("com.mojang.authlib.GameProfile"))(UUID.randomUUID(),"BorderEffects"));
        p.connection.markClientLoaded();p.setPos(0.5,300,0.5);level.addNewPlayer(p);entities.push(p);
        var border=level.getWorldBorder(),size=border.getSize(),cx=border.getCenterX(),cz=border.getCenterZ();
        try{border.setCenter(1000,1000);border.setSize(2);env.sample(p);check(has(p,"world_border"),"actual outside-border damage facts activate binding");
            border.setCenter(0,0);border.setSize(100);env.sample(p);check(!has(p,"world_border"),"return inside border removes binding");
        }finally{border.setCenter(cx,cz);border.setSize(size);}
        return JSON.stringify({passed:checks.length,checks:checks});
    }finally{for(var e of entities)e.discard();for(var s of saved)level.setBlock(s[0],s[1],2);}
})()
