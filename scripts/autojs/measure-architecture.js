// Identical workload for the repository HEAD baseline and the refactored server.
// One invocation stays below the default 2048 admissions/tick.
(function () {
    var root = "io.github.naimjeg.combatnexus.combat.damage.";
    var Api = Java.loadClass(root + "api.DamageModuleApi");
    var Request = Java.loadClass(root + "api.damage.DamageRequest");
    var Source = Java.loadClass(root + "api.damage.DamageSourceDescriptor");
    var System = Java.loadClass("java.lang.System");
    var Thread = Java.loadClass("java.lang.Thread");
    var level = AutoJS.server().overworld();
    var victim = Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(
        level, Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    victim.setPos(0, 100, 0); level.addFreshEntity(victim);
    var source = Source.from(victim.damageSources().generic());
    var samples = [], count = 500, totalDamage = 0;
    var recording = new (Java.loadClass("jdk.jfr.Recording"))();
    recording["enable(java.lang.String)"]("jdk.ObjectAllocationSample");
    recording["enable(java.lang.String)"]("jdk.GarbageCollection");
    recording["enable(java.lang.String)"]("minecraft.ServerTickTime");
    recording.start();
    var started = System.nanoTime();
    try {
        for (var index = 0; index < count; index++) {
            victim.setHealth(victim.getMaxHealth()); victim.invulnerableTime = 0;
            var begin = System.nanoTime();
            var result = Api.submitDamage(Request.builder(level, victim, source, 2).build());
            samples.push(System.nanoTime() - begin);
            if (String(result.status()) !== "APPLIED") throw new Error("Workload rejected at " + index + ": " + result);
            totalDamage += result.resolvedDamage();
        }
        var elapsed = System.nanoTime() - started;
        recording.stop();
        var path = Java.loadClass("java.nio.file.Path").of("architecture.jfr");
        recording.dump(path);
        var events = Java.loadClass("jdk.jfr.consumer.RecordingFile").readAllEvents(path);
        var allocation = 0, allocationSamples = 0, collections = 0, firstAllocationWeight = 0;
        for (var event of events) {
            var name = String(event.getEventType().getName());
            if (name === "jdk.ObjectAllocationSample" && event.getThread() !== null
                    && String(event.getThread().getJavaName()) === "Server thread") {
                if (allocationSamples === 0) firstAllocationWeight = event.getLong("weight");
                else allocation += event.getLong("weight");
                allocationSamples++;
            }
            if (name === "jdk.GarbageCollection") collections++;
        }
        samples.sort(function (a, b) { return a - b; });
        var tickTimes = [];
        for (var tickTime of AutoJS.server().getTickTimesNanos()) if (tickTime > 0) tickTimes.push(Number(tickTime));
        tickTimes.sort(function (a, b) { return a - b; });
        return JSON.stringify({requests:count, totalDamage:totalDamage, sampledAllocatedBytesPerRequest:allocation/count, allocationSamples:allocationSamples, excludedFirstWeight:firstAllocationWeight, collections:collections,
            elapsedMs:elapsed/1000000, p50ns:samples[Math.floor(count*0.50)], p95ns:samples[Math.floor(count*0.95)],
            p99ns:samples[Math.floor(count*0.99)], nativeTickWindow:{samples:tickTimes.length,
                p50ns:tickTimes[Math.floor(tickTimes.length*.50)],p95ns:tickTimes[Math.floor(tickTimes.length*.95)],
                p99ns:tickTimes[Math.floor(tickTimes.length*.99)]},javaVersion:System.getProperty("java.version")});
    } finally { victim.discard(); recording.close(); }
})()
