// Compare HEAD and refactored servers to make the former startup-catalog fallback visible.
(function(){
    var root="io.github.naimjeg.combatnexus.",level=AutoJS.server().overworld();
    var cow=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    cow.setPos(0,100,0);level.addFreshEntity(cow);
    cow.getAttribute(Java.loadClass(root+"attribute.api.CombatAttributes").resistancePhysical()).setBaseValue(50);
    try{
        var Api=Java.loadClass(root+"combat.damage.api.DamageModuleApi"),Request=Java.loadClass(root+"combat.damage.api.damage.DamageRequest");
        var Source=Java.loadClass(root+"combat.damage.api.damage.DamageSourceDescriptor");
        var result=Api.submitDamage(Request.builder(level,cow,Source.from(cow.damageSources().generic()),2).build());
        return JSON.stringify({status:String(result.status()),resolved:result.resolvedDamage(),healthLoss:result.settlement().isPresent()?result.settlement().get().evidence().healthLoss():null});
    }finally{cow.discard();}
})()
