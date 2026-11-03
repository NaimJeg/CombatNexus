// Same source on baseline and current builds. LOAD is 20, 100, or 500 live targets.
(function(){
    function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
    var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    cls("combat.spi.CombatWorldLifecycleAccess").beginTick(world);
    var System=Java.loadClass("java.lang.System"),Thread=Java.loadClass("java.lang.Thread");
    var entities=[],samples=[],sum=0,started;
    var recording=new (Java.loadClass("jdk.jfr.Recording"))();recording["enable(java.lang.String)"]("jdk.ObjectAllocationSample");
    function cow(){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        e.setPos(0,100,0);e.setNoAi(true);e.setNoGravity(true);level.addFreshEntity(e);entities.push(e);return e;}
    try{
        var attacker=cow();for(var i=0;i<LOAD;i++)cow();
        var source=cls("combat.damage.api.damage.DamageSourceDescriptor").from(attacker.damageSources().mobAttack(attacker));
        recording.start();started=System.nanoTime();
        for(var i=1;i<entities.length;i++){
            var t=System.nanoTime();var result=cls("combat.damage.api.DamageModuleApi").submitDamage(
                cls("combat.damage.api.damage.DamageRequest").builder(level,entities[i],source,2).logicalAttacker(attacker).directEntity(attacker).build());
            samples.push(System.nanoTime()-t);
            if(String(result.status())!=="APPLIED")throw new Error("Rejected load request "+i);
            sum+=result.resolvedDamage();
        }
        var elapsed=System.nanoTime()-started,bytes=0,allocationSamples=0;
        recording.stop();var path=Java.loadClass("java.nio.file.Path").of("capability-load.jfr");recording.dump(path);
        for(var event of Java.loadClass("jdk.jfr.consumer.RecordingFile").readAllEvents(path))
            if(String(event.getEventType().getName())==="jdk.ObjectAllocationSample"&&event.getThread()!==null&&String(event.getThread().getJavaName())==="Server thread"){
                if(allocationSamples++>0)bytes+=event.getLong("weight");
            }
        samples.sort(function(a,b){return a-b;});
        return JSON.stringify({targets:LOAD,requests:LOAD,damage:sum,elapsedMs:elapsed/1000000,sampledBytesPerRequest:allocationSamples>1?bytes/LOAD:null,allocationSamples:allocationSamples,
            p50ns:samples[Math.floor(LOAD*.50)],p95ns:samples[Math.floor(LOAD*.95)],p99ns:samples[Math.floor(LOAD*.99)],javaVersion:System.getProperty("java.version")});
    }finally{for(var e of entities)e.discard();recording.close();}
})()
