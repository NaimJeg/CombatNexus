"""Static 26.1.2 resource and Mixin callsite audit; never starts Minecraft.

Pass the resolved Minecraft sources+classes jar and NeoForge universal jar from
Gradle's artifact cache. No machine-specific dependency paths are stored here.
"""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--minecraft-jar", type=Path, required=True)
    parser.add_argument("--neoforge-jar", type=Path, required=True)
    args = parser.parse_args()
    tags, types = {}, set()
    for path in (args.minecraft_jar, args.neoforge_jar):
        with zipfile.ZipFile(path) as archive:
            for name in archive.namelist():
                parts = name.split("/")
                if not name.endswith(".json") or parts[0] != "data":
                    continue
                if len(parts) == 4 and parts[2] == "damage_type":
                    types.add(parts[1] + ":" + parts[3][:-5])
                if len(parts) == 5 and parts[2:4] == ["tags", "damage_type"]:
                    data = json.loads(archive.read(name))
                    key = parts[1] + ":" + parts[4][:-5]
                    if data.get("replace"):
                        tags[key] = []
                    tags.setdefault(key, []).extend(data["values"])
    data_root = ROOT / "common/src/main/resources/data/combatnexus"
    for path in (data_root / "tags/damage_type").glob("*.json"):
        tags["combatnexus:" + path.stem] = json.loads(path.read_text())["values"]

    def expand(tag, visiting=()):
        assert tag not in visiting, f"Cyclic tag: {tag}"
        assert tag in tags, f"Missing required tag: {tag}"
        result = set()
        for value in tags[tag]:
            required = value.get("required", True) if isinstance(value, dict) else True
            value = value["id"] if isinstance(value, dict) else value
            if value.startswith("#"):
                if required or value[1:] in tags:
                    result.update(expand(value[1:], (*visiting, tag)))
            elif value in types:
                result.add(value)
            else:
                assert not required, f"Missing damage type: {value}"
        return result

    channels = sorted((json.loads(p.read_text()) for p in (data_root / "combatnexus_channels").glob("*.json")),
                      key=lambda c: (-c["priority"], c["channel"]))
    matrix = json.loads((ROOT / "docs/native-damage-semantics.json").read_text())
    assert {row["id"] for row in matrix["damage_types"]} == types
    for row in matrix["damage_types"]:
        matches = [c["channel"] for c in channels if any(row["id"] in expand(t) for t in c["trigger_tags"])]
        assert row["channel"] == (matches[0] if matches else "combatnexus:untyped"), row
        assert row["managed"] == (row["id"] not in expand("minecraft:bypasses_invulnerability")), row
        actual = [name for name in ("melee", "projectile", "explosion", "environmental", "spear_attack", "mace_smash")
                  if row["id"] in expand("combatnexus:is_" + name)]
        assert row["categories"] == actual, row
    assert "minecraft:spear" not in expand("combatnexus:is_spear_stab")
    assert "minecraft:spear" not in expand("combatnexus:is_spear_charge")

    javap = str(Path(os.environ["JAVA_HOME"]) / ("bin/javap.exe" if os.name == "nt" else "bin/javap")) if "JAVA_HOME" in os.environ else "javap"
    mixins = ROOT / "targets/neoforge-26.1.2/src/main/java/io/github/naimjeg/combatnexus/combat/healing/platform/mixin"
    declared, covered = set(), set()
    for path in mixins.glob("*SourceMixin.java"):
        code = path.read_text()
        cls = re.search(r'@Mixin\(targets = "([^"]+)"\)', code)[1]
        disassembly = subprocess.run([javap, "-p", "-c", "-classpath", str(args.minecraft_jar), cls],
                                     check=True, capture_output=True, text=True).stdout
        methods, current = {}, None
        for line in disassembly.splitlines():
            if re.match(r"^  [^ ]", line) and "(" in line:
                current = line.split("(")[0].split()[-1]
                methods.setdefault(current, [])
            if current:
                methods[current].append(line)
        for method, owner, count in re.findall(
                r'@WrapOperation\(method = "([^"]+)", at = @At\(value = "INVOKE", target = "L([^;]+);heal\(F\)V"\), require = (\d+)\)', code):
            assert method in methods, (cls, method)
            lines = methods[method]
            needle = "Method " + ("" if owner.replace("/", ".") == cls else owner + ".") + "heal:(F)V"
            actual = sum(needle in line for line in lines)
            assert actual == int(count), (cls, method, actual, count)
            declared.add((cls, method))
        covered.add(cls)
    # Every native heal callsite must have a source wrapper or an explicit construction exception.
    with zipfile.ZipFile(args.minecraft_jar) as archive:
        assert json.loads(archive.read("version.json"))["id"] == matrix["minecraft"]
        callers = set()
        for name in archive.namelist():
            if name.startswith("net/minecraft/") and name.endswith(".java"):
                if re.search(r"\.heal\(", archive.read(name).decode()):
                    callers.add(name[:-5].replace("/", "."))
        exception = "net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces"
        assert callers == covered | {exception}, (callers - covered, covered - callers)
    print(f"PASS static only: {len(types)} damage types; {len(declared)} native healing method callsites; worldgen initialization exception")

if __name__ == "__main__":
    main()
