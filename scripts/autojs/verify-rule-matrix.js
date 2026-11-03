// Requires the temporary architecture-rules datapack installed by the external runner.
(function(){
    var root="io.github.naimjeg.combatnexus.",level=AutoJS.server().overworld();
    function cls(name){return Java.loadClass(root+name);}
    var UUID=Java.loadClass("java.util.UUID"),Id=Java.loadClass("net.minecraft.resources.Identifier");
    var fake=new (Java.loadClass("net.neoforged.neoforge.common.util.FakePlayer"))(level,
        new (Java.loadClass("com.mojang.authlib.GameProfile"))(UUID.randomUUID(),"CNVerifier"));
    fake.setPos(1,100,0);level.addNewPlayer(fake);
    var cow=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    cow.setPos(0,100,0);level.addFreshEntity(cow);
    var Api=cls("combat.damage.api.DamageModuleApi"),Request=cls("combat.damage.api.damage.DamageRequest"),Source=cls("combat.damage.api.damage.DamageSourceDescriptor");
    var crit=fake.getAttribute(cls("attribute.api.CombatAttributes").critChance());
    var armor=cow.getAttribute(Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").ARMOR);
    var source=Source.from(cow.damageSources().playerAttack(fake)),checks=[];
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function hit(action,amount){cow.setHealth(cow.getMaxHealth());cow.invulnerableTime=0;
        return Api.submitDamage(Request.builder(level,cow,source,amount).logicalAttacker(fake).actionId(Id.parse("combatnexus:"+action)).build());}
    try{
        crit.setBaseValue(0);armor.setBaseValue(0);
        var normal=hit("plain",2);check(String(normal.status())==="APPLIED" && !normal.critical(),"ordinary primary damage remains noncritical at zero chance");
        crit.setBaseValue(1);
        var critical=hit("plain",2);check(critical.critical() && critical.resolvedDamage()>normal.resolvedDamage(),"player attribute critical chance applies once");
        crit.setBaseValue(0);armor.setBaseValue(100);
        var truth=hit("true_probe",1);
        check(truth.resolvedDamage()>=5 && truth.resolvedDamage()<6,"true bucket bypasses armor while ordinary amount remains mitigated");
        var cancelled=hit("cancel_probe",2);
        check(cancelled.cancelled() && String(cancelled.status())==="NOT_APPLIED" && cow.getHealth()===cow.getMaxHealth(),
            "final cancellation freezes a non-applied result without native health loss");
        armor.setBaseValue(0);cow.setHealth(3);cow.invulnerableTime=0;
        var overkill=Api.submitDamage(Request.builder(level,cow,source,100).logicalAttacker(fake).build());
        check(overkill.settlement().get().evidence().healthLoss()===3,"overkill evidence caps confirmed health loss to actual native write");
    }finally{cow.discard();fake.discard();}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
