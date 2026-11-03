(function(){
    function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
    var level=AutoJS.server().overworld(),Type=Java.loadClass("net.minecraft.world.entity.EntityType"),Reason=Java.loadClass("net.minecraft.world.entity.EntitySpawnReason");
    var E=Java.loadClass("net.minecraft.world.effect.MobEffects"),I=Java.loadClass("net.minecraft.world.effect.MobEffectInstance"),UUID=Java.loadClass("java.util.UUID");
    var checks=[],entities=[],blocks=[],bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS;
    function check(v,l){if(!v)throw new Error(l);checks.push(l);}
    function spawn(type,x){var e=type.create(level,Reason.COMMAND);e.setPos(x,300,0);e.setNoGravity(true);e.setNoAi(true);level.addFreshEntity(e);entities.push(e);return e;}
    function count(type,x){var n=0;for(var e of level.getAllEntities())if(e.getType()===type&&Math.abs(e.getX()-x)<4&&e.getY()>298&&e.getY()<304)n++;return n;}
    var p=new (Java.loadClass("net.neoforged.neoforge.common.util.FakePlayer"))(level,new (Java.loadClass("com.mojang.authlib.GameProfile"))(UUID.randomUUID(),"EffectBehaviors"));
    p.connection.markClientLoaded();p.setInvulnerable(false);p.setPos(50,300,0);level.addNewPlayer(p);entities.push(p);
    try{
        var registry=Java.loadClass("net.minecraft.core.registries.BuiltInRegistries").MOB_EFFECT,attrCount=0;
        for(var holder of registry.listElements().toList()){
            if(String(holder.key().identifier().getNamespace())!=="minecraft"||holder.value().isInstantenous())continue;
            p.removeAllEffects();var baseline=new (Java.loadClass("java.util.HashMap"))();
            holder.value().createModifiers(1,function(attr,mod){baseline.put(attr,p.getAttributeValue(attr));});
            p.addEffect(new I(holder,1000,1));
            holder.value().createModifiers(1,function(attr,mod){var base=Number(baseline.get(attr)),op=String(mod.operation());var expected=op==="ADD_VALUE"?base+mod.amount():base*(1+mod.amount());
                expected=attr.value().sanitizeValue(expected);check(Math.abs(p.getAttributeValue(attr)-expected)<0.0001,"native attribute consumed exactly once: "+holder.key().identifier()+" / "+attr.unwrapKey().orElseThrow().identifier());attrCount++;});
        }
        check(attrCount>=12,"all native attribute contributions inspected on player");p.removeAllEffects();
        var util=Java.loadClass("net.minecraft.world.effect.MobEffectUtil");
        p.addEffect(new I(E.HASTE,100,2));check(util.hasDigSpeed(p)&&util.getDigSpeedAmplification(p)===2,"mining consumes managed haste projection");p.removeAllEffects();
        p.addEffect(new I(E.CONDUIT_POWER,100,0));check(util.hasDigSpeed(p)&&util.hasWaterBreathing(p),"conduit consumers retain native effect");p.removeAllEffects();
        p.addEffect(new I(E.WATER_BREATHING,100,0));check(util.hasWaterBreathing(p),"breathing consumes native water breathing");p.removeAllEffects();
        p.setOnGround(true);var stone=Java.loadClass("net.minecraft.world.level.block.Blocks").STONE.defaultBlockState(),mining=p.getDestroySpeed(stone);
        p.addEffect(new I(E.MINING_FATIGUE,100,0));check(Math.abs(p.getDestroySpeed(stone)-mining*0.3)<0.0001,"native mining fatigue consumer uses managed level");p.removeAllEffects();
        p.addEffect(new I(E.BLINDNESS,100,0));check(p.isMobilityRestricted(),"native sprint restriction consumes blindness");p.removeAllEffects();
        p.addEffect(new I(E.GLOWING,100,0));check(p.isCurrentlyGlowing(),"native glow consumer uses managed effect");p.removeAllEffects();
        var villager=spawn(Type.VILLAGER,4),Items=Java.loadClass("net.minecraft.world.item.Items"),Stack=Java.loadClass("net.minecraft.world.item.ItemStack");
        var offers=new (Java.loadClass("net.minecraft.world.item.trading.MerchantOffers"))();
        var offer=new (Java.loadClass("net.minecraft.world.item.trading.MerchantOffer"))(new (Java.loadClass("net.minecraft.world.item.trading.ItemCost"))(Items.EMERALD,20),new Stack(Items.BREAD),10,1,0);
        offers.add(offer);villager.setOffers(offers);p.addEffect(new I(E.HERO_OF_THE_VILLAGE,100,0));
        villager.mobInteract(p,Java.loadClass("net.minecraft.world.InteractionHand").MAIN_HAND);
        check(offer.getSpecialPriceDiff()===-6,"native trading discount consumes native village hero");villager.setTradingPlayer(null);p.removeAllEffects();
        var trialPos=null,BlockPos=Java.loadClass("net.minecraft.core.BlockPos");
        for(var y=250;y<270;y++){var candidate=new BlockPos(4,y,4);if((Number(candidate.asLong())+Number(level.getGameTime()))%20===0){trialPos=candidate;break;}}
        if(trialPos===null)throw new Error("Trial fixture phase");
        blocks.push([trialPos,level.getBlockState(trialPos)]);level.setBlock(trialPos,Java.loadClass("net.minecraft.world.level.block.Blocks").TRIAL_SPAWNER.defaultBlockState(),3);
        p.setPos(4.5,trialPos.getY()+1,5.5);p.addEffect(new I(E.BAD_OMEN,1000,1));
        var trial=level.getBlockEntity(trialPos).getTrialSpawner();trial.getStateData().tryDetectPlayers(level,trialPos,trial);
        check(trial.isOminous()&&!p.hasEffect(E.BAD_OMEN)&&p.getEffect(E.TRIAL_OMEN).getDuration()===36000,"native trial spawner converts native bad omen once");
        trial.getStateData().tryDetectPlayers(level,trialPos,trial);check(p.getEffect(E.TRIAL_OMEN).getDuration()===36000,"repeated trial detection does not reapply or extend omen");p.removeAllEffects();p.setPos(2,300,0);
        var exhaustion=function(){var output=Java.loadClass("net.minecraft.world.level.storage.TagValueOutput").createWithoutContext(Java.loadClass("net.minecraft.util.ProblemReporter").DISCARDING);p.getFoodData().addAdditionalSaveData(output);return Number(output.buildResult().getFloat("foodExhaustionLevel").orElseThrow());};
        var previousExhaustion=exhaustion();p.addEffect(new I(E.HUNGER,1,0));p.baseTick();
        check(Math.abs(exhaustion()-previousExhaustion-0.005)<0.00001,"one-tick hunger executes its first and last pulse once");p.removeAllEffects();
        p.getFoodData().setFoodLevel(5);p.getFoodData().setSaturation(0);p.addEffect(new I(E.SATURATION,1,1));p.baseTick();
        check(p.getFoodData().getFoodLevel()===7&&p.getFoodData().getSaturationLevel()===4&&!p.hasEffect(E.SATURATION),"native saturation feeds once and expires");
        var difficulty=level.getDifficulty(),D=Java.loadClass("net.minecraft.world.Difficulty"),env=cls("effect.internal.EnvironmentEffects");
        try{
            p.getFoodData().setFoodLevel(0);
            for(var row of [[D.EASY,10],[D.NORMAL,1]]){
                AutoJS.server().setDifficulty(row[0],true);p.setHealth(20);p.invulnerableTime=0;env.sample(p);p.hurtServer(level,p.damageSources().starve(),1000);
                check(p.getHealth()===20,"native starvation damage suppressed in favor of delayed Effect: "+row[0]);
            }
            p.getFoodData().setFoodLevel(1);env.sample(p);
            check(cls("combat.api.CombatWorldSessions").managedEffects(level,p.getUUID()).stream().noneMatch(function(e){return String(e.snapshot().effect())==="combatnexus:starvation";}),"eating removes starvation binding");
        }finally{AutoJS.server().setDifficulty(difficulty,true);p.setHealth(20);p.getFoodData().setFoodLevel(20);}
        var cow=spawn(Type.COW,2);cow.setHealth(10);cow.addEffect(new I(E.ABSORPTION,100,0));
        check(cow.getAbsorptionAmount()===4,"absorption grants one managed capacity");cow.invulnerableTime=0;cow.hurtServer(level,cow.damageSources().generic(),3);
        check(cow.getAbsorptionAmount()===1&&cow.getHealth()===10,"native damage consumes absorption once");cow.removeAllEffects();
        check(cow.getAbsorptionAmount()===0,"removal revokes unused absorption");
        cow.addEffect(new I(E.RESISTANCE,100,0));cow.invulnerableTime=0;cow.hurtServer(level,cow.damageSources().generic(),5);
        check(cow.getHealth()===6,"resistance applies one twenty-percent reduction");cow.removeAllEffects();
        cow.addEffect(new I(E.FIRE_RESISTANCE,100,0));cow.invulnerableTime=0;var hp=cow.getHealth();cow.hurtServer(level,cow.damageSources().onFire(),4);
        check(cow.getHealth()===hp,"fire immunity consumed through managed projection");cow.removeAllEffects();
        var zombie=spawn(Type.ZOMBIE,4);zombie.setHealth(5);E.INSTANT_DAMAGE.value().applyInstantenousEffect(level,null,null,zombie,0,0.5);
        check(zombie.getHealth()===7,"thrown harm heals inverted target at scaled base amount");
        var slimeCow=spawn(Type.COW,6),before=count(Type.SLIME,6);slimeCow.addEffect(new I(E.OOZING,100,0));
        slimeCow.remove(Java.loadClass("net.minecraft.world.entity.Entity$RemovalReason").KILLED);
        check(count(Type.SLIME,6)-before===2,"oozing native removal emits exactly two slimes");
        var discarded=spawn(Type.COW,8);discarded.addEffect(new I(E.OOZING,100,0));before=count(Type.SLIME,8);discarded.discard();
        check(count(Type.SLIME,8)===before,"discard does not run killed-only behavior");
        var bursts=0,listener=new (Java.loadClass("java.util.function.Consumer"))({accept:function(event){bursts++;}});
        bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").NORMAL,false,Java.loadClass("net.neoforged.neoforge.event.level.ExplosionEvent$Start"),listener);
        try{var wind=spawn(Type.COW,10);wind.addEffect(new I(E.WIND_CHARGED,100,0));wind.remove(Java.loadClass("net.minecraft.world.entity.Entity$RemovalReason").KILLED);check(bursts===1,"wind charged dispatches one native explosion");}finally{bus.unregister(listener);}
        var infested=spawn(Type.COW,12),random=Java.loadClass("net.minecraft.util.RandomSource").create(0),seed=0;
        while(seed<100000){random.setSeed(seed);if(random.nextFloat()<=0.1)break;seed++;}
        var expected=1+random.nextInt(2);infested.addEffect(new I(E.INFESTED,100,0));before=count(Type.SILVERFISH,12);infested.getRandom().setSeed(seed);
        infested.getEffect(E.INFESTED).onMobHurt(level,infested,infested.damageSources().generic(),1);
        check(count(Type.SILVERFISH,12)-before===expected,"infested deterministic hurt callback runs exactly once");
        var pos=Java.loadClass("net.minecraft.core.BlockPos"),Blocks=Java.loadClass("net.minecraft.world.level.block.Blocks");
        for(var x=13;x<=15;x++)for(var z=-1;z<=1;z++)for(var y=299;y<=301;y++){var at=new pos(x,y,z);blocks.push([at,level.getBlockState(at)]);level.setBlock(at,(y===299?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);}
        var woven=spawn(Type.COW,14);woven.getRandom().setSeed(1);woven.addEffect(new I(E.WEAVING,100,0));woven.remove(Java.loadClass("net.minecraft.world.entity.Entity$RemovalReason").KILLED);
        var webs=0;for(var saved of blocks)if(level.getBlockState(saved[0]).is(Blocks.COBWEB))webs++;
        check(webs>=1&&webs<=3,"weaving emits bounded cobwebs on valid floor");
        return JSON.stringify({passed:checks.length,checks:checks});
    }finally{for(var e of entities)e.discard();for(var e of level.getAllEntities())if(e!==null&&e.getY()>298&&e.getY()<304&&e.getX()>0&&e.getX()<16&&(e.getType()===Type.SLIME||e.getType()===Type.SILVERFISH))e.discard();for(var saved of blocks)level.setBlock(saved[0],saved[1],3);}
})()
