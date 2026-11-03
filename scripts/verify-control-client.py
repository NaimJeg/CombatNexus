"""Verify authoritative controls over real server/client payloads on a disposable connected client."""
import argparse
import json
from pathlib import Path
import time
import urllib.request

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--server-port", type=int, default=25590)
parser.add_argument("--client-port", type=int, default=25591)
args = parser.parse_args()

def evaluate(side, code):
    port = args.server_port if side == "server" else args.client_port
    request = urllib.request.Request(f"http://127.0.0.1:{port}/mcp", json.dumps({"jsonrpc":"2.0", "id":1,
        "method":"tools/call", "params":{"name":"eval_js", "arguments":{"side":side,"code":code,"timeout_ms":10000}}}).encode(),
        {"Content-Type":"application/json"})
    with urllib.request.urlopen(request, timeout=20) as response: envelope = json.load(response)
    assert "error" not in envelope, envelope
    result = json.loads(envelope["result"]["content"][0]["text"])
    assert result.get("ok") and not result.get("diagnostics"), result
    return json.loads(result["value"])

prefix = 'function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}'
server = prefix + '''var level=AutoJS.server().overworld(),p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");
if(p===null)throw new Error("Connect SettingsVerifier first");
var world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);cls("combat.spi.CombatWorldLifecycleAccess").beginTick(world);
var id=Java.loadClass("net.minecraft.resources.Identifier").parse("combatnexus:client_control_probe"),ES=cls("effect.api.EffectService");'''
checks = []
try:
    evaluate("server", "(function(){" + server + '''var EI=cls("effect.api.EffectInstance"),Action=cls("effect.api.EffectSnapshot$RestrictedAction");
    world.effects().apply(new EI(new (cls("effect.api.EffectSnapshot"))(p.getUUID(),id,id,0,Java.loadClass("java.util.OptionalLong").empty(),
        Java.loadClass("java.util.Set").copyOf(Java.loadClass("java.util.Arrays").asList(Action.values()))),Java.loadClass("java.util.List").of(),EI.Ownership.MANAGED),ES.Stacking.REPLACE);
    return JSON.stringify({installed:true});})()''')
    deadline = time.monotonic() + 10
    while True:
        state = evaluate("client", "(function(){" + prefix + '''var blocked=0;for(var action of cls("effect.api.EffectSnapshot$RestrictedAction").values())
            if(!cls("presentation.platform.client.CombatControlClient").allowed(action))blocked++;
            return JSON.stringify({blocked:blocked});})()''')
        if state["blocked"] == 7: break
        assert time.monotonic() < deadline, state
        time.sleep(.1)
    checks.append("seven server restrictions arrive on the real client")
    result = evaluate("client", "(function(){" + prefix + '''var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),p=mc.player;
        var yaw=p.getYRot();p.turn(120,60);var same=p.getYRot()===yaw;
        p.input.keyPresses=new (Java.loadClass("net.minecraft.world.entity.player.Input"))(true,true,true,true,true,false,true);
        cls("presentation.platform.client.CombatControlClient").input(new (Java.loadClass("net.neoforged.neoforge.client.event.MovementInputUpdateEvent"))(p,p.input));
        var keys=p.input.keyPresses;
        return JSON.stringify({turn:same,walk:!keys.forward()&&!keys.backward()&&!keys.left()&&!keys.right(),jump:!keys.jump(),vector:p.input.getMoveVector().lengthSquared()===0});})()''')
    assert all(result.values()), result
    checks.extend(["client turn intercepted", "movement keys cleared", "jump input cleared", "movement vector cleared"])
    result = evaluate("server", "(function(){" + server + '''var before=p.position(),yaw=p.getYRot();
        var Packet=Java.loadClass("net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$PosRot");
        p.connection.handleMovePlayer(new Packet(p.getX()+1,p.getY()+1,p.getZ()+1,yaw+90,p.getXRot(),p.onGround(),false));
        return JSON.stringify({position:p.position().equals(before),yaw:p.getYRot()===yaw});})()''')
    assert all(result.values()), result
    checks.append("server rejects restricted position and rotation packet")
    result = evaluate("client", "(function(){" + prefix + '''var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),old=mc.hitResult;
        var Event=Java.loadClass("net.neoforged.neoforge.client.event.InputEvent$InteractionKeyMappingTriggered"),Hand=Java.loadClass("net.minecraft.world.InteractionHand"),Control=cls("presentation.platform.client.CombatControlClient");
        try {
            mc.hitResult=null;
            var use=new Event(1,mc.options.keyUse,Hand.MAIN_HAND),mine=new Event(0,mc.options.keyAttack,Hand.MAIN_HAND);
            Control.action(use);Control.action(mine);
            mc.hitResult=new (Java.loadClass("net.minecraft.world.phys.EntityHitResult"))(mc.player);
            var attack=new Event(0,mc.options.keyAttack,Hand.MAIN_HAND);Control.action(attack);
            return JSON.stringify({use:!use.isCanceled(),mining:!mine.isCanceled(),attack:attack.isCanceled()});
        }finally{mc.hitResult=old;}})()''')
    assert all(result.values()), result
    checks.extend(["ordinary right click retained", "nonentity left click retained", "entity attack control retained"])

finally:
    evaluate("server", "(function(){" + server + '''world.effects().remove(p.getUUID(),id,id,ES.Removal.DISPELLED);return JSON.stringify({removed:true});})()''')
deadline = time.monotonic() + 10
while True:
    state = evaluate("client", "(function(){" + prefix + '''var allowed=0;for(var action of cls("effect.api.EffectSnapshot$RestrictedAction").values())
        if(cls("presentation.platform.client.CombatControlClient").allowed(action))allowed++;return JSON.stringify({allowed:allowed});})()''')
    if state["allowed"] == 7: break
    assert time.monotonic() < deadline, state
    time.sleep(.1)
checks.append("removal releases all client controls through network update")
report = {"passed":len(checks), "checks":checks}
output = Path(__file__).resolve().parents[1]/"targets/neoforge-26.1.2/build/verification/isolated/control-client.json"
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(report, indent=2), encoding="utf-8")
print(json.dumps(report, indent=2))
