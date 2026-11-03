// Run on the isolated NeoForge server through scripts/run-autojs.py.
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var prefix="io.github.naimjeg.combatnexus.";
    function cls(n){return Java.loadClass(prefix+n);}
    var Id=Java.loadClass("net.minecraft.resources.Identifier"), UUID=Java.loadClass("java.util.UUID");
    var Optional=Java.loadClass("java.util.Optional"), OL=Java.loadClass("java.util.OptionalLong");
    var List=Java.loadClass("java.util.List"), Set=Java.loadClass("java.util.Set");
    var level=AutoJS.server().overworld(), world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    var ES=cls("effect.api.EffectService"), EI=cls("effect.api.EffectInstance"), BP=cls("combat.api.behavior.BehaviorPlan");
    var checks=[], entities=[];
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function rejects(f){try{f();return false;}catch(e){return true;}}
    function cow(){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        e.setPos(0,100,0);level.addFreshEntity(e);entities.push(e);return e;}
    function effect(e,name,plan){return new EI(new (cls("effect.api.EffectSnapshot"))(e.getUUID(),Id.parse("combatnexus:"+name),Id.parse("combatnexus:"+name),0,OL.of(level.getGameTime()+200),Set.of()),List.of(),EI.Ownership.MANAGED,plan);}
    function install(e,name,plan){var v=effect(e,name,plan);check(world.effects().apply(v,ES.Stacking.REPLACE),"install "+name);return v;}
    function hit(v,a,amount){v.invulnerableTime=0;return cls("combat.damage.api.DamageModuleApi").submitDamage(cls("combat.damage.api.damage.DamageRequest").builder(level,v,
        cls("combat.damage.api.damage.DamageSourceDescriptor").from(v.damageSources().mobAttack(a)),amount).logicalAttacker(a).directEntity(a).build());}
    function heal(e,amount){return world.healing().submit(new (cls("combat.healing.api.HealingRequest"))(UUID.randomUUID(),e.getUUID(),new (cls("combat.api.CombatSource"))(Id.parse("combatnexus:verify"),Optional.empty(),Optional.empty()),cls("combat.healing.api.HealingRequest").Reason.EFFECT,amount));}
    try {

        var v=cow(),a=cow();
        install(v,"protect_a",new BP(100,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$LethalProtection"))())));
        install(v,"protect_b",new BP(0,Java.loadClass("java.util.List").of(new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent$LethalProtection"))())));
        check(rejects(function(){cls("combat.spi.CombatBehaviorAccess").protect(world,v.getUUID(),world.behaviors().capture(v.getUUID()),5,-1,cls("combat.api.behavior.ReactionInputs").EMPTY,Optional.empty());}),"protection SPI requires admitted application authority");
        var normal=hit(v,a,1);check(v.getHealth()>1,"nonlethal damage does not use protection");
        v.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").OFFHAND,new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").TOTEM_OF_UNDYING));
        v.setHealth(5);var first=hit(v,a,1000);
        check(!v.getOffhandItem().isEmpty(),"managed protection precedes native totem");
        check(v.getHealth()===1,"selected protection leaves one HP");
        var entries=first.settlement().get().evidence().journal().entries();
        check(entries.stream().filter(function(e){return String(e.resource())==="PROTECTION";}).count()===1,"exactly one protection receipt");
        check(entries.stream().anyMatch(function(e){return String(e.resource())==="PROTECTION" && String(e.source().get())==="combatnexus:protect_a";}),"highest priority wins");
        v.setHealth(5);hit(v,a,1000);check(v.getHealth()===1,"unchosen source remains available");
        v.setHealth(5);hit(v,a,1000);check(v.getHealth()===1&&v.getOffhandItem().isEmpty(),"native totem protects after managed sources exhausted");
        v.removeAllEffects();v.setHealth(5);hit(v,a,1000);check(!v.isAlive(),"all consumed protections cannot trigger again");

    } finally {for(var i=0;i<entities.length;i++){cls("combat.spi.CombatWorldLifecycleAccess").clear(world,entities[i].getUUID(),ES.Removal.WORLD_EXIT);entities[i].discard();}}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
