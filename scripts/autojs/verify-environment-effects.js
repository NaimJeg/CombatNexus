(function () {
    function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
    var level=AutoJS.server().overworld(), world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    cls("combat.spi.CombatWorldLifecycleAccess").beginTick(world);
    var Type=Java.loadClass("net.minecraft.world.entity.EntityType"), Reason=Java.loadClass("net.minecraft.world.entity.EntitySpawnReason"), Id=Java.loadClass("net.minecraft.resources.Identifier");
    var BlockPos=Java.loadClass("net.minecraft.core.BlockPos"), Blocks=Java.loadClass("net.minecraft.world.level.block.Blocks");
    var Effects=Java.loadClass("net.minecraft.world.effect.MobEffects"), Instance=Java.loadClass("net.minecraft.world.effect.MobEffectInstance");
    var env=cls("effect.internal.EnvironmentEffects"),checks=[],cow=Type.COW.create(level,Reason.COMMAND);
    cow.setPos(0.5,100,0.5);cow.setNoAi(true);cow.setNoGravity(true);level.addFreshEntity(cow);
    function check(v,l){if(!v)throw new Error(l);checks.push(l);}
    function state(name){return world.effects().snapshot(cow.getUUID()).stream().filter(function(e){return String(e.snapshot().effect())==="combatnexus:"+name;}).findFirst();}
    var pos=new BlockPos(0,100,0),above=new BlockPos(0,101,0),old=level.getBlockState(pos),oldAbove=level.getBlockState(above);
    var below=pos.below(),oldBelow=level.getBlockState(below);
    try {
        cow.setRemainingFireTicks(100);
        check(state("burning").isPresent()&&cow.getRemainingFireTicks()===100,"ignition installs a timed authoritative state");
        var fire=state("burning").get();
        cow.setRemainingFireTicks(100);
        check(state("burning").get().equals(fire),"same ignition deadline is idempotent");
        cow.igniteForTicks(200);check(cow.getRemainingFireTicks()===200,"ignition extension updates the same binding");
        cow.setRemainingFireTicks(40);check(cow.getRemainingFireTicks()===40,"explicit shortening updates authoritative duration");
        cow.removeAllEffects();check(state("burning").isPresent(),"ordinary clearing does not extinguish condition-owned fire");
        cow.clearFire();check(!state("burning").isPresent()&&cow.getRemainingFireTicks()<=0,"extinguishing removes the timed state");
        level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(above,Blocks.STONE.defaultBlockState());
        env.sample(cow);check(state("suffocation").isPresent(),"wall condition installs permanent suffocation");
        var bound=state("suffocation").get();env.sample(cow);check(state("suffocation").get().equals(bound),"repeated facts do not reinstall state");
        cow.removeAllEffects();check(state("suffocation").isPresent(),"milk-style removal retains suffocation");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(above,Blocks.AIR.defaultBlockState());
        env.sample(cow);check(!state("suffocation").isPresent(),"leaving wall removes suffocation");
        level.setBlockAndUpdate(pos,Blocks.CACTUS.defaultBlockState());env.sample(cow);
        check(state("cactus_contact").isPresent(),"contact facts install cactus state");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());env.sample(cow);
        check(!state("cactus_contact").isPresent(),"contact ends remove cactus state");
        cow.setTicksFrozen(200);env.sample(cow);check(state("freezing").isPresent(),"freezing fact installs a permanent state");
        cow.setTicksFrozen(0);env.sample(cow);check(!state("freezing").isPresent(),"thawing removes freezing state");
        var contacts=[["fire_contact",Blocks.FIRE.defaultBlockState()],["campfire_contact",Blocks.CAMPFIRE.defaultBlockState()],
            ["hot_floor_contact",Blocks.MAGMA_BLOCK.defaultBlockState()],["berry_contact",Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(Java.loadClass("net.minecraft.world.level.block.SweetBerryBushBlock").AGE,Java.loadClass("java.lang.Integer").valueOf(2))]];
        level.setBlockAndUpdate(below,Blocks.STONE.defaultBlockState());
        for(var contact of contacts){
            level.setBlockAndUpdate(pos,contact[1]);env.sample(cow);check(state(contact[0]).isPresent(),"contact installs "+contact[0]);
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());env.sample(cow);check(state(contact[0]).isEmpty(),"contact exit removes "+contact[0]);
        }
        level.setBlockAndUpdate(pos,Blocks.CAMPFIRE.defaultBlockState());cow.addEffect(new Instance(Effects.FIRE_RESISTANCE,100,0));env.sample(cow);
        check(state("campfire_contact").isEmpty(),"fire protection deactivates contact binding");cow.removeEffect(Effects.FIRE_RESISTANCE);env.sample(cow);
        check(state("campfire_contact").isPresent(),"immunity removal reinstalls eligible contact");level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());env.sample(cow);
        var before=cow.getHealth();
        Effects.INSTANT_HEALTH.value().applyInstantenousEffect(level,null,null,cow,0,0.5);
        check(cow.getHealth()===Math.min(cow.getMaxHealth(),before+2),"direct instant entry applies scaled healing once");
        check(!cow.hasEffect(Effects.INSTANT_HEALTH),"direct instant application leaves no projection");
        return JSON.stringify({passed:checks.length,checks:checks});
    }finally{level.setBlockAndUpdate(pos,old);level.setBlockAndUpdate(above,oldAbove);level.setBlockAndUpdate(below,oldBelow);cow.discard();}
})()
