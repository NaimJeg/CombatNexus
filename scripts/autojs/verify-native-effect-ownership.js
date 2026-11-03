(function () {
    function cls(n) { return Java.loadClass("io.github.naimjeg.combatnexus." + n); }
    var level=AutoJS.server().overworld(), world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    cls("combat.spi.CombatWorldLifecycleAccess").beginTick(world);
    var Type=Java.loadClass("net.minecraft.world.entity.EntityType"), Reason=Java.loadClass("net.minecraft.world.entity.EntitySpawnReason");
    var Instance=Java.loadClass("net.minecraft.world.effect.MobEffectInstance"), Effects=Java.loadClass("net.minecraft.world.effect.MobEffects");
    var cow=Type.COW.create(level,Reason.COMMAND), other=Type.COW.create(level,Reason.COMMAND);
    cow.setPos(0,512,0);cow.setNoAi(true);cow.setNoGravity(true);level.addFreshEntity(cow);
    other.setPos(1,512,0);other.setNoAi(true);other.setNoGravity(true);level.addFreshEntity(other);
    var checks=[];function check(v,label){if(!v)throw new Error(label);checks.push(label);}
    try {
        var ownedCount=0, count=0, bridge=cls("effect.platform.NativeEffectBridge"), registry=Java.loadClass("net.minecraft.core.registries.BuiltInRegistries").MOB_EFFECT;
        for(var holder of registry.listElements().toList()) {
            if(String(holder.key().identifier().getNamespace())!=="minecraft")continue;
            count++;cow.setHealth(5);cow.removeAllEffects();
            check(cow.addEffect(new Instance(holder,100,0)),"native application accepted: "+holder.key().identifier());
            var managed=world.effects().snapshot(cow.getUUID());
            var owned=bridge.managed(holder);if(owned)ownedCount++;
            check(!owned||holder.value().isInstantenous()?managed.isEmpty():managed.size()===1,"one lifecycle owner: "+holder.key().identifier());
            if(!holder.value().isInstantenous()) {
                check(cow.hasEffect(holder)&&cow.getEffect(holder).getDuration()===100,"native query projection: "+holder.key().identifier());
                check(cls("effect.api.EffectFacts").capture(cow).views().size()===1,"facts do not duplicate observed projection: "+holder.key().identifier());
            }
        }
        check(count===40&&ownedCount===26,"forty vanilla entries: 26 combat owners and 14 native owners");cow.removeAllEffects();
        var speed=Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").MOVEMENT_SPEED;
        var baseline=cow.getAttributeValue(speed);
        cow.addEffect(new Instance(Effects.SPEED,200,0),cow);
        cow.addEffect(new Instance(Effects.SPEED,40,2),other);
        check(world.effects().snapshot(cow.getUUID()).size()===1,"different casters share one effective vanilla layer");
        check(world.effects().snapshot(cow.getUUID()).get(0).hidden().size()===1,"different caster weak layer retained");
        check(Math.abs(cow.getAttributeValue(speed)-baseline*1.6)<0.0001,"only effective strong modifier installed");
        var copy=cow.getEffect(Effects.SPEED);copy.update(new Instance(Effects.SPEED,1000,8));
        check(cow.getEffect(Effects.SPEED).getAmplifier()===2,"returned native instance is a read-only projection copy");
        cow.addEffect(new Instance(Effects.SPEED,20,0,true,false,false),cow);
        check(!cow.getEffect(Effects.SPEED).isVisible()&&!cow.getEffect(Effects.SPEED).showIcon(),"weaker application updates native presentation flags");
        var api=cls("combat.api.CombatAccess").find(level).orElseThrow(),Id=Java.loadClass("net.minecraft.resources.Identifier"),UUID=Java.loadClass("java.util.UUID");
        api.effects().remove(UUID.randomUUID(),cow.getUUID(),Id.parse("minecraft:speed"),Id.parse("combatnexus:entity/"+cow.getUUID()),cls("effect.api.EffectService$Removal").REMOVED);
        check(world.effects().snapshot(cow.getUUID()).get(0).hidden().isEmpty()&&cow.getEffect(Effects.SPEED).getAmplifier()===2,"removing hidden source preserves effective caster");
        cow.addEffect(new Instance(Effects.SPEED,200,0),cow);
        api.effects().remove(UUID.randomUUID(),cow.getUUID(),Id.parse("minecraft:speed"),Id.parse("combatnexus:entity/"+other.getUUID()),cls("effect.api.EffectService$Removal").REMOVED);
        check(cow.getEffect(Effects.SPEED).getAmplifier()===0,"removing effective source restores other caster");
        cow.removeAllEffects();check(Math.abs(cow.getAttributeValue(speed)-baseline)<0.0001,"remove clears managed modifier");
        return JSON.stringify({passed:checks.length,checks:checks,scope:"all forty native entries, combat/native ownership split, managed single-instance query copies, effective attributes and source removal; behavior and natural ticks use companion suites"});
    }finally{cow.discard();other.discard();}
})()
