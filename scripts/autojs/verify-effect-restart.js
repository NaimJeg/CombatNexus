(function(){
    function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
    var level=AutoJS.server().overworld();cls("combat.platform.CombatPersistence").tick(level);
    var cow=null;for(var e of level.getAllEntities())if(e.entityTags().contains("combatnexus_effect_restart"))cow=e;
    var equipped=null;for(var e of level.getAllEntities())if(e.entityTags().contains("combatnexus_order_restart"))equipped=e;
    if(cow===null||equipped===null)return JSON.stringify({ready:false});
    var checks=[];function check(v,l){if(!v)throw new Error(l);checks.push(l);}
    var Effects=Java.loadClass("net.minecraft.world.effect.MobEffects"),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    var nativeSpeed=cow.getEffect(Effects.SPEED),states=world.effects().snapshot(cow.getUUID());
    check(nativeSpeed!==null&&nativeSpeed.getAmplifier()===2,"effective native layer restored after actual server restart");
    check(nativeSpeed.getDuration()>9000&&nativeSpeed.getDuration()<=10000,"remaining timed duration restored without wall-clock expiry");
    var speed=null;for(var state of states)if(String(state.snapshot().effect())==="minecraft:speed")speed=state;
    check(speed!==null&&speed.hidden().size()===1,"hidden layer restored");
    check(speed.hidden().get(0).display().owner().equals(cow.getUUID()),"hidden source attribution restored");
    check(cow.getEffect(Effects.REGENERATION).isInfiniteDuration(),"permanent native effect restored");
    check(cow.getRemainingFireTicks()>9000&&cow.getRemainingFireTicks()<=10000,"managed burning survives restart");
    var frozen=false;for(var state of states)if(String(state.snapshot().effect())==="combatnexus:freezing")frozen=true;
    check(!frozen,"expired environment binding is not restored");
    var attr=cow.getAttribute(Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").MOVEMENT_SPEED);
    check(Math.abs(attr.getValue()-attr.getBaseValue()*1.6)<0.00001,"restored modifier installed exactly once");
    check(!world.behaviors().capture(cow.getUUID()).isEmpty(),"saved periodic scheduler restored");
    var events=Java.loadClass("probe.EffectExtension").moments(),restored=[];
    for(var i=0;i<events.size();i++){var event=events.get(i);if(event instanceof cls("combat.api.action.CombatMoment$Effect")&&event.target().equals(cow.getUUID())&&String(event.effect())==="minecraft:speed")restored.push(event);}
    check(restored.length===1&&String(restored[0].change())==="ADDED","actual restart publishes one restored Effect event without replaying hidden applications");
    check(restored[0].managedChange().get().after().get(0).hidden().size()===1,"restart event contains the complete restored hidden layer");
    check(cow.hasEffect(Effects.NIGHT_VISION)&&cow.getEffect(Effects.NIGHT_VISION).getDuration()>19000,"native noncombat effect survives real restart");
    check(states.stream().noneMatch(function(s){return String(s.snapshot().effect())==="minecraft:night_vision";}),"restarted native effect is not adopted by managed service");
    var Id=Java.loadClass("net.minecraft.resources.Identifier");
    var slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot").MAINHAND;
    var before=world.behaviors().capture(equipped.getUUID()).get(0);
    var current=world.domainDefinitions().effects().get(Id.parse("combatnexus_effect_probe:restart_order")).behavior();
    check(String(current.components().get(0).type())==="combatnexus:periodic_healing"&&String(current.components().get(1).type())==="combatnexus:lethal_protection","fresh process decodes equipment definition in explicit JSON order");
    check(before.plan().equals(current),"saved multi-behavior plan equals freshly loaded definition after real restart");
    check(!world.behaviors().view(equipped.getUUID()).get(0).protectionAvailable(),"multi-behavior equipment restores consumed protection");
    cls("combat.spi.CombatEquipmentAccess").changed(world,equipped.getUUID(),slot,equipped.getItemBySlot(slot));
    var after=world.behaviors().capture(equipped.getUUID()).get(0);
    check(after.installation()===before.installation()&&!world.behaviors().view(equipped.getUUID()).get(0).protectionAvailable(),"equipment reconciliation neither reinstalls nor replenishes consumed protection");
    var remaining=after.expiresAtTick().getAsLong()-level.getGameTime();
    check(remaining>99000&&remaining<=100000,"multi-behavior equipment retains remaining lifetime");
    cls("combat.platform.CombatPersistence").save(equipped);
    var saved=cls("combat.internal.persistence.CombatStateCodec").decode(String(equipped.getPersistentData().getString("combatnexus:combat_state").orElseThrow())).behaviors().get(0);
    var periodic=saved.periodicRemaining().get(Id.parse("combatnexus:periodic_healing"));
    check(periodic!==null&&periodic.longValue()>49000&&periodic.longValue()<=50000,"multi-behavior equipment retains periodic delay instead of restarting full interval");
    check(equipped.getHealth()===1,"restoration does not replay periodic healing");
    equipped.discard();cow.discard();return JSON.stringify({ready:true,passed:checks.length,checks:checks});
})()
