"""Compare identical scripted workloads on isolated HEAD and refactored servers; JFR is sampled."""
import argparse
import json
from pathlib import Path
import statistics
import time
import urllib.request

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--baseline-run", type=Path, required=True)
parser.add_argument("--baseline-port", type=int, default=25590)
parser.add_argument("--current-port", type=int, default=25590)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
current_run = root / "targets/neoforge-26.1.2/build/boundary-run"

def evaluate(port, code):
    request = urllib.request.Request(f"http://127.0.0.1:{port}/mcp", json.dumps({
        "jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"eval_js","arguments":{
            "side":"server","timeout_ms":10000,"code":code}}}).encode(), {"Content-Type":"application/json"})
    with urllib.request.urlopen(request, timeout=20) as response:
        result = json.load(response)
    assert "error" not in result, result
    content = result["result"]
    assert not content.get("isError"), content
    value = json.loads(content["content"][0]["text"])
    assert value["ok"] and not value.get("diagnostics"), value
    return json.loads(value["value"])

def reload(port, enable):
    return evaluate(port, """(function(){
        var server=AutoJS.server();server.getPackRepository().reload();
        var packs=new (Java.loadClass("java.util.ArrayList"))(server.getPackRepository().getSelectedIds());
        packs.remove("file/architecture-profile");
        """ + ('packs.add("file/architecture-profile");' if enable else '') + """
        var future=server.reloadResources(packs);
        return JSON.stringify({done:future.isDone(),failed:future.isCompletedExceptionally()});
    })()""")

reports = {}
for label, run, port in (("baseline", args.baseline_run.resolve(), args.baseline_port),
                         ("current", current_run, args.current_port)):
    pack = run / "world/datapacks/architecture-profile"
    pack.mkdir(parents=True, exist_ok=True)
    (pack / "pack.mcmeta").write_text(json.dumps({"pack":{"description":"Architecture allocation workload",
        "min_format":[101,1],"max_format":[101,1]}}), encoding="utf-8")
    directory=pack/"data/combatnexus/combatnexus_rules"
    directory.mkdir(parents=True,exist_ok=True)
    (directory/"profile.json").write_text(json.dumps({"id":"combatnexus:profile",
        "role":"any","phase":"global_adjustment","operations":[
        {"type":"combatnexus:add_global_pre_multiplier","value":0.005} for _ in range(32)]}), encoding="utf-8")
    settings_directory=pack/"data/combatnexus/settings"
    settings_directory.mkdir(parents=True,exist_ok=True)
    settings=json.loads((root/"common/src/main/resources/data/combatnexus/settings/damage.json").read_text(encoding="utf-8"))
    settings["limits"]["max_managed_requests_per_server_tick"]=100000
    (settings_directory/"damage.json").write_text(json.dumps(settings),encoding="utf-8")
    try:
        assert reload(port, True) == {"done":True,"failed":False}
        deadline=time.monotonic()+30
        prepare=(root/"scripts/autojs/prepare-architecture.js").read_text(encoding="utf-8")
        while not evaluate(port,prepare)["prepared"]:
            assert time.monotonic()<deadline
            time.sleep(.2)
        code=(root/"scripts/autojs/measure-architecture.js").read_text(encoding="utf-8")
        for _ in range(5):
            evaluate(port,code) # warm-up excluded
        samples=[]
        for _ in range(5):
            time.sleep(.1)
            samples.append(evaluate(port,code))
        channel=evaluate(port,(root/"scripts/autojs/measure-channel-ownership.js").read_text(encoding="utf-8"))
        assert channel["status"]=="APPLIED",channel
        reports[label]={"channelOwnershipProbe":channel,"samples":samples,"medianSampledBytesPerRequest":statistics.median(
            s["sampledAllocatedBytesPerRequest"] for s in samples),"medianP95ns":statistics.median(
            s["p95ns"] for s in samples)}
    finally:
        assert reload(port, False) == {"done":True,"failed":False}
reports["method"]="500 requests/sample, five warmups plus five measured samples, 32 operations, JFR allocation weights with the first per-recording weight excluded to avoid pre-recording allocation carry; Rhino and dispatch overhead included."
assert reports["baseline"]["samples"][0]["totalDamage"] == reports["current"]["samples"][0]["totalDamage"], reports
print(json.dumps(reports,ensure_ascii=False,indent=2))
