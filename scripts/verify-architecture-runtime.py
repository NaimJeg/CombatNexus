"""Run architecture assertions against an isolated AutoJS server after native lookup is ready."""
import argparse
import json
from pathlib import Path
import time
import urllib.request

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--port", type=int, default=25590)
args = parser.parse_args()
root = Path(__file__).resolve().parent / "autojs"

def evaluate(name):
    payload = {"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {
        "name": "eval_js", "arguments": {"side": "server", "timeout_ms": 10000,
                                      "code": (root / name).read_text(encoding="utf-8")}}}
    request = urllib.request.Request(f"http://127.0.0.1:{args.port}/mcp",
                                    json.dumps(payload).encode(), {"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=20) as response:
        result = json.load(response)
    assert "error" not in result, result
    result = result["result"]
    assert not result.get("isError"), result
    execution = json.loads(result["content"][0]["text"])
    assert execution.get("ok") and not execution.get("diagnostics"), execution
    return json.loads(execution["value"])

deadline = time.monotonic() + 30
while not evaluate("prepare-architecture.js")["prepared"]:
    assert time.monotonic() < deadline, "Native lookup area did not become ready"
    time.sleep(0.2)

total = 0
for name in ("verify-architecture.js", "verify-effect-receipts.js",
             "verify-healing-evidence.js", "verify-common-boundary.js",
             "verify-native-effect-failures.js", "verify-kernel-and-resources.js",
             "verify-recovery-and-lifecycle.js", "verify-settings-snapshot.js",
             "verify-native-healing-failure.js"):
    result = evaluate(name)
    total += result["passed"]
    print(json.dumps({"script": name, **result}, ensure_ascii=False))
print(json.dumps({"passed": total}))
