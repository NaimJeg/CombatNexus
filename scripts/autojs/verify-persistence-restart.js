(function(){
 function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
 var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Life=cls("combat.spi.CombatWorldLifecycleAccess");Life.beginTick(world);
 cls("combat.platform.CombatPersistence").tick(level);
 var found=null,equipped=null;for(var e of level.getAllEntities()){
  if(e.entityTags().contains("combatnexus_persistence_probe"))found=e;
  if(e.entityTags().contains("combatnexus_equipment_restart_probe"))equipped=e;
 }
 if(found===null||equipped===null)return JSON.stringify({ready:false});
 var effects=world.effects().snapshot(found.getUUID());
 if(effects.size()!==1)throw new Error("Persistent effect missing after actual restart");
 if(world.behaviors().view(found.getUUID()).get(0).protectionAvailable())throw new Error("Consumed protection replenished after restart");
 if(effects.get(0).snapshot().expiresAtTick().getAsLong()<=level.getGameTime())throw new Error("Restored duration expired");
 var slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot").MAINHAND;
 cls("combat.spi.CombatEquipmentAccess").changed(world,equipped.getUUID(),slot,equipped.getItemBySlot(slot));
 if(world.behaviors().view(equipped.getUUID()).size()!==1||world.behaviors().view(equipped.getUUID()).get(0).protectionAvailable())throw new Error("Equipment reload replenished protection");
 var api=cls("combat.api.CombatAccess").find(level).orElseThrow(),Optional=Java.loadClass("java.util.Optional"),UUID=Java.loadClass("java.util.UUID"),Example=Java.loadClass("example.combatnexus.PlanExample"),id=Java.loadClass("net.minecraft.resources.Identifier").parse("combatnexus:restart_probe");
 if(api.states().read(new (cls("combat.state.api.StateAddress"))(found.getUUID(),Example.RETAINED_STATE,Optional.empty())).orElseThrow().intValue()!==17)throw new Error("Typed state missing after restart");
 if(api.cooldowns().snapshot(found.getUUID(),id).isEmpty())throw new Error("Cooldown missing after restart");
 if(found.getAbsorptionAmount()!==3||api.absorption().snapshot(found.getUUID()).get(0).balance()!==3)throw new Error("Absorption ownership missing after restart");
 if(api.scheduler().snapshot(found.getUUID()).size()!==1||api.scheduler().snapshot(found.getUUID()).get(0).repetitions()!==2)throw new Error("Schedule missing after restart");
 if(api.scheduler().snapshot(found.getUUID()).get(0).intent().amount()!==2)throw new Error("External intent parameters missing after restart");
 var restored=Example.restored().stream().filter(function(s){return s.source().target().equals(found.getUUID());}).findFirst().orElseThrow();
 if(!api.sources().revoke(UUID.randomUUID(),restored.handle()).applied()||!api.sources().snapshot(found.getUUID()).isEmpty())throw new Error("Restored owner handle does not revoke exact source");
 Life.clear(world,equipped.getUUID(),cls("effect.api.EffectService").Removal.DEATH);equipped.discard();
 Life.clear(world,found.getUUID(),cls("effect.api.EffectService").Removal.DEATH);found.discard();
 return JSON.stringify({ready:true,passed:10,checks:["entity-native state survives actual server restart","consumed protection remains consumed","remaining lifetime restored","native equipment plus installation ledger avoids replenishment","typed state restored","cooldown restored","absorption ownership restored","scheduled command restored","external intent codec parameters restored","restored owner revokes exact source"]});
})()
