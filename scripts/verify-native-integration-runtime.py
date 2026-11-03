"""Run native integration probes on an already started disposable AutoJS server. Never launches a server.

The healing probe resets the overworld combat session; do not use production worlds.
"""
import argparse
import json
from pathlib import Path
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=25590)
    parser.add_argument("--player", action="store_true", help="Also require the connected SettingsVerifier player")
    parser.add_argument("--reload", action="store_true", help="Reload current resources and repeat probes against the adopted generation")
    args = parser.parse_args()

    def evaluate(code):
        body = {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"eval_js",
                "arguments":{"side":"server","timeout_ms":30000,"code":code}}}
        request = urllib.request.Request(f"http://127.0.0.1:{args.port}/mcp", json.dumps(body).encode(),
                                         {"Content-Type":"application/json"})
        with urllib.request.urlopen(request, timeout=40) as response:
            envelope = json.load(response)
        assert "error" not in envelope, envelope
        result = envelope["result"]
        assert not result.get("isError"), result
        execution = json.loads(result["content"][0]["text"])
        assert execution.get("ok") and not execution.get("diagnostics"), execution
        return json.loads(execution["value"])

    def run(name):
        code = (ROOT / "scripts/autojs" / (name + ".js")).read_text(encoding="utf-8")
        if "// MATRIX" in code:
            matrix = json.loads((ROOT / "docs/native-damage-semantics.json").read_text())["damage_types"]
            code = code.replace("// MATRIX", "matrix = " + json.dumps(matrix) + ";")
        result = evaluate(code)
        print(json.dumps({"script":name, **result}, ensure_ascii=False))
        return result

    deadline = time.monotonic() + 30
    while not run("prepare-architecture")["prepared"]:
        assert time.monotonic() < deadline, "Native UUID lookup did not become ready"
        time.sleep(0.2)
    names = ["verify-native-damage-semantics", "verify-native-healing", "verify-native-healing-failure"]
    if args.player:
        names.append("verify-native-player-healing")
    for name in names:
        run(name)
    if args.reload:
        result = evaluate('(function(){var s=AutoJS.server(),D=Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions");'
                          'var before=D.forServer(s).generation();var f=s.reloadResources(s.getPackRepository().getSelectedIds());'
                          'return JSON.stringify({done:f.isDone(),failed:f.isCompletedExceptionally(),before:before,after:D.forServer(s).generation()});})()')
        assert result["done"] and not result["failed"] and result["after"] > result["before"], result
        for name in names:
            run(name)

if __name__ == "__main__":
    main()
