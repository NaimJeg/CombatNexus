// Public SDK extensions plus trusted clock advancement in a disposable world.
(function(){
    function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
    var E=Java.loadClass("example.combatnexus.SemanticExample"),Combo=Java.loadClass("example.combatnexus.SemanticExample$Combo");
    var UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional"),OL=Java.loadClass("java.util.OptionalLong"),Set=Java.loadClass("java.util.Set");
    var Id=Java.loadClass("net.minecraft.resources.Identifier"),Json=Java.loadClass("com.google.gson.JsonParser"),Ops=Java.loadClass("com.mojang.serialization.JsonOps");
    var level=AutoJS.server().overworld(),api=cls("combat.api.CombatAccess").find(level).orElseThrow(),runtime=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    var Life=cls("combat.spi.CombatWorldLifecycleAccess"),Task=cls("combat.api.scheduling.ScheduledTask"),Intent=cls("combat.api.scheduling.ScheduledIntent");
    var source=new (cls("combat.api.CombatSource"))(Id.parse("combatnexus_sdk_example:semantic_test"),Optional.empty(),Optional.empty());
    var checks=[],entities=[];
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function near(a,b,label){check(Math.abs(a-b)<0.001,label);}
    function rejects(f,label){var failed=false;try{f();}catch(e){failed=true;}check(failed,label);}
    function cow(){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);e.setPos(0,512,0);e.setNoAi(true);e.setNoGravity(true);level.addFreshEntity(e);entities.push(e);return e;}
    function advance(n){level.getLevelData().setGameTime(level.getGameTime()+n);Life.beginTick(runtime);Life.tickRecovery(runtime);}
    function children(result){return result.beforeApplication().steps().get(0).result().value().orElseThrow().steps();}
    function schedule(e,intent,repetitions){return api.scheduler().schedule(UUID.randomUUID(),e.getUUID(),new Task(UUID.randomUUID(),source,Optional.empty(),level.getGameTime()+1,2,repetitions,intent,Set.of(),true));}
    function hit(e,type,amount){e.invulnerableTime=0;return api.damage(cls("combat.damage.api.damage.DamageRequest").builder(level,e,cls("combat.damage.api.damage.DamageSourceDescriptor").of(Java.loadClass("net.minecraft.resources.ResourceKey").create(Java.loadClass("net.minecraft.core.registries.Registries").DAMAGE_TYPE,Id.parse(type))),amount).build());}
    function absorption(e){return api.absorption().submit(new (cls("combat.absorption.api.AbsorptionCommand"))(UUID.randomUUID(),e.getUUID(),source,cls("combat.absorption.api.AbsorptionCommand$Operation").GRANT,1,8));}
    try{
        E.clear();Life.beginTick(runtime);
        var e=cow();e.setHealth(1);
        var result=E.run(api,e.getUUID(),"combo"),parts=children(result);
        near(e.getHealth(),4,"registered composition heals before parent healing");
        near(e.getAbsorptionAmount(),2,"registered composition applies second domain");
        check(parts.size()===2&&parts.get(0).value().orElseThrow().actualRecovery()===2,"full typed healing receipt retained inside intent");
        check(parts.get(1).value().orElseThrow().applied(),"full absorption receipt retained inside intent");
        var logs=E.settlements(),parentIndex=-1,childIndex=-1;
        for(var i=0;i<logs.size();i++){if(logs.get(i).id()===result.context().get().identity().get().operation())parentIndex=i;if(logs.get(i).id()===parts.get(0).context().get().identity().get().operation())childIndex=i;}
        check(childIndex>=0&&parentIndex>childIndex,"completed plan child notifications retain FIFO order before parent completion");
        check(parts.get(0).context().get().identity().get().parent().getAsLong()===result.beforeApplication().steps().get(0).context().get().identity().get().operation(),"intent children share actual intent parent");
        check(String(result.beforeApplication().steps().get(0).commandType())==="combatnexus_sdk_example:combo","step identifies registered command type");
        rejects(function(){E.expired().target();},"intent execution capability expires");
        var encoded=Intent.CODEC.encodeStart(Ops.INSTANCE,new Combo(2)).getOrThrow();
        check(String(encoded).indexOf("combatnexus_sdk_example:combo")>=0&&Intent.CODEC.parse(Ops.INSTANCE,encoded).getOrThrow().amount()===2,"registered intent codec round trip uses namespace ID");
        rejects(function(){Intent.CODEC.parse(Ops.INSTANCE,Json.parseString('{"type":"missing:intent"}')).getOrThrow();},"unknown intent type rejected");
        rejects(function(){Intent.CODEC.parse(Ops.INSTANCE,Json.parseString('{"type":"combatnexus_sdk_example:combo","amount":-1}')).getOrThrow();},"invalid registered parameters rejected");
        var restricted=cow();restricted.setHealth(1);var denied=E.run(api,restricted.getUUID(),"restricted");
        check(restricted.getAbsorptionAmount()===0&&!denied.beforeApplication().steps().get(0).result().applied(),"undeclared domain rejected without write");
        check(String(children(denied).get(0).rejection().get().code())==="INVALID_PHASE","permission failure retains exact child rejection");
        var nested=cow();nested.setHealth(1);var nr=E.run(api,nested.getUUID(),"nested");
        near(nested.getHealth(),4,"nested intent retains allowed healing");
        check(nested.getAbsorptionAmount()===0&&!nr.beforeApplication().steps().get(0).result().applied(),"nested declaration cannot broaden parent permissions");
        var partial=cow();partial.setHealth(1);var pr=E.run(api,partial.getUUID(),"throw");
        check(partial.getAbsorptionAmount()===2&&!pr.beforeApplication().completed(),"confirmed write retained on intent failure");
        near(partial.getHealth(),1,"unknown intent completion stops parent even with CONTINUE");
        check(children(pr).size()===1&&children(pr).get(0).journal().applicationCompleted(),"failed composite retains exact child evidence");
        var limited=cow();limited.setHealth(1);var lr=E.run(api,limited.getUUID(),"repeat");
        check(children(lr).stream().anyMatch(function(v){return v.rejection().isPresent();}),"intent children consume shared root budget");
        var d=cow();d.setHealth(1);d.addTag("semantic_combo");var dr=hit(d,"minecraft:generic",1);
        check(dr.settlement().get().evidence().beforeApplication().steps().get(0).result().applied(),"same registered intent runs under damage parent");
        near(d.getHealth(),3,"damage parent uses committed child healing and absorption");
        var pulse=cow();pulse.setHealth(1);var installed=E.install(level,pulse.getUUID(),"pulse",false);advance(2);
        near(pulse.getHealth(),3,"periodic behavior executes registered composition");
        near(pulse.getAbsorptionAmount(),2,"periodic composition shares domain adapters");
        rejects(function(){E.expiredPeriodic().execute(new Combo(2));},"periodic execution capability expires");
        check(api.sources().revoke(UUID.randomUUID(),installed.value().get()).applied(),"periodic source revoked");advance(2);
        near(pulse.getHealth(),3,"revoked source stops future composition");
        var overflow=cow();E.install(level,overflow.getUUID(),"periodic_overflow",false);advance(2);
        check(api.shields().snapshot(overflow.getUUID()).stream().mapToDouble(function(p){return p.balance();}).sum()===2,"periodic healing convenience preserves overflow shield reaction permissions");
        var delayed=cow();delayed.setHealth(1);check(schedule(delayed,new Combo(2),1).applied(),"registered intent accepted by persistent scheduler");advance(1);cls("combat.internal.coordination.ScheduledOperations").tick(delayed);
        near(delayed.getHealth(),3,"delayed execution uses same registered composition");
        check(delayed.getAbsorptionAmount()===2&&api.scheduler().snapshot(delayed.getUUID()).isEmpty(),"one-shot composition consumed once");
        var dt=cow();check(schedule(dt,new (cls("combat.api.scheduling.ScheduledIntent$Damage"))(1,Id.parse("minecraft:generic")),1).applied(),"scheduled damage intent accepted");advance(1);cls("combat.internal.coordination.ScheduledOperations").tick(dt);
        near(dt.getHealth(),9,"scheduled damage uses shared command executor");
        var st=cow();schedule(st,new (cls("combat.api.scheduling.ScheduledIntent$Shield"))(3,20),1);advance(1);cls("combat.internal.coordination.ScheduledOperations").tick(st);
        check(api.shields().snapshot(st.getUUID()).get(0).balance()===3&&api.shields().snapshot(st.getUUID()).get(0).expiresAtTick().isPresent(),"scheduled shield preserves duration");
        var failed=cow();schedule(failed,new (Java.loadClass("example.combatnexus.SemanticExample$ThrowAfterWrite"))(),3);advance(1);cls("combat.internal.coordination.ScheduledOperations").tick(failed);
        check(failed.getAbsorptionAmount()===2&&api.scheduler().snapshot(failed.getUUID()).isEmpty(),"incomplete scheduled intent is not retried or repeated");
        var guarded=cow();E.install(level,guarded.getUUID(),"fire",false);hit(guarded,"minecraft:generic",1);guarded.setHealth(5);hit(guarded,"minecraft:in_fire",1000);
        near(guarded.getHealth(),1,"conditional protection reads frozen damage tags");
        guarded.setHealth(5);hit(guarded,"minecraft:in_fire",1000);check(!guarded.isAlive(),"effective protection consumes one use");
        var wrong=cow();E.install(level,wrong.getUUID(),"fire",false);hit(wrong,"minecraft:generic",1000);check(!wrong.isAlive(),"nonmatching lethal damage does not gain protection");
        var lethal=cow();E.install(level,lethal.getUUID(),"fire",false);lethal.setHealth(5);
        var lethalResult=api.health().execute(new (cls("combat.health.api.LethalAttempt"))(UUID.randomUUID(),lethal.getUUID(),source,Id.parse("minecraft:in_fire"),Optional.empty(),false));
        check(lethal.getHealth()===1&&String(lethalResult.value().get().outcome())==="MANAGED_PROTECTED","lethal command supplies known type without inventing attacker");
        var shielded=cow();shielded.setHealth(3);E.install(level,shielded.getUUID(),"shield",false);schedule(shielded,new (cls("combat.api.scheduling.ScheduledIntent$Shield"))(3,20),1);advance(1);cls("combat.internal.coordination.ScheduledOperations").tick(shielded);
        hit(shielded,"minecraft:generic",1);near(shielded.getHealth(),3,"nonbreaking shield consumption does not trigger recovery");
        hit(shielded,"minecraft:generic",2);near(shielded.getHealth(),5,"shield exhaustion triggers typed settlement reaction");
        var state=cow();state.setHealth(1);var sr=E.install(level,state.getUUID(),"state",false);
        near(state.getHealth(),1,"new source does not react to its own installation");absorption(state);near(state.getHealth(),2,"state command settlement triggers existing source behavior");
        api.sources().revoke(UUID.randomUUID(),sr.value().get());near(state.getHealth(),2,"revoked source does not react to its own removal");absorption(state);near(state.getHealth(),2,"revoked source cannot react to subsequent state commands");
        check(E.settlements().stream().anyMatch(function(v){return String(v.operation())==="SHIELD"&&v.shieldPayload();}),"unified envelope retains typed shield receipt");
        check(cls("combat.internal.transaction.CombatCompletionQueue").pendingCount()===0,"cross-domain completion FIFO drains");
        return JSON.stringify({passed:checks.length,checks:checks});
    } finally {for(var e of entities){Life.clear(runtime,e.getUUID(),cls("effect.api.EffectService").Removal.DEATH);e.discard();}E.clear();}
})()
