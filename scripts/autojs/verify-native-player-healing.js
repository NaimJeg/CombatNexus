// Requires SettingsVerifier in the disposable overworld. Uses a separate FoodData to preserve the player's hunger timers.
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var prefix="io.github.naimjeg.combatnexus.";
    function cls(n){return Java.loadClass(prefix+n);}
    var player=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");
    if(player===null)throw new Error("Connect SettingsVerifier before the optional player probe");
    var level=player.level(), health=player.getHealth(), attribute=player.getAttribute(cls("attribute.api.CombatAttributes").healingReceived());
    var base=attribute.getBaseValue(), checks=[], calls=0;
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    var bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS;
    var listener=new (Java.loadClass("java.util.function.Consumer"))({accept:function(e){if(e.getEntity().getUUID().equals(player.getUUID()))calls++;}});
    bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").NORMAL, false,
        Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingHealEvent"),listener);
    var rule=Java.loadClass("net.minecraft.world.level.gamerules.GameRules").NATURAL_HEALTH_REGENERATION;
    check(level.getGameRules().get(rule),"natural regeneration enabled in disposable world");
    try{
        attribute.setBaseValue(2);player.setHealth(2);
        var food=new (Java.loadClass("net.minecraft.world.food.FoodData"))();
        for(var i=0;i<9;i++)food.tick(player);
        check(player.getHealth()===2 && calls===0,"food timing unchanged before tenth eligible tick");
        food.tick(player);
        check(Math.abs(player.getHealth()-(2+5/3))<0.0001 && calls===1,"saturation regeneration applies multiplier once at native tenth tick");
        player.setHealth(2);calls=0;
        var slow=new (Java.loadClass("net.minecraft.world.food.FoodData"))();slow.setSaturation(0);slow.setFoodLevel(18);
        for(var j=0;j<79;j++)slow.tick(player);
        check(player.getHealth()===2 && calls===0,"slow food regeneration waits seventy-nine ticks");
        slow.tick(player);check(player.getHealth()===4 && calls===1,"slow food regeneration retains eighty-tick trigger");
    }finally{bus.unregister(listener);attribute.setBaseValue(base);player.setHealth(health);}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
