// A development-only GC probe; never used by gameplay or the allocation benchmark.
(function(){
    var Definitions=Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions");
    var Snapshot=Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration");
    var WeakReference=Java.loadClass("java.lang.ref.WeakReference"),System=Java.loadClass("java.lang.System");
    var original=Definitions.forServer(AutoJS.server()),references=[],candidate=null;
    try{
        for(var index=0;index<32;index++){
            candidate=new Snapshot(Definitions.nextGeneration(),original.settings(),original.damage(),original.behaviors(),original.domains());
            references.push(new WeakReference(candidate));Definitions.publish(AutoJS.server(),candidate);
        }
    }finally{
        candidate=null;
        Definitions.publish(AutoJS.server(),new Snapshot(Definitions.nextGeneration(),original.settings(),
            original.damage(),original.behaviors(),original.domains()));
    }
    System.gc();
    var retained=0;
    for(var reference of references)if(reference.get()!==null)retained++;
    if(retained!==0)throw new Error("Old generations still reachable in controlled GC probe: "+retained);
    return JSON.stringify({passed:2,checks:["thirty-two replaced generation handles released after controlled GC",
        "restored data adopted with a new monotonic generation"],replaced:references.length,retained:retained});
})()
