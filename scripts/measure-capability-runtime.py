"""Measure 20/100/500 live-target bursts; launch baseline and current separately with the same init script."""
import argparse
import importlib.util
import json
from pathlib import Path
import statistics
import time

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument("--port",type=int,default=25590)
parser.add_argument("--label",required=True)
parser.add_argument("--output",type=Path,required=True)
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location("verification",root/"scripts/verify-capabilities-runtime.py")
v=importlib.util.module_from_spec(spec);spec.loader.exec_module(v)
while not v.evaluate(args.port,(root/"scripts/autojs/prepare-architecture.js").read_text(encoding="utf-8"))["prepared"]:time.sleep(.2)
code=(root/"scripts/autojs/measure-capability-load.js").read_text(encoding="utf-8")
report={"label":args.label,"method":"Three warmups and five samples per load; one attack per live target in one server task. Entity creation excluded; Rhino/request/dispatch included. JFR allocation samples exclude the first carry-over weight; null means insufficient samples. Bursts are not sustained tick throughput.","loads":[]}
for load in (20,100,500):
    request="var LOAD="+str(load)+";\n"+code
    for _ in range(3):v.evaluate(args.port,request);time.sleep(.1)
    samples=[]
    for _ in range(5):samples.append(v.evaluate(args.port,request));time.sleep(.1)
    assert all(s["damage"]==load*2 for s in samples),samples
    allocated=[s["sampledBytesPerRequest"] for s in samples if s["sampledBytesPerRequest"] is not None]
    report["loads"].append({"targets":load,"medianP95ns":statistics.median(s["p95ns"] for s in samples),
        "medianSampledBytesPerRequest":statistics.median(allocated) if allocated else None,"samples":samples})
args.output.parent.mkdir(parents=True,exist_ok=True)
args.output.write_text(json.dumps(report,indent=2),encoding="utf-8")
print(json.dumps({"label":args.label,"loads":[{k:v for k,v in s.items() if k!="samples"} for s in report["loads"]]},indent=2))
