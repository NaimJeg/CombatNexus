// Scheduler semantics plus unload/reload through the same trusted lifecycle adapter.
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var prefix="io.github.naimjeg.combatnexus.";
    function cls(name){return Java.loadClass(prefix+name);}
    var UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional");
    var OptionalLong=Java.loadClass("java.util.OptionalLong"),Id=Java.loadClass("net.minecraft.resources.Identifier");
    var id=UUID.randomUUID(),calls=[],natural=true;
    var source=new (cls("combat.api.CombatSource"))(Id.parse("combatnexus:recovery_check"),Optional.empty(),Optional.empty());
    var Conditions=cls("combat.regeneration.spi.RegenerationConditions");
    var State=cls("combat.regeneration.spi.RegenerationConditions$State");
    var conditions=new Conditions({read:function(){return new State(true,natural);}});
    var healing=new (cls("combat.healing.api.HealingService"))({
        submit:function(request){
            calls.push(request.amount());
            return new (cls("combat.healing.api.HealingSettlement"))(request,request.amount(),0,0,
                cls("combat.healing.api.HealingSettlement").Outcome.CANCELLED,cls("combat.api.ApplicationJournal").EMPTY);
        },
        registerSettlementListener:function(){},close:function(){}
    });
    var scheduler=new (cls("combat.regeneration.internal.DefaultRegenerationScheduler"))(conditions);
    var Plan=cls("combat.regeneration.api.RegenerationPlan");
    var checks=[];
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function ticks(start,end){for(var tick=start;tick<=end;tick++)scheduler.tick(tick,healing);}
    try {
        scheduler.replace(new Plan(id,source,new (Java.loadClass("io.github.naimjeg.combatnexus.combat.regeneration.api.RegenerationDefinition"))(2,0,false,false,1200000),Java.loadClass("java.util.OptionalLong").empty()));ticks(0,18);
        check(calls.length===0,"fractional recovery waits for twenty eligible ticks");
        scheduler.tick(19,healing);
        check(calls.length===1 && Math.abs(calls[0]-2)<0.00001,"twenty eligible ticks aggregate HP per second");
        scheduler.tick(1000,healing);scheduler.tick(1000,healing);
        check(calls.length===1,"skipped or repeated ticks do not compensate offline healing");
        ticks(1001,1019);
        check(calls.length===2 && Math.abs(calls[1]-2)<0.00001,"cancelled recovery consumes the installment without retry burst");
        scheduler.reset();calls=[];scheduler.recordDamage(id,0);
        scheduler.replace(new Plan(id,source,new (Java.loadClass("io.github.naimjeg.combatnexus.combat.regeneration.api.RegenerationDefinition"))(2,200,false,false,1200000),Java.loadClass("java.util.OptionalLong").empty()));ticks(0,199);
        check(calls.length===0,"plan damage delay remains independent of combat window");
        ticks(200,219);check(calls.length===1,"recovery resumes at explicit damage-delay boundary");
        scheduler.reset();calls=[];scheduler.recordDamage(id,0);
        scheduler.replace(new Plan(id,source,new (Java.loadClass("io.github.naimjeg.combatnexus.combat.regeneration.api.RegenerationDefinition"))(2,0,false,true,1200000),Java.loadClass("java.util.OptionalLong").empty()));ticks(0,99);
        check(calls.length===0,"one hundred tick combat window suppresses out-of-combat recovery");
        ticks(100,119);check(calls.length===1,"combat-window boundary permits twenty eligible ticks");
        scheduler.reset();calls=[];natural=false;
        scheduler.replace(new Plan(id,source,new (Java.loadClass("io.github.naimjeg.combatnexus.combat.regeneration.api.RegenerationDefinition"))(2,0,true,false,1200000),Java.loadClass("java.util.OptionalLong").empty()));ticks(0,25);
        check(calls.length===0,"native natural-regeneration rule is respected");
        scheduler.reset();calls=[];natural=true;
        scheduler.replace(new Plan(id,source,new (cls("combat.regeneration.api.RegenerationDefinition"))(2,0,false,false,19),OptionalLong.of(19)));
        ticks(0,40);check(calls.length===0,"expiry discards incomplete recovery installment");
        scheduler.reset();calls=[];
        scheduler.replace(new Plan(id,source,new (cls("combat.regeneration.api.RegenerationDefinition"))(2,0,false,false,20),OptionalLong.of(20)));
        ticks(0,40);check(calls.length===1,"expiry after twentieth eligible tick allows exactly one installment");
    } finally {scheduler.close();}
    var server=AutoJS.server(),level=server.getLevel(Java.loadClass("net.minecraft.world.level.Level").NETHER);
    var Lifecycle=cls("combat.platform.CombatWorldLifecycle");
    var old=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),session=old.sessionId(),oldEffects=old.effects();
    var kernel=cls("combat.internal.transaction.CombatServerRuntime").of(server);
    var lineage=cls("combat.api.transaction.CombatLineage").root(cls("combat.api.transaction.CombatOperation").HEALING);
    kernel.admit(lineage);var ticket=kernel.ticket(lineage);ticket.bindSession(level,id);
    Lifecycle.unload(new (Java.loadClass("net.neoforged.neoforge.event.level.LevelEvent$Unload"))(level));
    Lifecycle.load(new (Java.loadClass("net.neoforged.neoforge.event.level.LevelEvent$Load"))(level));
    var replacement=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),rejected=false;
    try{oldEffects.snapshot(id);}catch(error){rejected=true;}
    check(rejected && !replacement.sessionId().equals(session),"world unload invalidates retained services and changes session");
    rejected=false;try{ticket.enter(cls("combat.internal.transaction.CombatExecutionScope").Phase.APPLICATION);}catch(error){rejected=true;}
    kernel.finish(lineage);
    check(rejected,"old world ticket rejected after session replacement");
    return JSON.stringify({passed:checks.length,checks:checks});
})()
