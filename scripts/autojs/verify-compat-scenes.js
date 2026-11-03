(function(){
    var mods=Java.loadClass("net.neoforged.fml.ModList").get();
    if(!mods.isLoaded("apothic_attributes"))return JSON.stringify({passed:0,skipped:"optional mod absent"});
    function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
    var level=AutoJS.server().overworld(),api=cls("combat.api.CombatAccess").find(level).orElseThrow(),checks=[],entities=[],listener=null;
    var UUID=Java.loadClass("java.util.UUID"),Id=Java.loadClass("net.minecraft.resources.Identifier"),Optional=Java.loadClass("java.util.Optional");
    function check(v,s){if(!v)throw new Error(s);checks.push(s);}
    function cow(){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);e.setPos(0,512,0);e.setNoAi(true);e.setNoGravity(true);level.addFreshEntity(e);entities.push(e);return e;}
    function F(x){return Java.loadClass("java.lang.Float").parseFloat(String(x));}
    try{
        var attacker=cow(),victim=cow(),Attr=Java.loadClass("dev.shadowsoffire.apothic_attributes.api.ALObjects$Attributes");
        attacker.getAttribute(Attr.CRIT_CHANCE).setBaseValue(2.25);attacker.getAttribute(Attr.CRIT_DAMAGE).setBaseValue(3);
        var random=Java.loadClass("net.minecraft.util.RandomSource").create(7123),expected=10,chance=2.25,multiplier=3,samples=[],multipliers=[];
        while(true){var sample=random.nextFloat();samples.push(sample);if(!(sample<=chance&&multiplier>1))break;
            chance--;expected=F(expected+F(10*F(multiplier-1)));multipliers.push(multiplier);multiplier=F(multiplier*F(0.85));}
        victim.getRandom().setSeed(7123);
        var request=new (cls("combat.api.bridge.DamageFormulaRequest"))(UUID.randomUUID(),Id.parse("apothic_attributes:multi_critical"),victim.getUUID(),Optional.of(attacker.getUUID()),10,Optional.empty(),Optional.empty());
        var result=api.damageFormula(request);
        check(result.completed()&&Math.abs(result.output().amount()-expected)<0.00001,"multi-critical equals native float algorithm");
        check(result.samples().size()===samples.length&&result.output().multipliers().size()===multipliers.length,"multi-critical records every sample and multiplier");
        for(var i=0;i<samples.length;i++)check(result.samples().get(i).floatValue()===samples[i],"recorded critical sample "+i);
        var duplicate=false;try{api.damageFormula(request);}catch(e){duplicate=true;}check(duplicate,"formula invocation cannot reroll");
        victim.getRandom().setSeed(7123);
        var event=new (Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent"))(victim,
            new (Java.loadClass("net.neoforged.neoforge.common.damagesource.DamageContainer"))(victim.damageSources().mobAttack(attacker),10));
        new (Java.loadClass("dev.shadowsoffire.apothic_attributes.impl.AttributeEvents"))().apothCriticalStrike(event);
        check(Math.abs(event.getAmount()-expected)<0.00001,"native critical entry replaced exactly once");
        var over=cow(),overVictim=cow();over.getAttribute(Attr.CRIT_CHANCE).setBaseValue(0);over.getAttribute(Attr.OVERHEAL).setBaseValue(1);
        api.damage(cls("combat.damage.api.damage.DamageRequest").builder(level,overVictim,cls("combat.damage.api.damage.DamageSourceDescriptor").from(overVictim.damageSources().mobAttack(over)),4).logicalAttacker(over).directEntity(over).build());
        check(over.getAbsorptionAmount()===4,"Overheal original post-hit route grants once");
        check(api.absorption().snapshot(over.getUUID()).size()===1&&api.absorption().snapshot(over.getUUID()).get(0).granted()===4,"Overheal source owns the actual native absorption");
        if(mods.isLoaded("apotheosis")){
            var mage=cow(),Items=Java.loadClass("net.minecraft.world.item.Items"),Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot");
            var helmet=new Stack(Items.DIAMOND_HELMET),Socket=Java.loadClass("dev.shadowsoffire.apotheosis.socket.SocketHelper");
            var gem=Java.loadClass("dev.shadowsoffire.apotheosis.socket.gem.GemRegistry").INSTANCE.holder(Id.parse("apotheosis:the_end/mageslayer")).get();
            var gemStack=Java.loadClass("dev.shadowsoffire.apotheosis.socket.gem.GemItem").createStack(gem,Java.loadClass("dev.shadowsoffire.apotheosis.socket.gem.Purity").PERFECT);
            Socket.setSockets(helmet,1);helmet=Socket.socketGemInItem(helmet,gemStack);mage.setItemSlot(Slot.HEAD,helmet);
            var magic=function(){mage.invulnerableTime=0;return api.damage(cls("combat.damage.api.damage.DamageRequest").builder(level,mage,cls("combat.damage.api.damage.DamageSourceDescriptor").from(mage.damageSources().magic()),4).build());};
            mage.setHealth(5);var mageResult=magic(),plan=mageResult.settlement().orElseThrow().evidence().beforeApplication();
            check(plan.steps().size()===1&&plan.completed(),"MageSlayer native hook retains child evidence");
            check(Math.abs(mage.getHealth()-3.8)<0.001,"MageSlayer heals before reduced damage exactly once");
            mage.setHealth(mage.getMaxHealth());mageResult=magic();plan=mageResult.settlement().orElseThrow().evidence().beforeApplication();
            check(String(plan.steps().get(0).result().value().orElseThrow().outcome())==="FULL_HEALTH"&&plan.continueParent(),"MageSlayer full-health rejection continues reduction");
            var bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS,Consumer=Java.loadClass("java.util.function.Consumer"),Priority=Java.loadClass("net.neoforged.bus.api.EventPriority");
            listener=new Consumer({accept:function(event){if(event.getEntity()===mage)event.setCanceled(true);}});
            bus.addListener(Priority.NORMAL,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingHealEvent"),listener);
            mage.setHealth(5);mageResult=magic();plan=mageResult.settlement().orElseThrow().evidence().beforeApplication();
            check(String(plan.steps().get(0).result().value().orElseThrow().outcome())==="CANCELLED"&&Math.abs(mage.getHealth()-2.4)<0.001,"MageSlayer cancelled child still keeps frozen damage reduction");
            bus.unregister(listener);listener=null;
            listener=new Consumer({accept:function(event){if(event.getEntity()===mage)event.setCanceled(true);}});
            bus.addListener(Priority.LOWEST,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent"),listener);
            mage.setHealth(5);mageResult=magic();
            check(Math.abs(mage.getHealth()-6.4)<0.001&&mageResult.settlement().orElseThrow().evidence().beforeApplication().steps().size()===1,"MageSlayer parent cancellation retains committed child without replay");
            bus.unregister(listener);listener=null;
            var archer=cow(),bow=new Stack(Items.BOW),GemItem=Java.loadClass("dev.shadowsoffire.apotheosis.socket.gem.GemItem");
            archer.getAttribute(Attr.CRIT_CHANCE).setBaseValue(0);
            var blood=Java.loadClass("dev.shadowsoffire.apotheosis.socket.gem.GemRegistry").INSTANCE.holder(Id.parse("apotheosis:the_nether/blood_lord")).get();
            Socket.setSockets(bow,1);bow=Socket.socketGemInItem(bow,GemItem.createStack(blood,Java.loadClass("dev.shadowsoffire.apotheosis.socket.gem.Purity").FLAWED));
            archer.setItemSlot(Slot.MAINHAND,bow);
            var arrow=Java.loadClass("net.minecraft.world.entity.EntityType").ARROW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);entities.push(arrow);arrow.setOwner(archer);
            var bloodInstance=Socket.getGems(bow).gems().get(0),beforeHealth=archer.getHealth();
            bloodInstance.onProjectileFired(archer,arrow);
            check(Math.abs(arrow.baseDamage-3.2)<0.001&&Math.abs(archer.getHealth()-(beforeHealth-archer.getMaxHealth()*0.2))<0.001,"BloodyArrow applies one native damage cost and one arrow multiplier");
            var launch=api.projectiles().snapshot(arrow).orElseThrow();
            var explanations=api.sourceDescriptions(launch.shooter());
            check(explanations.stream().anyMatch(function(e){return String(e.source())==="apotheosis:the_nether/blood_lord";}),"source descriptions use frozen gem slot and purity");
            archer.setItemSlot(Slot.MAINHAND,Stack.EMPTY);
            check(!launch.shooter().facts().equipment(Slot.MAINHAND).isEmpty(),"projectile launch equipment survives shooter swap");
            bloodInstance.onProjectileFired(archer,arrow);
            check(Math.abs(arrow.baseDamage-3.2)<0.001,"BloodyArrow cooldown survives unequip and prevents repeat");
            archer.getAttribute(Attr.CRIT_CHANCE).setBaseValue(7);
            var frozenFormula=api.damageFormula(new (cls("combat.api.bridge.DamageFormulaRequest"))(UUID.randomUUID(),Id.parse("apothic_attributes:multi_critical"),mage.getUUID(),Optional.of(archer.getUUID()),2,Optional.of(launch.action()),Optional.of(launch)));
            check(frozenFormula.output().multipliers().isEmpty(),"projectile critical calculation retains launch attributes after swap");
            archer.discard();
            for(var shotTarget of [mage,overVictim]) {
                var collision=new (Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent"))(shotTarget,
                    new (Java.loadClass("net.neoforged.neoforge.common.damagesource.DamageContainer"))(shotTarget.damageSources().arrow(arrow,archer),2));
                new (Java.loadClass("dev.shadowsoffire.apothic_attributes.impl.AttributeEvents"))().apothCriticalStrike(collision);
                check(collision.getAmount()===2,"projectile native critical retains frozen source after removal on target "+shotTarget.getUUID());
            }
            var affix=Java.loadClass("dev.shadowsoffire.apotheosis.affix.AffixRegistry").INSTANCE.holder(Id.parse("apotheosis:melee/executing"));
            var rarity=Java.loadClass("dev.shadowsoffire.apotheosis.loot.RarityRegistry").INSTANCE.holder(Id.parse("apotheosis:mythic"));
            var instance=new (Java.loadClass("dev.shadowsoffire.apotheosis.affix.AffixInstance"))(affix,1,rarity,new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").DIAMOND_SWORD));
            victim.setHealth(1);
            victim.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").OFFHAND,new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").TOTEM_OF_UNDYING));
            instance.doPostAttack(attacker,victim);
            check(victim.isAlive()&&victim.getHealth()===1&&victim.getOffhandItem().isEmpty(),"Executing routes to shared protection and consumes one totem");
            instance.doPostAttack(attacker,victim);
            check(!victim.isAlive(),"Executing kills after native protection consumed");
            var history=api.bridges().history(),seen={};
            for(var h=0;h<history.size();h++){var invocation=history.get(h);seen[String(invocation.capability())]=true;
                check(String(invocation.nativeContribution())==="SUPPRESSED","native invocation suppressed: "+invocation.capability());}
            check(seen["apotheosis:bloody_arrow"]&&seen["apotheosis:mageslayer"]&&seen["apotheosis:executing"]&&seen["apothic_attributes:overheal"]&&seen["apothic_attributes:multi_critical"],"replacement invocations expose per-call evidence");
        }
    } finally {if(listener!==null)Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS.unregister(listener);for(var i=0;i<entities.length;i++)entities[i].discard();}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
