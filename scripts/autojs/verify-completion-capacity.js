// Exhaust the real server queue reservation ceiling without writing any gameplay resources.
(function(){
    var root="io.github.naimjeg.combatnexus.";
    function cls(name){return Java.loadClass(root+name);}
    var level=AutoJS.server().overworld(),kernel=cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server());
    var held=new (Java.loadClass("java.util.ArrayList"))();
    var cow=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    cow.setPos(0,100,0);level.addFreshEntity(cow);
    var checks=[];function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    var Api=cls("combat.damage.api.DamageModuleApi"),Request=cls("combat.damage.api.damage.DamageRequest"),Source=cls("combat.damage.api.damage.DamageSourceDescriptor");
    try{
        var ceiling=cls("combat.api.transaction.CombatLimits").HARD_MAX_PER_TICK;
        for(var i=0;i<ceiling;i++){
            var slot=kernel.rejectionCompletion();if(slot.isEmpty())break;held.add(slot.get());
        }
        check(held.size()===ceiling,"real server completion ceiling reserved");
        var before=cow.getHealth(),count=kernel.currentTickCount();
        var result=Api.submitDamage(Request.builder(level,cow,Source.from(cow.damageSources().generic()),2).build());
        check(String(result.status())==="REJECTED" && String(result.failure().get().reason())==="COMPLETION_CAPACITY_EXHAUSTED",
            "completion envelope shortage rejects during admission");
        check(cow.getHealth()===before && kernel.currentTickCount()===count,"capacity rejection writes no resources and consumes no admission budget");
    }finally{for(var slot of held)slot.close();cow.discard();}
    check(cls("combat.internal.transaction.CombatCompletionQueue").pendingCount()===0,"reserved capacity released without publication");
    return JSON.stringify({passed:checks.length,checks:checks});
})()
