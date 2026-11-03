// Public SDK scenarios; trusted lifecycle helpers below are used only to advance the disposable acceptance world.
(function () {
    function cls(n) { return Java.loadClass("io.github.naimjeg.combatnexus." + n); }
    var UUID=Java.loadClass("java.util.UUID"), Id=Java.loadClass("net.minecraft.resources.Identifier");
    var Optional=Java.loadClass("java.util.Optional"), OL=Java.loadClass("java.util.OptionalLong");
    var List=Java.loadClass("java.util.List"), Map=Java.loadClass("java.util.Map"), Set=Java.loadClass("java.util.Set");
    var level=AutoJS.server().overworld(), Access=cls("combat.spi.CombatWorldLifecycleAccess");
    var runtime=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level); Access.beginTick(runtime);
    var api=cls("combat.api.CombatAccess").find(level).orElseThrow(), Example=Java.loadClass("example.combatnexus.SdkExample");
    var checks=[], entities=[], listener=null;
    var ES=cls("effect.api.EffectService"), EI=cls("effect.api.EffectInstance"), BP=cls("combat.api.behavior.BehaviorPlan");
    var Request=cls("combat.healing.api.HealingRequest"), Source=cls("combat.api.CombatSource");
    function id(n){return Id.parse("combatnexus_sdk_example:"+n);}
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function near(a,b,label){check(Math.abs(a-b)<0.001,label);}
    function rejects(f,label){var failed=false;try{f();}catch(e){failed=true;}check(failed,label);}
    function cow(){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        e.setPos(0,100,0);e.setNoAi(true);e.setNoGravity(true);level.addFreshEntity(e);entities.push(e);return e;}
    function heal(target,owner,amount){return api.healing().submit(new Request(UUID.randomUUID(),target.getUUID(),new Source(id("heal"),owner===null?Optional.empty():Optional.of(owner.getUUID()),Optional.empty()),Request.Reason.SKILL,amount));}
    function effect(target,name,plan){return new EI(new (cls("effect.api.EffectSnapshot"))(target.getUUID(),id(name),id(name),0,OL.of(level.getGameTime()+600),Set.of()),List.of(),EI.Ownership.MANAGED,plan);}
    function apply(e){return api.effects().apply(new (cls("effect.api.EffectApplication"))(UUID.randomUUID(),e,ES.Stacking.REPLACE));}
    function advance(ticks){level.getLevelData().setGameTime(level.getGameTime()+ticks);Access.beginTick(runtime);Access.tickRecovery(runtime);}
    try {
        var caster=cow(),target=cow(),second=cow();target.setHealth(1);
        var Json=Java.loadClass("com.google.gson.JsonParser"), Behavior=cls("combat.api.behavior.BehaviorComponent");
        var optional=Behavior.decode(Json.parseString('{"type":"combatnexus_sdk_example:periodic_damage","amount":1}').getAsJsonObject());
        check(optional.intervalTicks()===20,"registered codec retains optional parameter defaults");
        rejects(function(){Behavior.decode(Json.parseString('{"type":"combatnexus_sdk_example:periodic_damage","amount":1,"typo":1}').getAsJsonObject());},"registered codec rejects unknown parameter");
        var tagged=cls("effect.api.EffectDefinition").decode(Json.parseString('{"level":0,"lifetime":{"kind":"TIMED","ticks":20},"stacking":"combatnexus:replace","category":"HARMFUL","dispellable":true,"attributes":[],"behaviors":[],"priority":0,"restrictions":[],"tags":["combatnexus_sdk_example:custom"]}').getAsJsonObject(),Map.of());
        check(tagged.policy().tags().contains(id("custom")),"data effect semantic tags compile into shared policy");
        var expired=new EI(new (cls("effect.api.EffectSnapshot"))(target.getUUID(),id("expired"),id("expired"),0,OL.of(level.getGameTime()),Set.of()),List.of(),EI.Ownership.MANAGED,BP.NONE);
        check(String(apply(expired).rejection().get().code())==="EXPIRED","expired effect is not reported as immunity");
        check(api.session().equals(cls("combat.api.CombatAccess").find(level).get().session()),"lookup retains existing session");
        rejects(function(){api.close();},"public world cannot close runtime");
        rejects(function(){api.shields().reserve(target.getUUID(),1,id("damage"),1);},"business shield API does not expose reservation");
        rejects(function(){api.healing().registerSettlementListener(id("late"),0,function(){});},"runtime listener registration removed");
        var zero=heal(target,null,0);check(String(zero.outcome())==="ZERO_INPUT"&&target.getHealth()===1,"injured zero input is not full health");
        var negative=effect(target,"zero_effect",new BP(0,List.of(new (cls("combat.api.behavior.BehaviorComponent$HealingAdditive"))(-10))));
        check(String(apply(negative).outcome())==="APPLIED","state effect command admitted");
        check(String(heal(target,null,1).outcome())==="ZERO_EFFECT","calculated zero is distinct from zero input");
        api.effects().dispel(UUID.randomUUID(),api.effects().snapshot(target.getUUID()).get(0));
        target.setHealth(target.getMaxHealth());check(String(heal(target,null,1).outcome())==="FULL_HEALTH","actual full health classified");
        target.setHealth(1);
        Example.equip(caster.getUUID(),"left");Example.equip(caster.getUUID(),"right");
        check(api.sources().refresh(UUID.randomUUID(),caster.getUUID()).applied(),"external provider reconciles once");
        check(api.sources().snapshot(caster.getUUID()).size()===2,"same trinket in two slots has independent source instances");
        var attributes=cls("attribute.api.CombatAttributes");
        near(caster.getAttributeValue(attributes.healingPower()),2,"two trinkets contribute outgoing healing");
        near(caster.getAttributeValue(attributes.meleeDamageAdditive()),4,"same source installations contribute damage");
        target.getAttribute(attributes.healingReceived()).setBaseValue(1.5);
        near(heal(target,caster,2).actualRecovery(),6,"healer and recipient snapshots multiply once");
        target.setHealth(1);near(heal(target,null,2).actualRecovery(),3,"unknown healer does not borrow target or another owner");
        Example.unequip(caster.getUUID(),"left");api.sources().refresh(UUID.randomUUID(),caster.getUUID());
        near(caster.getAttributeValue(attributes.healingPower()),1.5,"unequip revokes only its healing contribution");
        near(caster.getAttributeValue(attributes.meleeDamageAdditive()),2,"unequip revokes only its damage contribution");
        check(api.sources().snapshot(caster.getUUID()).size()===1,"other slot remains installed");
        var nativeEffect=new (Java.loadClass("net.minecraft.world.effect.MobEffectInstance"))(Java.loadClass("net.minecraft.world.effect.MobEffects").POISON,400,0);
        target.addEffect(nativeEffect);
        var dot=Example.dot(level,target.getUUID(),caster.getUUID(),"cast-one",1,20,true);
        var dot2=Example.dot(level,target.getUUID(),second.getUUID(),"cast-two",1,40,false);
        check(dot.applied()&&dot2.applied(),"custom periodic type installs for multiple casters without core branches");
        var views=api.effects().snapshot(target.getUUID());
        check(views.stream().anyMatch(function(e){return String(e.definition())==="minecraft:poison"&&String(e.ownership())==="VANILLA_OBSERVED";}),"unified view includes native effect");
        check(views.stream().filter(function(e){return String(e.definition())==="combatnexus_sdk_example:dot";}).count()===2,"unified view preserves both managed sources");
        check(views.stream().anyMatch(function(e){return e.owner().isPresent()&&e.owner().get().equals(caster.getUUID());}),"managed owner attribution retained");
        check(api.facts(target.getUUID()).get().facts().hasEffect(id("dot")),"shared captured facts query managed definitions");
        check(api.facts(target.getUUID()).get().facts().hasEffect(Id.parse("minecraft:poison")),"shared captured facts query native definitions");
        target.setHealth(1);near(heal(target,caster,2).actualRecovery(),2.25,"public healing processor consumes frozen managed semantic tag");
        var nativeView=views.stream().filter(function(e){return String(e.definition())==="minecraft:poison";}).findFirst().get();
        check(String(api.effects().dispel(UUID.randomUUID(),nativeView).outcome())==="REMOVED"&&!target.hasEffect(Java.loadClass("net.minecraft.world.effect.MobEffects").POISON),"native owner performs dispel without managed reinstallation");
        var observed=[], bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS;
        var regen=new (cls("combat.regeneration.api.RegenerationPlan"))(target.getUUID(),new Source(id("blocked"),Optional.empty(),Optional.empty()),
            new (cls("combat.regeneration.api.RegenerationDefinition"))(1,0,false,false,100),OL.of(level.getGameTime()+100));
        listener=new (Java.loadClass("java.util.function.Consumer"))({accept:function(event){
            if(!event.snapshot().target().getUUID().equals(target.getUUID()))return;
            observed.push(String(heal(target,caster,1).rejection().get().code()));
            observed.push(String(apply(effect(target,"blocked",BP.NONE)).rejection().get().code()));
            observed.push(String(api.shields().remove(UUID.randomUUID(),target.getUUID(),id("blocked")).rejection().get().code()));
            observed.push(String(api.regeneration().replace(UUID.randomUUID(),regen).rejection().get().code()));
            observed.push(String(api.sources().revoke(UUID.randomUUID(),dot.value().get()).rejection().get().code()));
            observed.push(String(Example.dot(level,target.getUUID(),caster.getUUID(),"blocked",1,20,false).rejection().get().code()));
        }});
        bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").NORMAL,false,cls("combat.damage.platform.api.event.DamageSettledEvent"),listener);
        target.setHealth(8);target.invulnerableTime=0;target.hurtServer(level,target.damageSources().generic(),1);
        check(observed.length===6&&observed.every(function(v){return v==="INVALID_PHASE";}),"real observation event rejects healing effects shields recovery source revoke and installation consistently");
        bus.unregister(listener);listener=null;
        check(api.regeneration().snapshot(target.getUUID()).isEmpty(),"observer cannot leave a recovery plan");
        check(api.sources().snapshot(target.getUUID()).size()===2,"observer cannot revoke or add sources");
        target.setHealth(8);target.invulnerableTime=0;caster.setHealth(1);
        advance(20);
        check(target.getHealth()<8,"registered periodic damage uses actual damage application");
        near(caster.getHealth(),2.5,"remaining trinket periodic heal uses its real owner snapshot");
        var saved=Access.save(runtime,target.getUUID());
        check(String(saved).indexOf('"version"')<0&&String(saved).indexOf('"schema"')<0&&String(saved).indexOf('combatnexus_sdk_example:periodic_damage')>=0&&String(saved).indexOf('"kind":"Periodic')<0,"storage contains current named behavior parameters only");
        check(api.sources().revoke(UUID.randomUUID(),dot2.value().get()).applied(),"source-specific revoke leaves another caster intact");
        check(api.sources().snapshot(target.getUUID()).size()===1,"other caster source remains");
        Access.clear(runtime,target.getUUID(),ES.Removal.WORLD_EXIT);Access.restore(runtime,target.getUUID(),saved);
        check(api.sources().snapshot(target.getUUID()).size()===1,"only persistent source and generic task restore");
        check(!api.sources().revoke(UUID.randomUUID(),dot.value().get()).applied(),"restored installation cannot reuse an old handle");
        target.setHealth(8);target.invulnerableTime=0;advance(20);check(target.getHealth()<8,"restored custom periodic task executes");
        var recovered=Example.restored(dot.value().get().instance()).orElseThrow();
        check(api.sources().revoke(UUID.randomUUID(),recovered).applied(),"registered owner receives restored revocation handle");
        check(api.sources().snapshot(target.getUUID()).isEmpty(),"restored owner revokes exactly its installation");
        var codec=cls("combat.internal.persistence.CombatStateCodec");
        rejects(function(){codec.decode(String(saved).replace('"savedTick":','"unknownField":'));},"malformed current storage rejected");
        rejects(function(){codec.decode(String(saved).replace(/combatnexus_sdk_example:periodic_damage/g,'missing:periodic_damage'));},"unknown stored behavior type rejected");
        var request=new Request(UUID.randomUUID(),target.getUUID(),new Source(id("duplicate"),Optional.empty(),Optional.empty()),Request.Reason.SKILL,0);
        api.healing().submit(request);check(String(api.healing().submit(request).rejection().get().code())==="DUPLICATE","duplicate healing returns structured rejection");
    } finally {
        if(listener!==null)Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS.unregister(listener);
        for(var i=0;i<entities.length;i++) {
            Example.unequip(entities[i].getUUID(),"left");Example.unequip(entities[i].getUUID(),"right");
            Access.clear(runtime,entities[i].getUUID(),ES.Removal.WORLD_EXIT);entities[i].discard();
        }
    }
    return JSON.stringify({passed:checks.length,checks:checks});
})()
