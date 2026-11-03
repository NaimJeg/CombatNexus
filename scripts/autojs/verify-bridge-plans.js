(function () {
    function cls(n) { return Java.loadClass("io.github.naimjeg.combatnexus." + n); }
    var level = AutoJS.server().overworld(), checks = [], entities = [], listener = null;
    var Example = Java.loadClass("example.combatnexus.PlanExample");
    var api = cls("combat.api.CombatAccess").find(level).orElseThrow();
    function check(ok, label) { if (!ok) throw new Error(label); checks.push(label); }
    function near(a, b, label) { check(Math.abs(a-b) < 0.001, label); }
    function cow() {
        var e = Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level, Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        e.setPos(0,512,0); e.setNoAi(true); e.setNoGravity(true); level.addFreshEntity(e); entities.push(e); return e;
    }
    function hit(v, a) {
        v.invulnerableTime=0;
        return api.damage(cls("combat.damage.api.damage.DamageRequest").builder(level,v,
            cls("combat.damage.api.damage.DamageSourceDescriptor").from(v.damageSources().mobAttack(a)),2).logicalAttacker(a).directEntity(a).build());
    }
    function before(r) { return r.settlement().orElseThrow().evidence().beforeApplication(); }
    try {
        var target=cow(), attacker=cow(); target.addTag("plan_heal"); target.setHealth(1);
        if(Java.loadClass("net.neoforged.fml.ModList").get().isLoaded("apothic_attributes"))
            attacker.getAttribute(Java.loadClass("dev.shadowsoffire.apothic_attributes.api.ALObjects$Attributes").CRIT_CHANCE).setBaseValue(0);
        var captured=api.facts(target.getUUID()).orElseThrow().facts().external();
        target.addTag("after_capture");
        check(!captured.find(Example.TAGS).orElseThrow().contains("after_capture"),"external facts detached from live entity");
        target.addTag("capture_write"); target.addTag("capture_fail");
        var restricted=api.facts(target.getUUID()).orElseThrow().facts().external();
        check(restricted.find(Example.TAGS).orElseThrow().contains("capture_rejection:INVALID_PHASE") && target.getHealth()===1,"capture callbacks cannot submit commands");
        check(restricted.incomplete().contains(Example.CAPTURE_FAILURE.id()) && restricted.find(Example.CAPTURE_FAILURE).isEmpty(),"failed collector reports missing frozen fact");
        target.removeTag("capture_write"); target.removeTag("capture_fail");
        var first=hit(target,attacker);
        near(target.getHealth(),3,"child heals before lethal parent damage");
        check(before(first).steps().size()===1 && before(first).completed(),"one child receipt retained");
        near(before(first).steps().get(0).result().value().orElseThrow().actualRecovery(),4,"child actual recovery evidence");
        var logs=Java.loadClass("example.combatnexus.SdkExample").healingLog();
        var log=logs.get(logs.size()-1);
        check(log.lineage().parentId().getAsLong()===first.settlement().get().lineage().damageId(),"child inherits parent operation");
        target.setHealth(target.getMaxHealth());
        var full=hit(target,attacker);
        check(String(before(full).steps().get(0).result().value().orElseThrow().outcome())==="FULL_HEALTH" && before(full).continueParent(),"full child continues parent");
        target.addTag("plan_abort"); target.setHealth(target.getMaxHealth());
        var aborted=hit(target,attacker);
        near(target.getHealth(),target.getMaxHealth(),"ordinary child rejection can abort parent");
        check(!before(aborted).continueParent() && before(aborted).completed(),"ordinary rejection distinct from uncertain write");
        target.removeTag("plan_abort"); target.addTag("plan_throw"); target.setHealth(6);
        var failed=hit(target,attacker);
        near(target.getHealth(),4,"failed calculation discards queued heal");
        check(before(failed).steps().isEmpty(),"callback rollback includes operation plan");
        target.removeTag("plan_throw");
        var bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS;
        var Consumer=Java.loadClass("java.util.function.Consumer"), Priority=Java.loadClass("net.neoforged.bus.api.EventPriority");
        listener=new Consumer({accept:function(event){if(event.getEntity()===target)event.setCanceled(true);}});
        bus.addListener(Priority.NORMAL,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingHealEvent"),listener);
        target.setHealth(6); var cancelled=hit(target,attacker);
        near(target.getHealth(),4,"cancelled child does not cancel continue-policy parent");
        check(String(before(cancelled).steps().get(0).result().value().orElseThrow().outcome())==="CANCELLED","child cancellation retained");
        bus.unregister(listener); listener=null;
        listener=new Consumer({accept:function(event){if(event.getEntity()===target)event.setCanceled(true);}});
        bus.addListener(Priority.LOWEST,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent"),listener);
        target.setHealth(1); var late=hit(target,attacker);
        near(target.getHealth(),5,"later parent cancellation preserves successful child");
        check(before(late).steps().size()===1,"cancelled parent retains child receipt");
        bus.unregister(listener); listener=null;
        var calls=0;
        listener=new Consumer({accept:function(event){if(event.getEntity()===target){calls++;target.setHealth(target.getHealth()+1);throw new (Java.loadClass("java.lang.IllegalStateException"))("injected event failure after write");}}});
        bus.addListener(Priority.NORMAL,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingHealEvent"),listener);
        target.addTag("plan_twice"); target.setHealth(1); var uncertain=hit(target,attacker);
        near(target.getHealth(),2,"uncertain child preserves write and prevents parent damage");
        check(calls===1 && before(uncertain).steps().size()===1 && !before(uncertain).completed(),"uncertain write stops remaining steps without retry");
        bus.unregister(listener); listener=null; target.removeTag("plan_twice");
        var healingTarget=cow();healingTarget.setHealth(1);healingTarget.addTag("healing_plan");
        var UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional"),Id=Java.loadClass("net.minecraft.resources.Identifier");
        var plannedHeal=function(){return api.healing().submit(new (cls("combat.healing.api.HealingRequest"))(UUID.randomUUID(),healingTarget.getUUID(),new (cls("combat.api.CombatSource"))(Id.parse("combatnexus_sdk_example:healing_plan"),Optional.empty(),Optional.empty()),cls("combat.healing.api.HealingRequest$Reason").SKILL,2));};
        var healed=plannedHeal();
        near(healingTarget.getHealth(),3,"healing parent applies frozen calculation after child absorption");
        near(healingTarget.getAbsorptionAmount(),2,"healing calculation submits typed before-application command");
        check(healed.beforeApplication().steps().get(0).context().get().identity().get().parent().getAsLong()===healed.context().get().identity().get().operation(),"healing plan child shares admitted parent identity");
        healingTarget.addTag("healing_plan_throw");var rollback=plannedHeal();
        check(rollback.beforeApplication().steps().isEmpty()&&healingTarget.getAbsorptionAmount()===2,"failed healing calculation discards its plan");
        var expired=false; try { Example.expiredRegistrar().facts(Example.TAGS,function(e){return Java.loadClass("java.util.List").of();}); } catch(e) { expired=true; }
        check(expired,"retained registrar expires");
        var mods=Java.loadClass("net.neoforged.fml.ModList").get();
        check(mods.isLoaded("combatnexus_compat"),"nested compat mod loaded");
        check(mods.getMods().stream().filter(function(m){return String(m.getModId())==="combatnexus";}).count()===1
            && mods.getMods().stream().filter(function(m){return String(m.getModId())==="combatnexus_compat";}).count()===1,"core and compat each load exactly once");
        if(typeof COMBATNEXUS_PACKAGED!=="undefined" && COMBATNEXUS_PACKAGED)
            check(String(mods.getModFileById("combatnexus").getFile().getFilePath()).endsWith(".jar"),"core loaded from final Jar");
        var coverage=cls("combat.api.CombatAccess").bridgeCoverage();
        if (mods.isLoaded("apotheosis")) {
            check(coverage.size()===6,"six capabilities report actual coverage");
            var player=new (Java.loadClass("net.neoforged.neoforge.common.util.FakePlayer"))(level,
                new (Java.loadClass("com.mojang.authlib.GameProfile"))(Java.loadClass("java.util.UUID").randomUUID(),"CNAttributeProbe"));
            var attribute=Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").MOVEMENT_SPEED;
            var modifier=Java.loadClass("net.minecraft.world.entity.ai.attributes.AttributeModifier");
            var Id=Java.loadClass("net.minecraft.resources.Identifier"), Set=Java.loadClass("java.util.Set"), List=Java.loadClass("java.util.List");
            player.getAttribute(attribute).addTransientModifier(new modifier(Id.parse("combatnexus_sdk_example:toggle"),1,modifier.Operation.ADD_VALUE));
            var attachment=Java.loadClass("dev.shadowsoffire.apotheosis.Apoth$Attachments").ATTRIBUTE_TOGGLES;
            player.setData(attachment,player.getData(attachment).withSuppressed(Set.of(attribute),true));
            var frame=cls("attribute.api.AttributeReader").capture(player,1,List.of(attribute)).valueFrame().values().get(Id.parse("minecraft:movement_speed"));
            check(frame.raw()>frame.effective(),"disabled attribute preserves raw and native effective values");
            near(frame.effective(),player.getAttributeValue(attribute),"snapshot honors native toggle");
            check(String(frame.effectiveReason())==="apotheosis:attribute_toggle","toggle explanation captured with value");
        } else check(coverage.size()===(mods.isLoaded("apothic_attributes")?2:0),"absent optional mods declare no active bridges");
    } finally { if(listener!==null)Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS.unregister(listener); for(var i=0;i<entities.length;i++) entities[i].discard(); }
    return JSON.stringify({passed:checks.length,checks:checks});
})()
