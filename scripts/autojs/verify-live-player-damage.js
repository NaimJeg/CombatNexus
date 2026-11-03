// Requires the isolated SettingsVerifier client connected to the server.
(function(){
    var root="io.github.naimjeg.combatnexus.";
    function cls(name){return Java.loadClass(root+name);}
    var player=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");
    if(player===null)throw new Error("Connect the isolated client first");
    var level=player.level(),runtime=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    var Id=Java.loadClass("net.minecraft.resources.Identifier"),Optional=Java.loadClass("java.util.Optional");
    var OptionalLong=Java.loadClass("java.util.OptionalLong"),Set=Java.loadClass("java.util.Set");
    var Pool=cls("combat.shield.api.ShieldGrant");
    var source=new (cls("combat.api.CombatSource"))(Id.parse("combatnexus:player_application_check"),Optional.empty(),Optional.empty());
    var Api=cls("combat.damage.api.DamageModuleApi"),Request=cls("combat.damage.api.damage.DamageRequest"),Source=cls("combat.damage.api.damage.DamageSourceDescriptor");
    var mode=player["gameMode()"](),health=player.getHealth(),checks=[];
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    try{
        player.setGameMode(Java.loadClass("net.minecraft.world.level.GameType").SURVIVAL);player.setHealth(player.getMaxHealth());player.invulnerableTime=0;
        runtime.shields().replace(player.getUUID(),new Pool(source,new (Java.loadClass("io.github.naimjeg.combatnexus.combat.shield.api.ShieldDefinition"))(10,0,Set.of(),Set.of(),0,0,(Java.loadClass("io.github.naimjeg.combatnexus.combat.shield.api.ShieldDefinition$Stacking")).REPLACE_SAME_SOURCE),10,OptionalLong.empty()));
        var before=player.getHealth();
        var full=Api.submitDamage(Request.builder(level,player,Source.from(player.damageSources().generic()),2).build());
        check(String(full.status())==="APPLIED" && player.getHealth()===before && full.settlement().get().evidence().shieldLoss()===2,
            "real player shield mixin commits full absorption at native boundary");
        check(full.settlement().get().evidence().journal().entries().stream().anyMatch(function(entry){
            return String(entry.resource())==="SHIELD" && entry.source().isPresent();}),"player shield journal identifies the consumed source");
        runtime.shields().remove(player.getUUID(),source.id(),(Java.loadClass("io.github.naimjeg.combatnexus.combat.shield.api.ShieldPoolSnapshot$RemovalReason")).REMOVED);player.invulnerableTime=0;
        var hit=Api.submitDamage(Request.builder(level,player,Source.from(player.damageSources().generic()),2).build());
        check(String(hit.status())==="APPLIED" && hit.settlement().get().evidence().healthWriteObserved()
            && hit.settlement().get().evidence().healthLoss()===2,"real player native health mixin records confirmed loss");
        check(hit.settlement().get().evidence().journal().applicationCompleted(),"real player application freezes completed journal");
    }finally{runtime.shields().remove(player.getUUID(),source.id(),(Java.loadClass("io.github.naimjeg.combatnexus.combat.shield.api.ShieldPoolSnapshot$RemovalReason")).REMOVED);
        player.setHealth(health);player.setGameMode(mode);player.invulnerableTime=0;}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
