(function(){
    var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),root="io.github.naimjeg.combatnexus.";
    var Effects=Java.loadClass("net.minecraft.world.effect.MobEffects"),Component=Java.loadClass("net.minecraft.network.chat.Component");
    var Tooltip=Java.loadClass(root+"presentation.client.NativeEffectTooltip"),Cache=Java.loadClass(root+"presentation.client.ClientEffectTooltips");
    var Id=Java.loadClass("net.minecraft.resources.Identifier"),checks=[];
    function check(v,l){if(!v)throw new Error(l);checks.push(l);}
    function list(){return new (Java.loadClass("java.util.ArrayList"))();}
    function text(lines){var out=[];for(var line of lines)out.push(String(line.getString()));return out;}
    var screen=new (Java.loadClass("net.minecraft.client.gui.screens.inventory.InventoryScreen"))(mc.player);mc.setScreen(screen);
    var effect=mc.player.getEffect(Effects.SPEED),before=mc.player.getAttributeValue(Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").MOVEMENT_SPEED);
    var wide=Java.loadClass("net.neoforged.neoforge.client.ClientHooks").getEffectTooltip(screen,effect,list()),wideText=text(wide);
    check(wideText.some(function(s){return s.indexOf("40%")>=0;}),"wide native tooltip consumes server attribute definition");
    var compact=list();compact.add(wide.get(0));compact.add(wide.get(1));compact.add(Component.literal("OTHER_MOD_LINE"));
    var compactText=text(Java.loadClass("net.neoforged.neoforge.client.ClientHooks").getEffectTooltip(screen,effect,compact));
    check(compactText.filter(function(s){return s===wideText[0];}).length===1&&compactText.filter(function(s){return s===wideText[1];}).length===1,"compact title and countdown appear once");
    check(compactText.indexOf("OTHER_MOD_LINE")>=0,"other mod tooltip lines preserved");
    var details=text(Tooltip.append(list(),effect,20,true));
    check(details.some(function(s){return s.indexOf(String(mc.player.getUUID()))>=0;}),"Shift source matches actual caster");
    check(details.some(function(s){return s.indexOf(String(Component.translatable("tooltip.combatnexus.effect.level","2").getString()))>=0;}),"Shift displays one-based native level");
    var burning=Java.loadClass("net.minecraft.core.registries.BuiltInRegistries").MOB_EFFECT.get(Id.parse("combatnexus:burning")).orElseThrow();
    var fire=mc.player.getEffect(burning);check(fire!==null&&fire.getDuration()>0,"environment identity appears in original active effect collection");
    var fireText=text(Tooltip.append(list(),fire,20,true));
    check(fireText.some(function(s){return s.indexOf(String(Component.translatable("tooltip.combatnexus.environment.burning").getString()))>=0;}),"environment explanation comes from adopted definition");
    check(fireText.every(function(s){return s.indexOf("tooltip.combatnexus.")<0;}),"all adopted environment tooltip keys translated");
    var cactus=Cache.get(Id.parse("combatnexus:cactus_contact")).orElseThrow();
    check(text(cactus.generated().stream().map(function(t){return Java.loadClass(root+"presentation.client.DisplayTextResolver").resolve(t);}).toList()).some(function(s){return s.indexOf("0.5")>=0;}),"scheduled contact tooltip receives actual half-second period");
    check(text(cactus.details().stream().map(function(t){return Java.loadClass(root+"presentation.client.DisplayTextResolver").resolve(t);}).toList()).indexOf(String(Component.translatable("tooltip.combatnexus.effect.independent_cooldown").getString()))>=0,"scheduled contact describes independent cooldown and retained immunity");
    var hunger=Cache.get(Id.parse("combatnexus:starvation")).orElseThrow(),resolve=Java.loadClass(root+"presentation.client.DisplayTextResolver");
    var hungerSummary=text(hunger.generated().stream().map(function(t){return resolve.resolve(t);}).toList());
    var hungerDetails=text(hunger.details().stream().map(function(t){return resolve.resolve(t);}).toList());
    check(hungerSummary.some(function(s){return s.indexOf("4")>=0&&s.indexOf("1")>=0;}),"starvation text consumes adopted four-second period and one base damage");
    check(hungerDetails.indexOf(String(Component.translatable("tooltip.combatnexus.effect.first_delay","4").getString()))>=0,"starvation text consumes adopted four-second initial delay");
    check(hungerDetails.indexOf(String(Component.translatable("tooltip.combatnexus.effect.minimum_health","1").getString()))>=0,"starvation text consumes normal difficulty health floor");
    check(hungerSummary.every(function(s){return s.indexOf("tooltip.combatnexus.")<0;}),"starvation summary is translated");
    var atlas=mc.getAtlasManager().getAtlasOrThrow(Java.loadClass("net.minecraft.data.AtlasIds").GUI);
    for(var name of ["burning","suffocation","cramming","drowning","freezing","starvation","fire_contact","lava_contact","campfire_contact","hot_floor_contact","cactus_contact","berry_contact","drying","water_sensitive","world_border"]){
        var icon=Id.parse("combatnexus:mob_effect/"+name);
        check(String(atlas.getSprite(icon).contents().name())===String(icon),"native GUI atlas resolves environment icon: "+name);
    }
    check(mc.player.getEffect(Effects.NIGHT_VISION).isInfiniteDuration(),"permanent native projection has infinite duration");
    check(!mc.player.hasEffect(Effects.INSTANT_HEALTH)&&!mc.player.hasEffect(Effects.INSTANT_DAMAGE),"instant effects leave no active icon");
    check(before===mc.player.getAttributeValue(Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").MOVEMENT_SPEED),"tooltip rendering installs no attributes");
    return JSON.stringify({passed:checks.length,checks:checks,wide:wideText,details:details,environment:fireText,language:String(mc.getLanguageManager().getSelected())});
})()
