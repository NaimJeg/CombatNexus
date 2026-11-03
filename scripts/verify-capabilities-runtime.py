"""Run the CombatNexus AutoJS acceptance suites and retain versioned evidence."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "targets/neoforge-26.1.2"
SCRIPTS = ROOT / "scripts/autojs"
SUITES = {
    "effect-foundation": ["combat-scope", "food-healing-boundary", "effect-lifetimes", "instant-effects", "effect-damage-floor", "effect-receipts", "effect-phases", "combat-scheduling", "native-effect-ownership", "environment-effects", "environment-facts", "effect-extension", "effect-behaviors", "native-effect-failures", "domain-definitions"],
    "vanilla-integration": ["vanilla-integration"],
    "platform-lifecycle": ["runtime-query-boundary", "native-healing-failure", "recovery-and-lifecycle", "ownership-boundaries"],
    "semantic": ["semantic-extension"],
    "bridges": ["bridge-plans", "native-commands", "compat-scenes"],
    "compat-scenes": ["compat-scenes"],
    "native-commands": ["native-commands"],
    "bridge-plans": ["bridge-plans"],
    "sdk": ["sdk"],
    "core": ["architecture", "effect-receipts", "healing-evidence", "common-boundary", "native-effect-failures",
             "kernel-and-resources", "recovery-and-lifecycle", "settings-snapshot", "native-healing-failure", "callback-failures"],
    "capabilities": ["capabilities", "support-policy", "domain-definitions", "domain-pipelines", "behavior-sources",
                     "combat-reactions", "preapplication-decisions", "action-trigger-policy", "combat-scheduling",
                     "ownership-boundaries", "runtime-query-boundary"],
    "native": ["native-damage-semantics", "native-healing"],
}

def evaluate(port, code, timeout=30000, side="server"):
    payload = {"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "eval_js",
        "arguments": {"side": side, "code": code, "timeout_ms": timeout}}}
    request = urllib.request.Request(f"http://127.0.0.1:{port}/mcp", json.dumps(payload).encode(), {"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=timeout / 1000 + 10) as response:
        result = json.load(response)
    if "error" in result: raise RuntimeError(result["error"])
    execution = json.loads(result["result"]["content"][0]["text"])
    if not execution.get("ok") or execution.get("diagnostics"): raise RuntimeError(execution)
    return json.loads(execution["value"])

def verify_natural_ticks(port):
    # The observer must not call beginTick: it checks the native world-clock integration.
    import uuid
    key = "combatnexus.verify.tick." + uuid.uuid4().hex
    code = '''(function(){
        var level=AutoJS.server().overworld(),System=Java.loadClass("java.lang.System"),key=KEY;
        var cow=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        cow.setPos(0,512,0);cow.setNoAi(true);cow.setNoGravity(true);level.addFreshEntity(cow);cow.setHealth(1);
        var scheduled=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        scheduled.setPos(1,512,0);scheduled.setNoAi(true);scheduled.setNoGravity(true);level.addFreshEntity(scheduled);scheduled.setHealth(1);
        var api=Java.loadClass("io.github.naimjeg.combatnexus.combat.api.CombatAccess").find(level).orElseThrow(),UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional");
        var Task=Java.loadClass("io.github.naimjeg.combatnexus.combat.api.scheduling.ScheduledTask"),Heal=Java.loadClass("io.github.naimjeg.combatnexus.combat.api.scheduling.ScheduledIntent$Heal"),Absorption=Java.loadClass("io.github.naimjeg.combatnexus.combat.api.scheduling.ScheduledIntent$Absorption");
        var source=new (Java.loadClass("io.github.naimjeg.combatnexus.combat.api.CombatSource"))(Java.loadClass("net.minecraft.resources.Identifier").parse("combatnexus:scheduled_probe"),Optional.empty(),Optional.empty());
        api.scheduler().schedule(UUID.randomUUID(),scheduled.getUUID(),new Task(UUID.randomUUID(),source,Optional.empty(),level.getGameTime()+1,2,3,new Heal(2,Java.loadClass("io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest$Reason").SKILL),Java.loadClass("java.util.Set").of(),false));
        api.scheduler().schedule(UUID.randomUUID(),scheduled.getUUID(),new Task(UUID.randomUUID(),source,Optional.empty(),level.getGameTime()+1,1,1,new Absorption(4,5),Java.loadClass("java.util.Set").of(),false));
        var bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS,count=0;
        System.setProperty(key,"running");
        var listener=new (Java.loadClass("java.util.function.Consumer"))({accept:function(event){
            if(event.getLevel()!==level)return;
            try {
                if(System.getProperty(key)!=="running"){bus.unregister(listener);cow.discard();scheduled.discard();return;}
                cow.heal(0.25);count++;
                if(count===20){if(Math.abs(cow.getHealth()-6)>0.001)throw new Error("Natural tick healing mismatch: " + cow.getHealth());
                    if(Math.abs(scheduled.getHealth()-7)>0.001)throw new Error("Periodic command count mismatch: "+scheduled.getHealth());
                    if(scheduled.getAbsorptionAmount()!==4)throw new Error("Once-only absorption mismatch");
                    if(!api.scheduler().snapshot(scheduled.getUUID()).isEmpty())throw new Error("Completed schedules retained");
                    System.setProperty(key,"passed");bus.unregister(listener);cow.discard();scheduled.discard();}
            }catch(error){System.setProperty(key,"failed:"+error);bus.unregister(listener);cow.discard();scheduled.discard();}
        }});
        bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").NORMAL,false,
            Java.loadClass("net.neoforged.neoforge.event.tick.LevelTickEvent$Post"),listener);
        return JSON.stringify({installed:true});
    })()'''.replace("KEY", json.dumps(key))
    evaluate(port, code)
    try:
        deadline = time.monotonic() + 20
        while True:
            state = evaluate(port, 'JSON.stringify({state:Java.loadClass("java.lang.System").getProperty(' + json.dumps(key) + ')})')["state"]
            if state == "passed": return {"passed": 23, "checks": ["twenty natural world ticks heal after native clock advance without query-driven lifecycle", "periodic scheduled healing fires three times", "one-shot absorption fires once", "completed schedules removed"]}
            if state != "running" or time.monotonic() > deadline: raise RuntimeError(state)
            time.sleep(.1)
    finally:
        evaluate(port, 'Java.loadClass("java.lang.System").clearProperty(' + json.dumps(key) + ');JSON.stringify({cleared:true})')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=25590)
    parser.add_argument("--game-port", type=int, default=25566)
    parser.add_argument("--suite", choices=["all", *SUITES], default="all")
    parser.add_argument("--start", action="store_true")
    parser.add_argument("--restart", action="store_true", help="Verify entity persistence through a real server restart; requires --start")
    parser.add_argument("--reload", action="store_true", help="Verify atomic domain reloads in the started disposable world")
    parser.add_argument("--compatibility", action="store_true")
    parser.add_argument("--attributes-only", action="store_true")
    parser.add_argument("--packaged", action="store_true", help="Load core and nested compat from the final Jar")
    parser.add_argument("--syntax-only", action="store_true")
    args = parser.parse_args()
    if args.compatibility and args.attributes_only: parser.error("Choose only one optional-mod profile")
    if (args.restart or args.reload) and not args.start: parser.error("--restart/--reload requires --start")
    if args.syntax_only:
        for script in sorted(SCRIPTS.glob("*.js")):
            subprocess.run(["node", "--check", str(script)], check=True)
        for script in (ROOT / "scripts").glob("*.py"):
            import ast
            ast.parse(script.read_text(encoding="utf-8"), filename=str(script))
        print("PASS: AutoJS and Python syntax")
        return 0
    profile = "compatibility" if args.compatibility else "isolated"
    if args.attributes_only: profile = "attributes-only"
    if args.packaged: profile += "-packaged"
    output = TARGET / "build/verification" / profile
    output.mkdir(parents=True, exist_ok=True)
    process = None
    log = None
    results = []
    try:
        if args.start:
            run_dir = TARGET / "build" / ("acceptance-" + profile)
            run_dir.mkdir(parents=True, exist_ok=True)
            (run_dir / "eula.txt").write_text("eula=true\n", encoding="utf-8")
            (run_dir / "server.properties").write_text("online-mode=false\nserver-ip=127.0.0.1\nspawn-protection=0\npause-when-empty-seconds=-1\n", encoding="utf-8")
            log = (output / "server.log").open("w", encoding="utf-8")
            wrapper = TARGET / ("gradlew.bat" if os.name == "nt" else "gradlew")
            command = [str(wrapper), "runServer", "-I", str(SCRIPTS / "server.init.gradle"),
                f"-PverificationGamePort={args.game_port}", f"-PverificationAutojsPort={args.port}",
                f"-PverificationCompatibility={str(args.compatibility).lower()}",
                f"-PverificationAttributesOnly={str(args.attributes_only).lower()}",
                f"-PverificationPackaged={str(args.packaged).lower()}",
                f"-PverificationRunDirectory=build/acceptance-{profile}", "--console", "plain"]
            if args.suite in ("effect-foundation", "vanilla-integration", "platform-lifecycle", "all"):
                command.extend(["-I", str(SCRIPTS / "effect-extension.init.gradle")])
            process = subprocess.Popen(command, cwd=TARGET, stdout=log, stderr=subprocess.STDOUT,
                creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
            deadline = time.monotonic() + 180
            while True:
                if process.poll() is not None: raise RuntimeError("Server exited; inspect server.log")
                try:
                    evaluate(args.port, 'JSON.stringify({ready:true})')
                    break
                except (OSError, RuntimeError):
                    if time.monotonic() >= deadline: raise RuntimeError("Server startup timeout")
                    time.sleep(.5)
        deadline = time.monotonic() + 30
        while not evaluate(args.port, (SCRIPTS / "prepare-architecture.js").read_text(encoding="utf-8"))["prepared"]:
            if time.monotonic() > deadline: raise RuntimeError("Native entity lookup not ready")
            time.sleep(.2)
        names = list(dict.fromkeys(name for group, names in SUITES.items() if args.suite in ("all", group) for name in names))
        for name in names:
            script = SCRIPTS / ("verify-" + name + ".js")
            try:
                code = script.read_text(encoding="utf-8")
                if "// MATRIX" in code:
                    matrix = json.loads((ROOT / "docs/legacy/native-damage-semantics.json").read_text(encoding="utf-8"))["damage_types"]
                    code = code.replace("// MATRIX", "matrix = " + json.dumps(matrix) + ";")
                result = evaluate(args.port, "var COMBATNEXUS_COMPATIBILITY=" + str(args.compatibility).lower()
                    + ";var COMBATNEXUS_PACKAGED=" + str(args.packaged).lower() + ";\n" + code)
                record = {"script": str(script.relative_to(ROOT)), "ok": True, **result}
            except Exception as error:
                record = {"script": str(script.relative_to(ROOT)), "ok": False, "error": str(error)}
            results.append(record)
            (output / (name + ".json")).write_text(json.dumps(record, ensure_ascii=False, indent=2), encoding="utf-8")
            print(json.dumps({"script": record["script"], "ok": record["ok"], "passed": record.get("passed", 0), "error": record.get("error", "")[:600]}, ensure_ascii=False), flush=True)
        try:
            record = {"script":"natural-world-tick observer", "ok":True, **verify_natural_ticks(args.port)}
        except Exception as error:
            record = {"script":"natural-world-tick observer", "ok":False, "error":str(error)}
        results.append(record)
        (output / "natural-ticks.json").write_text(json.dumps(record, indent=2), encoding="utf-8")
        print(json.dumps(record), flush=True)
        observers = ("effect-natural", "environment-cadence", "starvation-cadence") if args.suite in ("effect-foundation", "all") else ()
        if args.suite in ("vanilla-integration", "all"): observers += ("native-block-sources",)
        if observers:
            for observer in observers:
                installed = evaluate(args.port, (SCRIPTS / ("verify-" + observer + ".js")).read_text(encoding="utf-8"))
                deadline = time.monotonic() + 30
                while True:
                    record = evaluate(args.port, 'Java.loadClass("java.lang.System").getProperty(' + json.dumps(installed["key"]) + ')')
                    if not record.get("running"): break
                    if time.monotonic() > deadline: raise RuntimeError("Effect natural tick observer timeout")
                    time.sleep(.2)
                record.update(script="scripts/autojs/verify-" + observer + ".js", ok="error" not in record)
                results.append(record)
                (output / (observer + ".json")).write_text(json.dumps(record, indent=2), encoding="utf-8")
                print(json.dumps(record), flush=True)
        if args.reload:
            reload_result = subprocess.run([sys.executable, str(ROOT / "scripts/verify-behavior-runtime.py"),
                "--port", str(args.port), "--definitions", "--definitions-only", "--run-directory", str(run_dir)],
                capture_output=True, text=True, encoding="utf-8")
            (output / "reload.log").write_text(reload_result.stdout + reload_result.stderr, encoding="utf-8")
            record = {"script":"scripts/verify-behavior-runtime.py --definitions-only", "ok":reload_result.returncode == 0,
                "passed":sum(json.loads(line).get("passed", 0) for line in reload_result.stdout.splitlines()) if reload_result.returncode == 0 else 0}
            results.append(record); print(json.dumps(record), flush=True)
        if args.restart:
            restart_name = "effect-restart" if args.suite == "effect-foundation" else "persistence-restart"
            prepared = evaluate(args.port, (SCRIPTS / ("prepare-" + restart_name + ".js")).read_text(encoding="utf-8"))
            (output / "persistence-before.json").write_text(json.dumps(prepared, indent=2), encoding="utf-8")
            evaluate(args.port, 'AutoJS.server().halt(false); JSON.stringify({stopping:true})', 1000)
            process.wait(timeout=60)
            log.close()
            log = (output / "restart.log").open("w", encoding="utf-8")
            process = subprocess.Popen(command, cwd=TARGET, stdout=log, stderr=subprocess.STDOUT,
                creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
            deadline = time.monotonic() + 180
            while True:
                if process.poll() is not None: raise RuntimeError("Restart exited; inspect restart.log")
                try:
                    evaluate(args.port, 'AutoJS.server().overworld().getChunk(0,0); JSON.stringify({ready:true})')
                    break
                except (OSError, RuntimeError):
                    if time.monotonic() >= deadline: raise RuntimeError("Restart startup timeout")
                    time.sleep(.5)
            deadline = time.monotonic() + 30
            while True:
                record = evaluate(args.port, (SCRIPTS / ("verify-" + restart_name + ".js")).read_text(encoding="utf-8"))
                if record.get("ready"): break
                if time.monotonic() >= deadline: raise RuntimeError("Saved entity did not reload")
                time.sleep(.2)
            record.update(script="scripts/autojs/verify-" + restart_name + ".js", ok=True)
            results.append(record)
            (output / "persistence-restart.json").write_text(json.dumps(record, indent=2), encoding="utf-8")
            print(json.dumps(record), flush=True)
    finally:
        if process is not None:
            try: evaluate(args.port, 'AutoJS.server().halt(false); JSON.stringify({stopping:true})', 1000)
            except (OSError, RuntimeError): pass
            try: process.wait(timeout=60)
            except subprocess.TimeoutExpired:
                if os.name == "nt": subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"], check=False, capture_output=True)
                else: process.terminate()
        if log is not None: log.close()
        digest = hashlib.sha256()
        paths = subprocess.check_output(["git", "ls-files", "-co", "--exclude-standard", "-z"], cwd=ROOT).decode().split("\0")
        for name in sorted(set(paths)):
            path = ROOT / name
            if name and path.is_file() and "__pycache__" not in name:
                digest.update(name.encode()); digest.update(b"\0"); digest.update(path.read_bytes())
        report = {"commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
            "workspace_sha256": digest.hexdigest(), "profile": profile,
            "passed": sum(r.get("passed", 0) for r in results if r["ok"]), "scripts": results}
        (output / "summary.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    return 0 if results and all(r["ok"] for r in results) else 1

if __name__ == "__main__":
    raise SystemExit(main())
