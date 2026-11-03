(function(){
    function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
    if(!Java.loadClass("net.neoforged.fml.ModList").get().isLoaded("combatnexus_effect_probe"))throw new Error("Start with effect-extension.init.gradle");
    var level=AutoJS.server().overworld(), world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    cls("combat.spi.CombatWorldLifecycleAccess").beginTick(world);
    var api=cls("combat.api.CombatAccess").find(level).orElseThrow(),Id=Java.loadClass("net.minecraft.resources.Identifier"),UUID=Java.loadClass("java.util.UUID");
    var List=Java.loadClass("java.util.List"),Set=Java.loadClass("java.util.Set"),Map=Java.loadClass("java.util.Map");
    var cow=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    cow.setPos(0,512,0);cow.setNoGravity(true);cow.setNoAi(true);level.addFreshEntity(cow);
    var checks=[];function check(v,l){if(!v)throw new Error(l);checks.push(l);}
    try{
        var id=Id.parse("combatnexus_effect_probe:condition"),stack=cls("effect.api.EffectService$Stacking").REPLACE;
        var definition=new (cls("effect.api.EffectDefinition"))(0,cls("effect.api.EffectLifetime").permanent(),stack,
            new (cls("effect.api.EffectPolicy"))(cls("effect.api.EffectPolicy$Category").BENEFICIAL,false),List.of(),
            new (cls("combat.api.behavior.BehaviorPlan"))(0,List.of(new (Java.loadClass("probe.EffectExtension$Pulse"))(1,20))),Set.of());
        check(Java.loadClass("probe.EffectExtension").rejectedDescriptorMismatch,"descriptor type mismatch is rejected during registration");
        var described=cls("presentation.api.DescriptionDocument").leaf(cls("effect.api.EffectTooltip").describe(id,definition));
        var wire=cls("presentation.api.DescriptionDocument").CODEC.encodeStart(Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE,described).getOrThrow().toString();
        check(String(wire).indexOf("effect.periodic")>=0 && String(wire).indexOf("unknown_behavior")<0,"custom behavior descriptor projects server parameters without client registration");
        var active=cls("presentation.api.DescriptionDocument").leaf(cls("effect.api.EffectTooltip").nativeInstance(id,definition));
        check(active.generated().equals(described.generated().subList(0,described.generated().size()-1)),"custom item and active explanation share the registered behavior descriptor");
        var unknown=cls("effect.internal.EffectBehaviorDescriptors").describe(new (Java.loadClass("probe.EffectExtension$Mystery"))(),0);
        check(String(unknown.summary().getFirst().getContents()).indexOf("unknown_behavior")>=0,"registered behavior without descriptor is explicitly unknown");
        var frozen=false;try{cls("combat.damage.api.display.DamageDescriptions").registration().registerSchema(new (cls("presentation.api.phrase.RulePhraseSchema"))(cls("presentation.api.phrase.RulePhraseType").of("probe","late"),Set.of(cls("presentation.api.phrase.PhraseVariant").DEFAULT),List.of()));}catch(e){frozen=true;}
        check(frozen,"description registration is frozen after startup");
        var effect=definition.instantiate(cow.getUUID(),id,id,level.getGameTime()),Fact=cls("effect.api.EffectFact");
        var result=api.effects().fact(UUID.randomUUID(),new Fact(effect,id,Map.of("active",1.0),stack.id()));
        check(String(result.outcome())==="APPLIED","registered condition installs custom permanent effect through public API");
        var installed=world.behaviors().capture(cow.getUUID()).get(0).installation();
        api.effects().fact(UUID.randomUUID(),new Fact(effect,id,Map.of("active",1.0),stack.id()));
        check(world.behaviors().capture(cow.getUUID()).get(0).installation()===installed,"duplicate custom facts preserve installation and phase");
        var holder=Java.loadClass("net.minecraft.core.registries.BuiltInRegistries").MOB_EFFECT.get(id).orElseThrow();
        check(cow.hasEffect(holder)&&cow.getEffect(holder).isInfiniteDuration(),"custom display mapping uses native effect projection without a dedicated mixin");
        api.effects().clear(UUID.randomUUID(),cow.getUUID());check(cow.hasEffect(holder),"generic clearing respects the definition removal policy");
        var encoded=cls("combat.internal.registration.BehaviorTypes").encode(new (Java.loadClass("probe.EffectExtension$Pulse"))(1,20));
        check(cls("combat.internal.registration.BehaviorTypes").decode(encoded).intervalTicks()===20,"registered custom periodic behavior survives codec round-trip");
        var removed=api.effects().fact(UUID.randomUUID(),new Fact(effect,id,Map.of("active",0.0),stack.id()));
        check(String(removed.outcome())==="REMOVED"&&!cow.hasEffect(holder),"condition end removes custom projection and behavior");
        check(world.behaviors().capture(cow.getUUID()).isEmpty(),"custom condition removal cancels the periodic source");
        var Probe=Java.loadClass("probe.EffectExtension");Probe.resetTriggers();
        var eventId=Id.parse("combatnexus_effect_probe:events");
        var events=new (cls("effect.api.EffectDefinition"))(0,cls("effect.api.EffectLifetime").permanent(),stack,cls("effect.api.EffectPolicy").DEFAULT,List.of(),
            new (cls("combat.api.behavior.BehaviorPlan"))(0,List.of(new (cls("effect.api.EffectCapabilityBehavior"))(id))),Set.of());
        api.effects().apply(new (cls("effect.api.EffectApplication"))(UUID.randomUUID(),events.instantiate(cow.getUUID(),eventId,eventId,level.getGameTime()),stack));
        check(Probe.triggers("APPLY")===1,"registered capability receives generic application trigger");
        cow.invulnerableTime=0;cow.hurtServer(level,cow.damageSources().generic(),1);
        check(Probe.triggers("HURT")===1,"unprojected custom effect receives one hurt trigger");
        api.effects().remove(UUID.randomUUID(),cow.getUUID(),eventId,eventId,cls("effect.api.EffectService$Removal").REMOVED);
        check(Probe.triggers("END")===1,"registered capability receives generic end trigger");
        return JSON.stringify({passed:checks.length,checks:checks});
    }finally{cow.discard();}
})()
