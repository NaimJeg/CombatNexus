"""Run carried Effect tooltips against an isolated AutoJS client/server, including real item events."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import socket
import time
import sys

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / 'targets/neoforge-26.1.2'
spec = importlib.util.spec_from_file_location('runtime', ROOT / 'scripts/verify-capabilities-runtime.py')
runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runtime)

def main():
    output = ROOT / 'build/verification/carried-effect-tooltips'
    output.mkdir(parents=True, exist_ok=True)
    run = TARGET / 'build/carried-tooltip-run'
    run.mkdir(parents=True, exist_ok=True)
    (run / 'eula.txt').write_text('eula=true\n')
    (run / 'server.properties').write_text('online-mode=false\nserver-ip=127.0.0.1\nspawn-protection=0\npause-when-empty-seconds=-1\n')
    for port in [25587, 25588, 25589]:
        with socket.socket() as probe:
            probe.bind(('127.0.0.1', port))
    processes, logs = [], []
    def evaluate(code, client=False):
        return runtime.evaluate(25588 if client else 25587, code, side='client' if client else 'server')
    def wait(code, client=False, seconds=180):
        deadline = time.monotonic() + seconds
        while True:
            if any(p.poll() is not None for p in processes): raise RuntimeError('Verification process exited; inspect server/client log')
            try:
                value = evaluate(code, client)
                if value.get('ready'): return value
            except (OSError, RuntimeError):
                if time.monotonic() >= deadline: raise
            if time.monotonic() >= deadline: raise TimeoutError(code)
            time.sleep(.5)
    try:
        for side, properties in [('server', ['-PverificationAutojsPort=25587', '-PverificationGamePort=25589', '-PverificationRunDirectory=build/carried-tooltip-run']),
                                 ('client', ['-PverificationClientAutojsPort=25588'])]:
            log = (output / (side + '.log')).open('w', encoding='utf-8'); logs.append(log)
            processes.append(subprocess.Popen([str(TARGET / 'gradlew.bat'), 'run' + side.title(), '-I',
                str(ROOT / 'scripts/autojs' / (side + '.init.gradle')), '-I', str(ROOT / 'scripts/autojs/effect-extension.init.gradle'), *properties, '--console', 'plain'], cwd=TARGET,
                stdout=log, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0))
            wait('JSON.stringify({ready:true})', side == 'client')
        evaluate('''var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.options.pauseOnLostFocus=false;
var Data=Java.loadClass("net.minecraft.client.multiplayer.ServerData"),address="127.0.0.1:25589";
Java.loadClass("net.minecraft.client.gui.screens.ConnectScreen").startConnecting(new (Java.loadClass("net.minecraft.client.gui.screens.TitleScreen"))(),mc,
Java.loadClass("net.minecraft.client.multiplayer.resolver.ServerAddress").parseString(address),new Data("Tooltip verification",address,Data.Type.OTHER),false,null);JSON.stringify({connecting:true})''', True)
        wait('''JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance().player!==null&&
Java.loadClass("io.github.naimjeg.combatnexus.presentation.client.ClientTooltipDefinitions").itemEffect(Java.loadClass("net.minecraft.resources.Identifier").parse("minecraft:resistance")).isPresent()})''', True)
        reports = []
        samples = []
        for item in ['POTION','SPLASH_POTION','LINGERING_POTION','TIPPED_ARROW']:
            for potion in ['TURTLE_MASTER','LONG_TURTLE_MASTER','STRONG_TURTLE_MASTER']:
                samples.append(('turtle-'+item+'-'+potion, 'Potion.createItemStack(Items.'+item+',Potions.'+potion+')', ''))
        samples += [('golden','new Stack(Items.GOLDEN_APPLE)',''),('rotten','new Stack(Items.ROTTEN_FLESH)',''),
            ('water','Potion.createItemStack(Items.POTION,Potions.WATER)',''),
            ('stew','new Stack(Items.SUSPICIOUS_STEW)','stack.set(Components.SUSPICIOUS_STEW_EFFECTS,new (Java.loadClass("net.minecraft.world.item.component.SuspiciousStewEffects"))(Java.loadClass("java.util.List").of(new (Java.loadClass("net.minecraft.world.item.component.SuspiciousStewEffects$Entry"))(Java.loadClass("net.minecraft.world.effect.MobEffects").NIGHT_VISION,200))));'),
            ('hidden','Potion.createItemStack(Items.POTION,Potions.STRONG_TURTLE_MASTER)','stack.set(Components.TOOLTIP_DISPLAY,Java.loadClass("net.minecraft.world.item.component.TooltipDisplay").DEFAULT.withHidden(Components.POTION_CONTENTS,true));')]
        for language in ['en_us', 'zh_cn']:
            evaluate('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.getLanguageManager().setSelected('+json.dumps(language)+');mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());JSON.stringify({changed:true})', True)
            for name, expression, mutate in samples:
                evaluate('var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier"),Items=Java.loadClass("net.minecraft.world.item.Items"),Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Potion=Java.loadClass("net.minecraft.world.item.alchemy.PotionContents"),Potions=Java.loadClass("net.minecraft.world.item.alchemy.Potions"),Components=Java.loadClass("net.minecraft.core.component.DataComponents");var stack='+expression+';'+mutate+'stack.set(Components.CUSTOM_NAME,Java.loadClass("net.minecraft.network.chat.Component").literal('+json.dumps(name)+'));p.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").MAINHAND,stack);p.containerMenu.broadcastChanges();JSON.stringify({given:true})')
                wait('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();JSON.stringify({ready:String(mc.player.getMainHandItem().getHoverName().getString())==='+json.dumps(name)+'})',True)
                report=evaluate('var scenario='+json.dumps(name)+';'+(ROOT / 'scripts/autojs/verify-carried-effect-tooltip-client.js').read_text(encoding='utf-8'),True)
                reports.append(report)
                print(json.dumps({'sample':name,'language':language,'passed':report['passed']}),flush=True)
        evaluate('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.disconnect(new (Java.loadClass("net.minecraft.client.gui.screens.TitleScreen"))(),false);JSON.stringify({done:true})', True)
        cleared = evaluate('JSON.stringify({cleared:Java.loadClass("io.github.naimjeg.combatnexus.presentation.client.ClientTooltipDefinitions").itemEffect(Java.loadClass("net.minecraft.resources.Identifier").parse("minecraft:resistance")).isEmpty()})', True)
        assert cleared['cleared'], cleared
        result = {'passed': sum(r['passed'] for r in reports)+1, 'reports': reports, 'disconnectCleared': True,
                  'environment': evaluate('JSON.stringify({java:String(Java.loadClass("java.lang.System").getProperty("java.version")),neoforge:"26.1.2.126",autojs:"1.0.1"})')}
        (output / 'report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
        print(json.dumps({'passed':result['passed'],'report':str(output / 'report.json')}))
        for script, extra in [('verify-item-tooltips.py',['--game-port','25589','--language','en_us']),
                              ('verify-item-tooltips.py',['--game-port','25589','--language','zh_cn']),
                              ('verify-effect-display.py',['--game-port','25589','--public-events']),
                              ('verify-presentation-runtime.py',[]),
                              ('verify-control-client.py',[])]:
            completed=subprocess.run([sys.executable,'-X','utf8',str(ROOT/'scripts'/script),'--server-port','25587','--client-port','25588',*extra],capture_output=True,text=True,encoding='utf-8')
            (output/(script+'-'+str(extra)+'.log').replace(':','_')).write_text(completed.stdout+completed.stderr,encoding='utf-8')
            print(json.dumps({'script':script,'returncode':completed.returncode,'tail':(completed.stdout+completed.stderr)[-1600:]}),flush=True)
            if completed.returncode: raise RuntimeError(script+' failed')

    finally:
        for side in [True, False]:
            try: evaluate('Java.loadClass("net.minecraft.client.Minecraft").getInstance().stop();JSON.stringify({stopping:true})' if side else 'AutoJS.server().halt(false);JSON.stringify({stopping:true})', side)
            except (OSError, RuntimeError): pass
        for process in processes:
            try: process.wait(timeout=30)
            except subprocess.TimeoutExpired: process.terminate()
        for log in logs: log.close()

if __name__ == '__main__': main()
