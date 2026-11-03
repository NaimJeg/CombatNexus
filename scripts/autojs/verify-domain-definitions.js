// Isolated server acceptance: definitions, typed components, explicit installation and cleanup.
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    function cls(n) { return Java.loadClass("io.github.naimjeg.combatnexus." + n); }
    var Id=Java.loadClass("net.minecraft.resources.Identifier"), List=Java.loadClass("java.util.List");
    var Map=Java.loadClass("java.util.Map"), Optional=Java.loadClass("java.util.Optional"), OL=Java.loadClass("java.util.OptionalLong");
    var UUID=Java.loadClass("java.util.UUID"), checks=[], entities=[];
    var level=AutoJS.server().overworld(), world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    var Definitions=cls("combat.internal.definition.CombatDefinitions"), Generation=cls("combat.internal.definition.DefinitionGeneration");
    var original=Definitions.forServer(AutoJS.server()), ES=cls("effect.api.EffectService");
    function id(n) { return Id.parse("combatnexus:"+n); }
    function check(ok,n) { if(!ok)throw new Error(n); checks.push(n); }
    function reject(f,n) { var failed=false;try{f();}catch(e){failed=true;}check(failed,n); }
    function json(v) { return cls("config.SettingsJson").read(new (Java.loadClass("java.io.StringReader"))(typeof v==="string"?v:JSON.stringify(v))); }
    function cow() { var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);e.setPos(0,100,0);level.addFreshEntity(e);entities.push(e);return e; }
    function source(n) { return new (cls("combat.api.CombatSource"))(id(n),Optional.empty(),Optional.empty()); }
    try {
        var domains=world.domainDefinitions();
        check(domains.shields().containsKey(id("physical_guard"))&&domains.effects().containsKey(id("agility"))&&domains.regeneration().containsKey(id("field_recovery")),"all domain samples adopted");
        reject(function(){json('{"a":1,"a":2}');},"duplicate keys rejected");
        reject(function(){json('{"a":NaN}');},"nonfinite JSON rejected");
        reject(function(){json('{"a":1} {}');},"trailing JSON rejected");
        var BP=cls("combat.api.behavior.BehaviorPlan"), Protect=cls("combat.api.behavior.BehaviorComponent$LethalProtection");
        reject(function(){new BP(0,List.of(new Protect(),new Protect()));},"duplicate behavior type rejected");
        var BD=cls("combat.api.behavior.BehaviorDefinition");
        reject(function(){BD.decode(json({priority:0,duration_ticks:20,components:[{type:"unknown"}]}));},"unknown component rejected");
        reject(function(){BD.decode(json({priority:0,duration_ticks:20,components:[{type:"combatnexus:lethal_protection",typo:1}]}));},"unknown component field rejected");
        var a=cow(), b=cow();a.setHealth(1);b.setHealth(1);
        check(world.effects().snapshot(a.getUUID()).isEmpty(),"loaded effects are not auto granted");
        var Request=cls("combat.healing.api.HealingRequest");
        var heal=function(e,n){return world.healing().submit(new Request(UUID.randomUUID(),e.getUUID(),source(n),Request.Reason.EFFECT,1));};
        check(heal(a,"example_healing").actualRecovery()===2,"healing rule matches source and frozen health");
        check(heal(b,"unmatched").actualRecovery()===1,"unmatched source retains base healing");
        a.setHealth(a.getMaxHealth()-2);check(heal(a,"example_healing").actualRecovery()===1,"health predicate boundary excludes healthy target");
        var rule={priority:0,phase:"RECEIVED",reasons:[],sources:[],minimum_health_ratio:0,maximum_health_ratio:1,additive:0,multiplier:1};
        reject(function(){cls("combat.healing.api.HealingRuleDefinition").decode(json(rule));},"rules cannot replace received stage");
        var received=b.getAttribute(cls("attribute.api.CombatAttributes").healingReceived()), receivedBase=received.getBaseValue();
        try {
            received.setBaseValue(2);
            rule.phase="CONTRIBUTIONS";rule.additive=1;rule.multiplier=2;
            var firstRule=cls("combat.healing.api.HealingRuleDefinition").decode(json(rule));
            rule.phase="LIMITS";rule.additive=-2;rule.multiplier=0.5;
            var lastRule=cls("combat.healing.api.HealingRuleDefinition").decode(json(rule));
            var request=new Request(UUID.randomUUID(),b.getUUID(),source("staged"),Request.Reason.EFFECT,2);
            var input=world.captureInput(b,original.generation()), before=b.getHealth();
            var calculation=cls("combat.healing.internal.HealingPipeline").calculate(request,new (cls("combat.api.input.ParticipantInputs"))(Map.of(cls("combat.api.input.ParticipantInputs$Role").TARGET,input)),Map.of(id("first"),firstRule,id("last"),lastRule));
            check(calculation.modifiedAmount()===5,"domain phases surround exactly one received multiplier");
            check(b.getHealth()===before,"rule calculation never writes health");
        } finally {received.setBaseValue(receivedBase);}
        var speed=Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").MOVEMENT_SPEED;
        var baseline=a.getAttributeValue(speed);
        check(cls("effect.internal.EffectDefinitions").install(world,a.getUUID(),id("agility"),id("probe")),"effect definition installs");
        check(Math.abs(a.getAttributeValue(speed)-baseline*1.1)<0.000001,"managed effect uses native speed attribute");
        world.effects().remove(a.getUUID(),id("agility"),id("probe"),ES.Removal.DISPELLED);
        check(Math.abs(a.getAttributeValue(speed)-baseline)<0.000001,"dispel revokes attribute contribution");
        var effect={level:0,lifetime:{kind:"TIMED",ticks:20},stacking:"combatnexus:replace",category:"BENEFICIAL",dispellable:false,attributes:[],behaviors:[],priority:0,restrictions:[]};
        var ED=cls("effect.api.EffectDefinition"), protectedEffect=ED.decode(json(effect),original.behaviors());
        check(protectedEffect.behavior().components().isEmpty(),"empty behavior reference array is legal");
        var ordered=JSON.parse(JSON.stringify(effect));ordered.lifetime={kind:"INSTANT"};
        var healing=BD.decode(json({priority:0,duration_ticks:20,components:[{type:"combatnexus:instant_healing",amount:4}]}));
        var damage=BD.decode(json({priority:0,duration_ticks:20,components:[{type:"combatnexus:instant_damage",amount:3,damage_type:"minecraft:generic"}]}));
        var refs=Map.of(id("order_heal"),healing,id("order_damage"),damage,id("order_alias"),healing);
        ordered.behaviors=["combatnexus:order_heal","combatnexus:order_damage"];
        var forward=ED.decode(json(ordered),refs);
        check(String(forward.behavior().components().get(0).type())==="combatnexus:instant_healing"&&String(forward.behavior().components().get(1).type())==="combatnexus:instant_damage","behavior array expands in JSON order");
        ordered.behaviors.reverse();var backward=ED.decode(json(ordered),refs);
        check(String(backward.behavior().components().get(0).type())==="combatnexus:instant_damage"&&String(backward.behavior().components().get(1).type())==="combatnexus:instant_healing","reversed references reverse component order");
        var forwardTarget=cow(),backwardTarget=cow();
        forwardTarget.setHealth(forwardTarget.getMaxHealth()-1);backwardTarget.setHealth(backwardTarget.getMaxHealth()-1);
        world.effects().apply(forward.instantiate(forwardTarget.getUUID(),id("order"),id("order"),level.getGameTime()),ES.Stacking.REPLACE);
        world.effects().apply(backward.instantiate(backwardTarget.getUUID(),id("order"),id("order"),level.getGameTime()),ES.Stacking.REPLACE);
        check(forwardTarget.getHealth()===forwardTarget.getMaxHealth()-3,"heal then damage executes in array order with capped healing");
        check(backwardTarget.getHealth()===backwardTarget.getMaxHealth(),"damage then heal executes in reversed array order");
        ordered.behaviors=["combatnexus:order_heal","combatnexus:order_heal"];
        reject(function(){ED.decode(json(ordered),refs);},"duplicate behavior reference rejected");
        ordered.behaviors=["combatnexus:order_heal","combatnexus:order_alias"];
        reject(function(){ED.decode(json(ordered),refs);},"duplicate expanded component type rejected");
        world.effects().apply(protectedEffect.instantiate(a.getUUID(),id("protected"),id("probe"),level.getGameTime()),ES.Stacking.REPLACE);
        reject(function(){world.effects().remove(a.getUUID(),id("protected"),id("probe"),ES.Removal.DISPELLED);},"nondispellable removal explicitly rejected");
        check(world.effects().snapshot(a.getUUID()).size()===1,"nondispellable effect remains installed");
        world.effects().remove(a.getUUID(),id("protected"),id("probe"),ES.Removal.REMOVED);
        check(world.effects().snapshot(a.getUUID()).isEmpty(),"owner removal still revokes nondispellable effect");
        effect.restrictions=["ATTACK"];check(ED.decode(json(effect),original.behaviors()).restrictions().size()===1,"action restriction decoded");
        effect.restrictions=[];effect.behaviors=["combatnexus:missing"];reject(function(){ED.decode(json(effect),original.behaviors());},"missing behavior reference rejected");
        cls("combat.shield.internal.ShieldDefinitions").grant(world,a.getUUID(),id("physical_guard"),source("guard"),10,100,null);
        var shield=world.shields().snapshot(a.getUUID()).get(0);
        check(shield.balance()===10&&shield.definition().capacity()===20&&!shield.eligibleDamageTypes().isEmpty(),"shield definition resolved into immutable installed filter");
        reject(function(){cls("combat.shield.internal.ShieldDefinitions").grant(world,b.getUUID(),id("physical_guard"),source("invalid"),21,100,null);},"balance above capacity rejected");
        reject(function(){cls("combat.shield.internal.ShieldDefinitions").grant(world,UUID.randomUUID(),id("physical_guard"),source("invalid"),1,100,null);},"missing shield target rejected");
        var Bindings=cls("combat.api.behavior.EquipmentBindings"), Binding=cls("combat.api.behavior.EquipmentBindings$Binding");
        var Slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot"), Stack=Java.loadClass("net.minecraft.world.item.ItemStack");
        var item=new Stack(Java.loadClass("net.minecraft.world.item.Items").STICK);
        var first=new Binding(id("first"),Bindings.Kind.BEHAVIOR,id("last_stand"),List.of(Slot.MAINHAND));
        var second=new Binding(id("second"),Bindings.Kind.EFFECT,id("agility"),List.of(Slot.MAINHAND));
        reject(function(){new Bindings(List.of(first,first));},"duplicate equipment identity rejected");
        item.set(Bindings.component(),new Bindings(List.of(first,second)));
        var Ops=Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE;
        var encoded=Bindings.CODEC.encodeStart(Ops,item.get(Bindings.component())).getOrThrow();
        check(Bindings.CODEC.parse(Ops,encoded).getOrThrow().equals(item.get(Bindings.component())),"equipment codec round trip");
        encoded.getAsJsonArray().get(0).getAsJsonObject().addProperty("typo",1);
        check(Bindings.CODEC.parse(Ops,encoded).error().isPresent(),"equipment codec rejects unknown binding field");
        var Equipment=cls("combat.spi.CombatEquipmentAccess");
        Equipment.changed(world,b.getUUID(),Slot.OFFHAND,item);
        check(world.effects().snapshot(b.getUUID()).isEmpty(),"equipment slot filter enforced");
        Equipment.changed(world,b.getUUID(),Slot.MAINHAND,item);
        check(world.effects().snapshot(b.getUUID()).size()===2,"multiple equipment bindings installed independently");
        world.effects().remove(b.getUUID(),id("agility"),world.effects().snapshot(b.getUUID()).stream().filter(function(e){return String(e.snapshot().effect())==="combatnexus:agility";}).findFirst().get().snapshot().source(),ES.Removal.DISPELLED);
        Equipment.changed(world,b.getUUID(),Slot.MAINHAND,item.copy());
        check(world.effects().snapshot(b.getUUID()).size()===1,"unchanged equipment does not replenish dispelled grant");
        Equipment.changed(world,b.getUUID(),Slot.MAINHAND,Stack.EMPTY);
        check(world.effects().snapshot(b.getUUID()).isEmpty(),"unequip revokes all owned sources");
        var ref=new (cls("combat.api.CombatOperationRef"))(1,cls("combat.api.transaction.CombatOperation").HEALING);
        check(ref.operation()===cls("combat.api.transaction.CombatOperation").HEALING,"causality retains neutral operation type");
    } finally {
        for(var i=0;i<entities.length;i++){cls("combat.spi.CombatWorldLifecycleAccess").clear(world,entities[i].getUUID(),ES.Removal.WORLD_EXIT);entities[i].discard();}
    }
    return JSON.stringify({passed:checks.length,checks:checks});
})()
