(function(){
    function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
    var level=AutoJS.server().overworld();
    for(var old of level.getAllEntities())if(old.entityTags().contains("combatnexus_effect_restart"))old.discard();
    var cow=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    cow.setPos(0,512,0);cow.setNoGravity(true);cow.setNoAi(true);cow.setInvulnerable(true);cow.addTag("combatnexus_effect_restart");level.addFreshEntity(cow);
    cls("combat.platform.CombatPersistence").tick(level);
    var Effects=Java.loadClass("net.minecraft.world.effect.MobEffects"),Instance=Java.loadClass("net.minecraft.world.effect.MobEffectInstance");
    cow.addEffect(new Instance(Effects.SPEED,20000,0),cow);cow.addEffect(new Instance(Effects.SPEED,10000,2));
    cow.addEffect(new Instance(Effects.REGENERATION,-1,0));cow.setRemainingFireTicks(10000);
    cow.addEffect(new Instance(Effects.NIGHT_VISION,20000,0));
    cow.setTicksFrozen(140);cls("effect.internal.EnvironmentEffects").sample(cow);
    cls("combat.platform.CombatPersistence").save(cow);
    var json=String(cow.getPersistentData().getString("combatnexus:combat_state").orElseThrow());
    if(json.indexOf('combatnexus:freezing')>=0)throw new Error("Transient environment binding leaked into persistent state");
    if(json.indexOf('minecraft:speed')<0||json.indexOf('combatnexus:burning')<0)throw new Error("Native or timed fire state absent from save");
    cow.setTicksFrozen(0);
    for(var old of level.getAllEntities())if(old.entityTags().contains("combatnexus_order_restart"))old.discard();
    var equipped=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    equipped.setPos(1,512,0);equipped.setNoGravity(true);equipped.setNoAi(true);equipped.addTag("combatnexus_order_restart");level.addFreshEntity(equipped);
    cls("combat.platform.CombatPersistence").tick(level);
    var world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Id=Java.loadClass("net.minecraft.resources.Identifier"),List=Java.loadClass("java.util.List");
    var slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot").MAINHAND,Bindings=cls("combat.api.behavior.EquipmentBindings");
    var stack=new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").STICK);
    stack.set(Bindings.component(),new Bindings(List.of(new (cls("combat.api.behavior.EquipmentBindings$Binding"))(Id.parse("combatnexus:restart_order"),Bindings.Kind.EFFECT,Id.parse("combatnexus_effect_probe:restart_order"),List.of(slot)))));
    equipped.setItemSlot(slot,stack);cls("combat.spi.CombatEquipmentAccess").changed(world,equipped.getUUID(),slot,stack);
    var plan=world.behaviors().capture(equipped.getUUID()).get(0).plan();
    if(String(plan.components().get(0).type())!=="combatnexus:periodic_healing"||String(plan.components().get(1).type())!=="combatnexus:lethal_protection")throw new Error("Initial equipment behavior order differs from JSON array");
    equipped.setHealth(5);equipped.invulnerableTime=0;equipped.hurtServer(level,equipped.damageSources().generic(),1000);
    if(equipped.getHealth()!==1||world.behaviors().view(equipped.getUUID()).get(0).protectionAvailable())throw new Error("Equipment protection consumption failed");
    cls("combat.platform.CombatPersistence").save(equipped);
    return JSON.stringify({prepared:true,target:String(cow.getUUID()),passed:4,saved:JSON.parse(json)});
})()
