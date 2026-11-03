// Disposable server only. An isolated Nether runtime uses a failing contribution port; no reflection.
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    function cls(n) { return Java.loadClass("io.github.naimjeg.combatnexus." + n); }
    var Id=Java.loadClass("net.minecraft.resources.Identifier"), List=Java.loadClass("java.util.List");
    var level=AutoJS.server().getLevel(Java.loadClass("net.minecraft.world.level.Level").NETHER);
    var Lifecycle=cls("combat.platform.CombatWorldLifecycle"), Access=cls("combat.spi.CombatWorldLifecycleAccess");
    var world=null, equipment=null, Example=Java.loadClass("probe.EffectExtension");
    function effectEvents(){
        cls("combat.internal.transaction.CombatCompletionQueue").drainIfSafe();
        var events=[],all=Example.moments();
        for(var i=0;i<all.size();i++){var m=all.get(i);if(m instanceof cls("combat.api.action.CombatMoment$Effect")&&m.target().equals(entity.getUUID()))events.push(m);}
        return events;
    }
    var original=new (cls("attribute.internal.EffectAttributeContributions"))(level);
    var ES=cls("effect.api.EffectService"), checks=[], entity=null;
    function check(ok,n) { if(!ok)throw new Error(n); checks.push(n); }
    function rejects(f,n) { var failed=false;try{f();}catch(e){failed=true;}check(failed,n); }
    var Port=cls("effect.spi.EffectContributionPort"), Prepared=cls("effect.spi.EffectContributionPort$Prepared");
    var Receipt=cls("effect.spi.EffectContributionPort$Receipt"), Failure=Java.loadClass("java.lang.IllegalStateException");
    var failApply=true, failCompensation=false, immune=false, attempts=0;
    var port=new Port({prepare:function(change) {
        var nativePrepared=original.prepare(change), installation=!change.after().isEmpty();
        return new Prepared({apply:function() {
            var receipt=nativePrepared.apply();
            if(installation) { attempts++; if(failApply)return new Receipt(receipt.writes(),false,new Failure("ownership probe")); }
            return receipt;
        },reconcile:function() {
            if(failCompensation)return new Receipt(List.of(),false,new Failure("blocked compensation"));
            return nativePrepared.reconcile();
        }});
    }});
    Lifecycle.unload(new (Java.loadClass("net.neoforged.neoforge.event.level.LevelEvent$Unload"))(level));
    try {
        var Conditions=cls("combat.regeneration.spi.RegenerationConditions"), State=cls("combat.regeneration.spi.RegenerationConditions$State");
        world=new (cls("combat.internal.CombatRuntime"))(level,cls("combat.internal.platform.PlatformWorldLifecycle").healingApplication(new (cls("combat.platform.WorldHealingPort"))(level)),
            new Conditions({read:function(){return new State(true,true);}}),
            function(){return immune;},port,10,256);
        Access.beginTick(world);
        equipment=new (cls("combat.internal.coordination.EquipmentBehaviorController"))(world);
        entity=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        level.getChunk(0,0); entity.setPos(0,100,0); check(level.addFreshEntity(entity),"probe entity added");
        check(level.getEntity(entity.getUUID())!==null,"probe entity visible to world services");
        var target=entity.getUUID(), Slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot");
        var Stack=Java.loadClass("net.minecraft.world.item.ItemStack"), Bindings=cls("combat.api.behavior.EquipmentBindings");
        var item=new Stack(Java.loadClass("net.minecraft.world.item.Items").STICK);
        item.set(Bindings.component(),new Bindings(List.of(new (cls("combat.api.behavior.EquipmentBindings$Binding"))(
            Id.parse("combatnexus:ownership"),Bindings.Kind.EFFECT,Id.parse("combatnexus:agility"),List.of(Slot.MAINHAND)))));
        // The controller retries against the actual slot, rather than the failed event's stale stack.
        entity.setItemSlot(Slot.MAINHAND,item);

        equipment.changed(target,Slot.MAINHAND,item);
        check(world.effects().snapshot(target).isEmpty(),"compensated failure never commits active effect");
        check(effectEvents().length===0,"compensated failed commit publishes no public Effect event");
        var generation=cls("combat.internal.definition.CombatDefinitions").forServer(AutoJS.server()).generation();
        failApply=false; equipment.tick(generation);
        check(world.effects().snapshot(target).size()===1&&attempts===2,"unchanged equipment retries after compensation");
        check(effectEvents().length===1,"successful retry publishes one public Effect event");
        var grantOrigin=world.effects().snapshot(target).get(0).display().provenance();
        check(grantOrigin.entity().equals(target)&&String(grantOrigin.item())==="minecraft:stick"&&grantOrigin.slot()===Slot.MAINHAND,"equipment grant records actual holder item and slot independently of stacking key");
        var source=world.effects().snapshot(target).get(0).snapshot().source();
        world.effects().remove(target,Id.parse("combatnexus:agility"),source,ES.Removal.DISPELLED);
        equipment.changed(target,Slot.MAINHAND,item.copy()); equipment.tick(generation);
        check(world.effects().snapshot(target).isEmpty()&&attempts===2,"successful grant is not replenished after dispel");
        check(effectEvents().length===2&&String(effectEvents()[1].managedChange().get().removal().get())==="DISPELLED","direct service removal publishes one committed reason");
        equipment.changed(target,Slot.MAINHAND,Stack.EMPTY);
        failApply=true; failCompensation=true;
        equipment.changed(target,Slot.MAINHAND,item);
        check(world.sourceInstallations().snapshot(target).stream().anyMatch(function(s){return s.pending();}),"partial installation remains tracked");
        var before=attempts; equipment.tick(generation);
        check(attempts===before&&!world.effects().pendingSources(target).isEmpty(),"pending compensation blocks another installation");
        check(effectEvents().length===2,"failed compensation publishes no committed event");
        entity.setItemSlot(Slot.MAINHAND,Stack.EMPTY);
        failApply=false; failCompensation=false; equipment.tick(generation);
        check(world.effects().pendingSources(target).isEmpty()&&world.effects().snapshot(target).isEmpty(),"unequip reconciles attempted ownership without reinstalling stale equipment");

        immune=true; entity.setItemSlot(Slot.MAINHAND,item);
        equipment.changed(target,Slot.MAINHAND,item);
        check(world.effects().snapshot(target).isEmpty(),"false apply result is not recorded as successful installation");
        immune=false; equipment.tick(generation);
        check(world.effects().snapshot(target).size()===1,"rejected grant retries when immunity ends");

        rejects(function(){world.regeneration().recordDamage(target,Java.loadClass("java.lang.Long").MAX_VALUE);},"business view cannot inject future damage time");
        rejects(function(){cls("combat.spi.CombatBehaviorAccess").recordRecoveryDamage(world,null);},"recovery SPI requires a live settlement callback");
        var panels=new (cls("presentation.internal.DefaultCombatPanelService"))(10,256), view=panels.businessView();
        var now=level.getGameTime(), snapshot=world.captureAttributes(entity,List.of());
        var capture=function(){return world.panel(entity,snapshot);};
        check(view.request(target,target,now,capture).snapshot().isPresent(),"panel business facade delivers snapshot");
        check(String(view.request(target,target,now,capture).refusal())==="RATE_LIMITED","same-tick panel request stays limited");
        rejects(function(){view.reset();},"business facade cannot reset panel throttle");
        rejects(function(){view.close();},"business facade cannot close panel session");
        panels.clear(target);
        check(view.request(target,target,now,capture).snapshot().isPresent(),"owner lifecycle clears panel throttle");
        panels.close();
        rejects(function(){view.request(target,target,now,capture);},"retained facade rejects after owner closes");
    } finally {
        failApply=false; failCompensation=false;
        try {
            if(entity!==null) { Access.clear(world,entity.getUUID(),ES.Removal.WORLD_EXIT);entity.discard(); }
            if(equipment!==null)equipment.close();
            if(world!==null)Access.close(world);
        } finally {Lifecycle.load(new (Java.loadClass("net.neoforged.neoforge.event.level.LevelEvent$Load"))(level));}
    }
    return JSON.stringify({passed:checks.length,checks:checks});
})()
