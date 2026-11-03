(function () {
    var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),root="io.github.naimjeg.combatnexus.",checks=[];
    var stack=mc.player.getMainHandItem(),Context=Java.loadClass("net.minecraft.world.item.Item$TooltipContext"),Component=Java.loadClass("net.minecraft.network.chat.Component");
    var projection=Java.loadClass(root+"presentation.platform.client.CombatPresentationClient").item(stack);
    function check(v,n){if(!v)throw new Error(n+" / "+scenario);checks.push(scenario+": "+n);}
    function tooltip(advanced,shift){var flags=new (Java.loadClass("net.neoforged.neoforge.client.ClientTooltipFlag"))(advanced,false,shift,false,false);var result=[];for(var line of stack.getTooltipLines(Context.of(mc.level),mc.player,flags))result.push(String(line.getString()));return result;}
    var compact=tooltip(false,false),expanded=tooltip(false,true),advanced=tooltip(true,true);
    if(scenario==="rotten"||scenario==="stew"){
        check(projection.isEmpty(),"noncombat effect retains native item description");
        check(expanded.length>0&&expanded.every(function(s){return s.indexOf("tooltip.combatnexus.")<0;}),"native tooltip remains readable");
        return JSON.stringify({passed:checks.length,checks:checks});
    }
    projection=projection.orElseThrow();
    check(!projection.has("error"),"server instance projection accepted");
    check(expanded.every(function(s){return s.indexOf("tooltip.combatnexus.")<0&&s.indexOf("%s")<0;}),"no residual translation placeholders");
    var hint=String(Component.translatable("tooltip.combatnexus.hold_shift").getString());
    if(scenario.indexOf("turtle")>=0){
        check(projection.getAsJsonArray("carried").size()===2,"both adopted effects serialized");
        check(compact.filter(function(s){return s.trim()===hint;}).length===1,"one compact Shift hint");
        check(expanded.some(function(s){return s.indexOf("20%")>=0&&s.indexOf("100%")>=0;}),"server resistance formula and cap");
        check(!compact.some(function(s){return s.indexOf("20%")>=0&&s.indexOf("100%")>=0;}),"compact hides generated explanation");
        var applied=String(Component.translatable("potion.whenDrank").getString());
        check(compact.indexOf(applied)<0&&expanded.indexOf(applied)>=0,"native attributes shown only with Shift");
    }
    if(scenario==="rotten")check(expanded.some(function(s){return s.indexOf("80%")>=0;})&&!compact.some(function(s){return s.indexOf("80%")>=0;}),"food probability only in expanded view");
    if(scenario==="golden")check(projection.getAsJsonArray("carried").size()===2,"food retains both effects");
    if(scenario==="water"||scenario==="hidden")check(projection.getAsJsonArray("carried").isEmpty()&&compact.indexOf(hint)<0,"empty or hidden component exposes no effect");
    if(scenario==="stew")check(projection.getAsJsonArray("stew").size()===1&&projection.getAsJsonArray("carried").isEmpty()&&advanced.length>expanded.length,"stew remains advanced-only");
    return JSON.stringify({passed:checks.length,checks:checks,compact:compact,expanded:expanded,language:String(mc.getLanguageManager().getSelected())});
})()
