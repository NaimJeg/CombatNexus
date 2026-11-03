// Isolated server: real resource writes plus persistence and control lifecycle.
(function () {
    function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
    var Id=Java.loadClass("net.minecraft.resources.Identifier"), UUID=Java.loadClass("java.util.UUID");
    var List=Java.loadClass("java.util.List"), Set=Java.loadClass("java.util.Set"), Map=Java.loadClass("java.util.Map");
    var Optional=Java.loadClass("java.util.Optional"), OL=Java.loadClass("java.util.OptionalLong");
    var level=AutoJS.server().overworld(), world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    var Life=cls("combat.spi.CombatWorldLifecycleAccess"); Life.beginTick(world);
    var Attr=cls("attribute.api.CombatAttributes"), Native=Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes");
    var ES=cls("effect.api.EffectService"), EI=cls("effect.api.EffectInstance"), BP=cls("combat.api.behavior.BehaviorPlan");
    var entities=[],checks=[];
    var Definitions=cls("combat.internal.definition.CombatDefinitions"),original=Definitions.forServer(AutoJS.server());
    function id(n){return Id.parse("combatnexus:"+n);}
    function check(ok,n){if(!ok)throw new Error(n);checks.push(n);}
    function near(a,b,n){check(Math.abs(a-b)<0.001,n+" actual="+a+" expected="+b);}
    function rejects(f,n){var rejected=false;try{f();}catch(e){rejected=true;}check(rejected,n);}
    function cow(){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);e.setPos(0,100,0);level.addFreshEntity(e);entities.push(e);return e;}
    function source(n){return new (cls("combat.api.CombatSource"))(id(n),Optional.empty(),Optional.empty());}
    function hit(v,a,n){v.invulnerableTime=0;return cls("combat.damage.api.DamageModuleApi").submitDamage(cls("combat.damage.api.damage.DamageRequest").builder(level,v,cls("combat.damage.api.damage.DamageSourceDescriptor").from(v.damageSources().mobAttack(a)),n).logicalAttacker(a).directEntity(a).build());}
    function effect(e,n,restrictions,behavior){return new EI(new (cls("effect.api.EffectSnapshot"))(e.getUUID(),id(n),id(n),0,OL.of(level.getGameTime()+200),restrictions),List.of(),EI.Ownership.MANAGED,behavior);}
    try {
        var a=cow(),v=cow(),other=cow();
        var formula=cls("formula.PenetrationFormula");
        near(formula.calculate(20,0.5,3).effectiveArmor(),10,"proportional wins");
        near(formula.calculate(20,0.2,7).effectiveArmor(),13,"flat wins");
        near(formula.calculate(20,0.5,10).effectiveArmor(),10,"tie applied once");
        near(formula.calculate(20,1,50).effectiveArmor(),0,"penetration floor");
        rejects(function(){formula.calculate(20,1.1,0);},"invalid ratio rejected");
        near(v.getAttributeValue(Attr.vulnerableDamageAdditive()),0,"vulnerability default zero");
        v.getAttribute(Attr.vulnerableDamageAdditive()).setBaseValue(0.5);
        var before=v.getHealth();hit(v,a,4);near(before-v.getHealth(),6,"target percentage assembled");
        v.getAttribute(Attr.vulnerableDamageAdditive()).setBaseValue(0);v.setHealth(v.getMaxHealth());
        v.getAttribute(Attr.dodgeChance()).setBaseValue(1);
        before=v.getHealth();var avoided=hit(v,a,4);near(v.getHealth(),before,"dodge prevents health write");
        var snap=avoided.settlement().orElseThrow();check(snap.cancelled(),"dodge cancellation recorded");
        rejects(function(){snap.lineage().transaction().child(cls("combat.api.transaction.CombatOperation").HEALING);},"dodge rejects causal child");
        before=other.getHealth();hit(other,a,4);near(before-other.getHealth(),4,"other target unaffected");
        v.invulnerableTime=0;before=v.getHealth();v.hurtServer(level,v.damageSources().fall(),2);near(before-v.getHealth(),2,"environment does not roll dodge");
        v.getAttribute(Attr.dodgeChance()).setBaseValue(0);
        v.getAttribute(Native.ARMOR).setBaseValue(20);a.getAttribute(Attr.armorPenetrationFlat()).setBaseValue(20);
        before=v.getHealth();hit(v,a,4);near(before-v.getHealth(),4,"real managed armor penetration");
        a.getAttribute(Attr.armorPenetrationFlat()).setBaseValue(0);v.getAttribute(Native.ARMOR).setBaseValue(0);
        var Policy=cls("combat.damage.config.VanillaCompatibilitySettings"),Mode=cls("combat.damage.config.VanillaReductionCompatibilityMode");
        var witch=Java.loadClass("net.minecraft.world.entity.EntityType").WITCH.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        witch.setPos(0,100,0);level.addFreshEntity(witch);entities.push(witch);
        var modes=[Mode.FULL_REPLACEMENT,Mode.COOPERATIVE,Mode.CONFIGURABLE];
        for(var modeIndex=0;modeIndex<modes.length;modeIndex++){
            var policy=new Policy(modes[modeIndex],true,true,true,true);
            Definitions.publish(AutoJS.server(),new (cls("combat.internal.definition.DefinitionGeneration"))(Definitions.nextGeneration(),
                new (cls("combat.config.CombatServerSettings"))(original.settings().formulas(),policy,original.settings().safety()),original.damage(),original.behaviors(),original.domains()));
            witch.setHealth(witch.getMaxHealth());witch.invulnerableTime=0;before=witch.getHealth();
            witch.hurtServer(level,witch.damageSources().magic(),8);near(before-witch.getHealth(),1.2,"witch resistant type once "+modes[modeIndex]);
            witch.invulnerableTime=0;before=witch.getHealth();witch.hurtServer(level,witch.damageSources().mobAttack(witch),8);
            near(before-witch.getHealth(),0,"witch self immunity "+modes[modeIndex]);
            v.setHealth(v.getMaxHealth());v.getAttribute(Native.ARMOR).setBaseValue(20);
            a.getAttribute(Attr.armorPenetrationRatio()).setBaseValue(0.5);a.getAttribute(Attr.armorPenetrationFlat()).setBaseValue(7);
            before=v.getHealth();hit(v,a,4);near(before-v.getHealth(),modeIndex===1?2.72:60/23,"penetration before native or managed armor "+modes[modeIndex]);
        }
        Definitions.publish(AutoJS.server(),new (cls("combat.internal.definition.DefinitionGeneration"))(Definitions.nextGeneration(),original.settings(),original.damage(),original.behaviors(),original.domains()));
        a.getAttribute(Attr.armorPenetrationRatio()).setBaseValue(0);a.getAttribute(Attr.armorPenetrationFlat()).setBaseValue(0);v.getAttribute(Native.ARMOR).setBaseValue(0);
        var Def=cls("combat.shield.api.ShieldDefinition"), Grant=cls("combat.shield.api.ShieldGrant");
        var fire=id("fire"),cold=id("cold");
        var def=new Def(10,0,Set.of(),Set.of(),Set.of(fire),0,0,Def.Stacking.REPLACE_SAME_SOURCE);
        world.shields().replace(v.getUUID(),new Grant(source("channel_pool"),def,10,OL.empty()));
        var channels=new (Java.loadClass("java.util.LinkedHashMap"))();channels.put(fire,Java.loadClass("java.lang.Float").valueOf(2));channels.put(cold,Java.loadClass("java.lang.Float").valueOf(2));
        var reservation=world.shields().reserve(v.getUUID(),999001,Id.parse("minecraft:generic"),8,channels);
        try {near(reservation.absorbed(),4,"Pre total scales channel shares");near(reservation.remainingDamage(),4,"unmatched channel remains");var receipt=reservation.commit();near(receipt.absorbedChannels().get(fire),4,"per-channel receipt");}finally{reservation.close();}
        world.shields().replace(other.getUUID(),new Grant(source("physical_pool"),new Def(2,0,Set.of(),Set.of(),Set.of(id("physical")),0,0,Def.Stacking.REPLACE_SAME_SOURCE),2,OL.empty()));
        before=other.getHealth();hit(other,a,4);near(before-other.getHealth(),2,"native damage supplies channel basis to shield");
        near(world.shields().snapshot(other.getUUID()).get(0).balance(),0,"native channel pool depleted");
        world.shields().remove(other.getUUID(),id("physical_pool"),cls("combat.shield.api.ShieldPoolSnapshot$RemovalReason").REMOVED);
        var Action=cls("effect.api.EffectSnapshot$RestrictedAction"),Gate=cls("combat.api.action.CombatActionGate");
        world.effects().apply(effect(v,"control_a",Set.of(Action.JUMP,Action.TURN,Action.ATTACK),BP.NONE),ES.Stacking.REPLACE);
        world.effects().apply(effect(v,"control_b",Set.of(Action.JUMP),BP.NONE),ES.Stacking.REPLACE);
        check(!Gate.allows(v,Action.JUMP),"control gate active");
        var yaw=v.getYRot();v.setYRot(yaw+90);near(v.getYRot(),yaw,"native turn prevented");
        v.setDeltaMovement(Java.loadClass("net.minecraft.world.phys.Vec3").ZERO);v.jumpFromGround();near(v.getDeltaMovement().y,0,"native jump prevented");
        world.effects().remove(v.getUUID(),id("control_a"),id("control_a"),ES.Removal.DISPELLED);
        check(!Gate.allows(v,Action.JUMP)&&Gate.allows(v,Action.TURN),"control source union");
        world.effects().remove(v.getUUID(),id("control_b"),id("control_b"),ES.Removal.DISPELLED);
        check(Gate.allows(v,Action.JUMP),"last control removed");
        world.effects().apply(effect(v,"all_control",Set.copyOf(Java.loadClass("java.util.Arrays").asList(Action.values())),BP.NONE),ES.Stacking.REPLACE);
        for(var action of Action.values())check(!Gate.allows(v,action),"authoritative gate "+action);
        v.setDeltaMovement(Java.loadClass("net.minecraft.world.phys.Vec3").ZERO);v.push(1,1,1);check(v.getDeltaMovement().length()>0,"native push retained");v.setDeltaMovement(Java.loadClass("net.minecraft.world.phys.Vec3").ZERO);
        v.knockback(1,1,0);near(v.getDeltaMovement().length(),0,"knockback blocked");
        var oldX=v.getX();v.move(Java.loadClass("net.minecraft.world.entity.MoverType").PISTON,new (Java.loadClass("net.minecraft.world.phys.Vec3"))(1,0,0));check(v.getX()>oldX,"native piston movement retained");
        v.setPos(oldX+2,100,0);near(v.getX(),oldX+2,"administrative position authority retained");
        var arrow=Java.loadClass("net.minecraft.world.entity.EntityType").ARROW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);arrow.setOwner(v);
        check(!level.addFreshEntity(arrow),"native projectile launch blocked");arrow.discard();
        var teleport=new (Java.loadClass("net.neoforged.neoforge.event.entity.EntityTeleportEvent$EnderEntity"))(v,0,100,0);
        Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS.post(teleport);check(teleport.isCanceled(),"gameplay teleport blocked");
        var commandTeleport=new (Java.loadClass("net.neoforged.neoforge.event.entity.EntityTeleportEvent$TeleportCommand"))(v,level,0,100,0);
        Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS.post(commandTeleport);check(!commandTeleport.isCanceled(),"command teleport retained");
        world.effects().remove(v.getUUID(),id("all_control"),id("all_control"),ES.Removal.DISPELLED);
        world.sourcePersistence(v.getUUID(),id("persistent"),cls("combat.internal.CombatRuntime$SourcePersistence").PERSISTENT);
        world.sourcePersistence(v.getUUID(),id("channel_pool"),cls("combat.internal.CombatRuntime$SourcePersistence").PERSISTENT);
        var protect=new BP(0,List.of(new (cls("combat.api.behavior.BehaviorComponent$LethalProtection"))()));
        world.effects().apply(effect(v,"persistent",Set.of(),protect),ES.Stacking.REPLACE);
        v.setHealth(5);hit(v,a,1000);near(v.getHealth(),1,"one-use protection consumed");
        var encoded=Life.save(world,v.getUUID());
        check(String(encoded).indexOf('"savedTick":')>=0&&String(encoded).indexOf('"version"')<0,"current detached save without format version");
        var codec=cls("combat.internal.persistence.CombatStateCodec");
        check(codec.decode(encoded).effects().size()===1,"persistent effect round trip");
        rejects(function(){codec.decode(String(encoded).replace('"savedTick":','"unknownField":'));},"unknown save field rejected");
        Life.clear(world,v.getUUID(),ES.Removal.WORLD_EXIT);
        Life.restore(world,v.getUUID(),encoded);
        check(world.effects().snapshot(v.getUUID()).size()===1,"persistent effect restored");
        check(!world.behaviors().view(v.getUUID()).get(0).protectionAvailable(),"consumed protection not replenished");
        near(world.shields().snapshot(v.getUUID()).get(0).balance(),6,"shield balance restored");
        check(world.effects().snapshot(v.getUUID()).get(0).snapshot().expiresAtTick().getAsLong()-level.getGameTime()===200,"remaining duration preserved");
        var stale=Java.loadClass("com.google.gson.JsonParser").parseString(encoded).getAsJsonObject();
        stale.getAsJsonArray("shields").get(0).getAsJsonObject().getAsJsonObject("grant").addProperty("expiresAtTick",Java.loadClass("java.lang.Long").valueOf(level.getGameTime()-1));
        Life.clear(world,v.getUUID(),ES.Removal.WORLD_EXIT);Life.restore(world,v.getUUID(),stale.toString());
        check(world.shields().snapshot(v.getUUID()).isEmpty(),"one-tick-expired shield never restored as permanent");
        world.sourcePersistence(other.getUUID(),id("overflow_save"),cls("combat.internal.CombatRuntime$SourcePersistence").PERSISTENT);
        var Limit=cls("combat.api.action.TriggerLimit"),Scope=cls("combat.api.action.TriggerLimit$Scope");
        var overflow=new BP(0,List.of(new (cls("combat.api.behavior.BehaviorComponent$OverflowShield"))(1,10,200,new Limit(Scope.SOURCE,1))));
        world.effects().apply(effect(other,"overflow_save",Set.of(),overflow),ES.Stacking.REPLACE);
        other.setHealth(other.getMaxHealth());other.heal(5);
        near(world.shields().snapshot(other.getUUID()).get(0).balance(),5,"overflow fixture balance");
        var overflowSave=Life.save(world,other.getUUID());Life.clear(world,other.getUUID(),ES.Removal.WORLD_EXIT);Life.restore(world,other.getUUID(),overflowSave);
        near(world.shields().snapshot(other.getUUID()).get(0).balance(),5,"overflow pool rebound after restore");
        other.heal(2);near(world.shields().snapshot(other.getUUID()).get(0).balance(),5,"source trigger allowance not replenished");
        world.effects().remove(other.getUUID(),id("overflow_save"),id("overflow_save"),ES.Removal.DISPELLED);
        check(world.shields().snapshot(other.getUUID()).isEmpty(),"restored overflow revoked with its new installation");
    } finally {for(var i=0;i<entities.length;i++){Life.clear(world,entities[i].getUUID(),ES.Removal.DEATH);entities[i].discard();}
        Definitions.publish(AutoJS.server(),new (cls("combat.internal.definition.DefinitionGeneration"))(Definitions.nextGeneration(),original.settings(),original.damage(),original.behaviors(),original.domains()));}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
