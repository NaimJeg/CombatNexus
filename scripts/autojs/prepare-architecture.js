// Poll from the external runner until forced chunks support native entity lookup.
(function () {
    var prepared=true;
    for(var level of [AutoJS.server().overworld(), AutoJS.server().getLevel(Java.loadClass("net.minecraft.world.level.Level").NETHER)]) {
        level.setChunkForced(0,0,true); level.getChunk(0,0);
        var probe=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        probe.setPos(0,100,0); level.addFreshEntity(probe);
        try { prepared=prepared && level.getEntity(probe.getUUID())!==null; }
        finally {probe.discard();}
    }
    return JSON.stringify({prepared:prepared});
})()
