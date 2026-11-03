"""Real dedicated server/client Effect projection, translated tooltips and reconnect checks."""
import argparse
import json
import time
from pathlib import Path
import importlib.util

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('effect_runtime', ROOT/'scripts/verify-capabilities-runtime.py')
runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runtime)
evaluate = runtime.evaluate

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-port', type=int, default=25595)
    parser.add_argument('--client-port', type=int, default=25596)
    parser.add_argument('--game-port', type=int, default=25570)
    parser.add_argument('--public-events', action='store_true', help='Requires the effectProbe startup extension on both sides')
    args = parser.parse_args()
    checks = []
    def reset_events():
        if args.public_events: server('Java.loadClass("probe.EffectExtension").clearMoments();JSON.stringify({cleared:true})')
    def effect_events():
        return server('''(function(){var E=Java.loadClass("probe.EffectExtension"),M=Java.loadClass("io.github.naimjeg.combatnexus.combat.api.action.CombatMoment$Effect"),p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier"),all=E.moments(),events=[];
            for(var i=0;i<all.size();i++){var m=all.get(i);if(m instanceof M&&m.target().equals(p.getUUID())&&String(m.effect())==="minecraft:speed")events.push({kind:String(m.change()),reason:m.managedChange().get().removal().isPresent()?String(m.managedChange().get().removal().get()):null});}
            return JSON.stringify({events:events});})()''')['events']
    def client(code): return evaluate(args.client_port, code, side='client')
    def server(code): return evaluate(args.server_port, code)
    def wait(code, predicate=lambda v: v['ready']):
        deadline = time.monotonic() + 40
        while True:
            result = client(code)
            if predicate(result): return result
            if time.monotonic() > deadline: raise AssertionError(result)
            time.sleep(.2)
    def connect():
        client('''var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.player===null){var Data=Java.loadClass("net.minecraft.client.multiplayer.ServerData"),address="127.0.0.1:PORT";
        Java.loadClass("net.minecraft.client.gui.screens.ConnectScreen").startConnecting(new (Java.loadClass("net.minecraft.client.gui.screens.TitleScreen"))(),mc,
        Java.loadClass("net.minecraft.client.multiplayer.resolver.ServerAddress").parseString(address),new Data("Effects",address,Data.Type.OTHER),false,null);}
        JSON.stringify({connecting:true})'''.replace('PORT',str(args.game_port)))
        wait('JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance().player!==null})')
    connect()
    server('var s=AutoJS.server();s.getCommands().performPrefixedCommand(s.createCommandSourceStack(),"gamemode survival SettingsVerifier");s.getCommands().performPrefixedCommand(s.createCommandSourceStack(),"execute in minecraft:overworld run tp SettingsVerifier 0 300 0");JSON.stringify({overworld:true})')
    wait('JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance().level.dimension().identifier().toString()==="minecraft:overworld"})')
    server('var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");p.setNoGravity(true);p.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").HEAD,new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").TURTLE_HELMET));JSON.stringify({equipped:true})')
    wait('JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance().player.hasEffect(Java.loadClass("net.minecraft.world.effect.MobEffects").WATER_BREATHING)})')
    checks.append('real connected player native turtle helmet tick grants native Water Breathing')
    native = server('''(function(){var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier"),states=Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(p.level()).effects().snapshot(p.getUUID());
        return JSON.stringify({valid:states.stream().noneMatch(function(s){return String(s.snapshot().effect())==="minecraft:water_breathing";})});})()''')
    assert native['valid'], native
    checks.append('turtle helmet Water Breathing retains native ownership')
    server('var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");p.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").HEAD,Java.loadClass("net.minecraft.world.item.ItemStack").EMPTY);p.removeEffect(Java.loadClass("net.minecraft.world.effect.MobEffects").WATER_BREATHING);JSON.stringify({unequipped:true})')
    server('''var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");p.setInvulnerable(true);p.setNoGravity(true);p.teleportTo(0.5,300,0.5);p.removeAllEffects();
        var E=Java.loadClass("net.minecraft.world.effect.MobEffects"),I=Java.loadClass("net.minecraft.world.effect.MobEffectInstance");
        p.addEffect(new I(E.SPEED,20000,1),p);p.addEffect(new I(E.NIGHT_VISION,-1,0));p.setRemainingFireTicks(20000);
        p.level().setBlock(p.blockPosition().below(2),Java.loadClass("net.minecraft.world.level.block.Blocks").STONE.defaultBlockState(),2);
        p.level().setBlock(p.blockPosition().below(),Java.loadClass("net.minecraft.world.level.block.Blocks").SAND.defaultBlockState(),2);
        p.level().setBlock(p.blockPosition(),Java.loadClass("net.minecraft.world.level.block.Blocks").CACTUS.defaultBlockState(),2);
        Java.loadClass("io.github.naimjeg.combatnexus.effect.internal.EnvironmentEffects").sample(p);
        JSON.stringify({installed:true})''')
    ready='''(function(){var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),C=Java.loadClass("io.github.naimjeg.combatnexus.presentation.client.ClientEffectTooltips"),Id=Java.loadClass("net.minecraft.resources.Identifier");
        return JSON.stringify({ready:mc.player!==null&&mc.player.hasEffect(Java.loadClass("net.minecraft.world.effect.MobEffects").SPEED)&&C.get(Id.parse("minecraft:speed")).isPresent()&&C.get(Id.parse("combatnexus:burning")).isPresent()});})()'''
    wait(ready)
    checks.append('native projection and adopted definition arrive from dedicated server')
    wait('JSON.stringify({ready:Java.loadClass("io.github.naimjeg.combatnexus.presentation.client.ClientEffectTooltips").get(Java.loadClass("net.minecraft.resources.Identifier").parse("combatnexus:cactus_contact")).isPresent()})')
    server('var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");AutoJS.server().setDifficulty(Java.loadClass("net.minecraft.world.Difficulty").NORMAL,true);p.getFoodData().setFoodLevel(0);Java.loadClass("io.github.naimjeg.combatnexus.effect.internal.EnvironmentEffects").sample(p);JSON.stringify({starving:true})')
    wait('JSON.stringify({ready:Java.loadClass("io.github.naimjeg.combatnexus.presentation.client.ClientEffectTooltips").get(Java.loadClass("net.minecraft.resources.Identifier").parse("combatnexus:starvation")).isPresent()})')
    reports=[]
    for language in ('en_us','zh_cn'):
        client('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.getLanguageManager().setSelected('+json.dumps(language)+');mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());JSON.stringify({changed:true})')
        report=client((ROOT/'scripts/autojs/verify-effect-display-client.js').read_text(encoding='utf-8'))
        checks.extend(language+': '+s for s in report['checks']);reports.append(report)
    server('var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");p.getFoodData().setFoodLevel(20);Java.loadClass("io.github.naimjeg.combatnexus.effect.internal.EnvironmentEffects").sample(p);JSON.stringify({fed:true})')
    wait('JSON.stringify({ready:Java.loadClass("io.github.naimjeg.combatnexus.presentation.client.ClientEffectTooltips").get(Java.loadClass("net.minecraft.resources.Identifier").parse("combatnexus:starvation")).isEmpty()})')
    checks.append('food recovery removes starvation icon and adopted description')
    if args.public_events:
        reset_events()
        generation=server('var s=AutoJS.server(),d=Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions").forServer(s);var g=d.generation();s.getCommands().performPrefixedCommand(s.createCommandSourceStack(),"reload");JSON.stringify({generation:g})')['generation']
        deadline=time.monotonic()+40
        while server('JSON.stringify({generation:Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions").forServer(AutoJS.server()).generation()})')['generation'] <= generation:
            if time.monotonic()>deadline: raise TimeoutError('definition reload')
            time.sleep(.2)
        assert effect_events() == [], effect_events()
        checks.append('actual definition reload does not republish unchanged adopted Effect state')
    server('var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");p.level().setBlock(p.blockPosition(),Java.loadClass("net.minecraft.world.level.block.Blocks").AIR.defaultBlockState(),2);Java.loadClass("io.github.naimjeg.combatnexus.effect.internal.EnvironmentEffects").sample(p);JSON.stringify({contactEnded:true})')
    client('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.disconnect(new (Java.loadClass("net.minecraft.client.gui.screens.TitleScreen"))(),false);JSON.stringify({disconnected:true})')
    wait('JSON.stringify({ready:Java.loadClass("io.github.naimjeg.combatnexus.presentation.client.ClientEffectTooltips").get(Java.loadClass("net.minecraft.resources.Identifier").parse("minecraft:speed")).isEmpty()})')
    checks.append('disconnect clears adopted tooltip definitions')
    time.sleep(1)
    connect();wait(ready)
    checks.append('relogin restores timed effect, burning and their server definitions')
    reset_events()
    server('''var s=AutoJS.server(),p=s.getPlayerList().getPlayerByName("SettingsVerifier");p.setTicksFrozen(140);
        Java.loadClass("io.github.naimjeg.combatnexus.effect.internal.EnvironmentEffects").sample(p);p.setTicksFrozen(0);
        s.getCommands().performPrefixedCommand(s.createCommandSourceStack(),"execute in minecraft:the_nether run tp SettingsVerifier 0 120 0");JSON.stringify({moved:true})''')
    wait('JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance().level.dimension().identifier().toString()==="minecraft:the_nether"})')
    wait(ready)
    checks.append('dimension transfer reinstalls managed effects and server descriptions')
    if args.public_events:
        events=effect_events()
        assert events == [{'kind':'REMOVED','reason':'DIMENSION_CHANGE'},{'kind':'ADDED','reason':None}], events
        checks.append('dimension transfer publishes one removal with dimension reason and one destination restore')
    wait('JSON.stringify({ready:!Java.loadClass("net.minecraft.client.Minecraft").getInstance().player.hasEffect(Java.loadClass("net.minecraft.core.registries.BuiltInRegistries").MOB_EFFECT.get(Java.loadClass("net.minecraft.resources.Identifier").parse("combatnexus:freezing")).orElseThrow())})')
    checks.append('destination does not restore stale freezing binding')
    reset_events()
    server('var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");p.setInvulnerable(false);p.hurtServer(p.level(),p.damageSources().genericKill(),1000);JSON.stringify({killed:true})')
    wait('JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance().player.isDeadOrDying()})')
    client('Java.loadClass("net.minecraft.client.Minecraft").getInstance().player.respawn();JSON.stringify({respawn:true})')
    wait('JSON.stringify({ready:!Java.loadClass("net.minecraft.client.Minecraft").getInstance().player.isDeadOrDying()&&Java.loadClass("net.minecraft.client.Minecraft").getInstance().player.getActiveEffects().isEmpty()})')
    checks.append('death and actual client respawn clear managed effects and native icons')
    if args.public_events:
        events=effect_events()
        assert events == [{'kind':'REMOVED','reason':'DEATH'}], events
        checks.append('death publishes one removal; respawn does not repeat it')
    server('var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");p.removeAllEffects();p.clearFire();JSON.stringify({cleared:true})')
    wait('JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance().player.getActiveEffects().isEmpty()&&Java.loadClass("io.github.naimjeg.combatnexus.presentation.client.ClientEffectTooltips").get(Java.loadClass("net.minecraft.resources.Identifier").parse("minecraft:speed")).isEmpty()})')
    checks.append('removal clears both native icons and adopted definitions')
    output=ROOT/'build/verification/effect-display.json';output.parent.mkdir(parents=True,exist_ok=True)
    result={'passed':len(checks),'checks':checks,'languages':reports,'scope':'actual server/client packets and native inventory event; no mouse screenshot assertion'}
    output.write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__': main()
