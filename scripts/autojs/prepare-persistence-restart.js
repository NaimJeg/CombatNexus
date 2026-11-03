(function(){
 function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
 var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Life=cls("combat.spi.CombatWorldLifecycleAccess");Life.beginTick(world);
 var Id=Java.loadClass("net.minecraft.resources.Identifier"),List=Java.loadClass("java.util.List"),Set=Java.loadClass("java.util.Set"),OL=Java.loadClass("java.util.OptionalLong");
 for(var old of level.getAllEntities())if(old.entityTags().contains("combatnexus_persistence_probe")||old.entityTags().contains("combatnexus_equipment_restart_probe"))old.discard();
 var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
 e.setPos(0,512,0);e.setNoGravity(true);e.setNoAi(true);e.addTag("combatnexus_persistence_probe");level.addFreshEntity(e);
 cls("combat.platform.CombatPersistence").tick(level);
 var id=Id.parse("combatnexus:restart_probe"),EI=cls("effect.api.EffectInstance"),ES=cls("effect.api.EffectService");
 world.sourcePersistence(e.getUUID(),id,cls("combat.internal.CombatRuntime$SourcePersistence").PERSISTENT);
 var plan=new (cls("combat.api.behavior.BehaviorPlan"))(0,List.of(new (cls("combat.api.behavior.BehaviorComponent$LethalProtection"))()));
 world.effects().apply(new EI(new (cls("effect.api.EffectSnapshot"))(e.getUUID(),id,id,0,OL.of(level.getGameTime()+100000),Set.of()),List.of(),EI.Ownership.MANAGED,plan),ES.Stacking.REPLACE);
 e.setHealth(5);e.invulnerableTime=0;e.hurtServer(level,e.damageSources().generic(),1000);
 if(e.getHealth()!==1)throw new Error("Protection fixture failed");
 cls("combat.platform.CombatPersistence").save(e);
 var api=cls("combat.api.CombatAccess").find(level).orElseThrow(),UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional");
 var source=new (cls("combat.api.CombatSource"))(id,Optional.empty(),Optional.empty()),Example=Java.loadClass("example.combatnexus.PlanExample");
 api.states().write(UUID.randomUUID(),new (cls("combat.state.api.StateAddress"))(e.getUUID(),Example.RETAINED_STATE,Optional.empty()),source,Java.loadClass("java.lang.Integer").valueOf(17));
 api.cooldowns().submit(new (cls("combat.state.api.CooldownCommand"))(UUID.randomUUID(),e.getUUID(),source,cls("combat.state.api.CooldownCommand$Operation").START,cls("combat.state.api.CooldownStamp$DeathPolicy").RETAIN));
 api.absorption().submit(new (cls("combat.absorption.api.AbsorptionCommand"))(UUID.randomUUID(),e.getUUID(),source,cls("combat.absorption.api.AbsorptionCommand$Operation").GRANT,3,5));
 api.scheduler().schedule(UUID.randomUUID(),e.getUUID(),new (cls("combat.api.scheduling.ScheduledTask"))(UUID.randomUUID(),source,Optional.empty(),level.getGameTime()+100000,20,2,new (Java.loadClass("example.combatnexus.SemanticExample$Combo"))(2),Set.of(),true));
 var instance=new (cls("combat.api.source.SourceInstance"))(UUID.randomUUID(),Id.parse("combatnexus_sdk_example:restored_probe"),Optional.empty(),"probe");
 api.sources().install(new (cls("combat.api.source.SourceInstallation"))(UUID.randomUUID(),e.getUUID(),instance,new (cls("combat.api.source.SourcePlan"))(id,Java.loadClass("java.util.Map").of(),Java.loadClass("java.util.Map").of(),Java.loadClass("java.util.Map").of(),true)));
 cls("combat.platform.CombatPersistence").save(e);
 var equipped=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
 equipped.setPos(1,512,0);equipped.setNoGravity(true);equipped.setNoAi(true);equipped.addTag("combatnexus_equipment_restart_probe");level.addFreshEntity(equipped);
 cls("combat.platform.CombatPersistence").tick(level);
 var slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot").MAINHAND,Bindings=cls("combat.api.behavior.EquipmentBindings");
 var stack=new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").STICK);
 stack.set(Bindings.component(),new Bindings(List.of(new (cls("combat.api.behavior.EquipmentBindings$Binding"))(Id.parse("combatnexus:restart_guard"),Bindings.Kind.BEHAVIOR,Id.parse("combatnexus:last_stand"),List.of(slot)))));
 equipped.setItemSlot(slot,stack);cls("combat.spi.CombatEquipmentAccess").changed(world,equipped.getUUID(),slot,stack);
 equipped.setHealth(5);equipped.invulnerableTime=0;equipped.hurtServer(level,equipped.damageSources().generic(),1000);
 if(equipped.getHealth()!==1)throw new Error("Equipment protection fixture failed");
 cls("combat.platform.CombatPersistence").save(equipped);
 return JSON.stringify({prepared:true,target:String(e.getUUID()),saved:String(e.getPersistentData().getString("combatnexus:combat_state").orElseThrow())});
})()
