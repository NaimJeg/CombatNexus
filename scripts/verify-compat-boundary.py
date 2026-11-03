"""Check the independently packaged compat mod and its public SDK dependency boundary."""
from pathlib import Path
from io import BytesIO
import json
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "targets/neoforge-26.1.2"
COMPAT = TARGET / "combatnexus_compat"
INTERNAL = re.compile(rb"io/github/naimjeg/combatnexus/(?!compat/)[\w/]+/(?:internal|core|spi|registry)/")

for source in [*(ROOT / "common/src/main/java").rglob("*.java"), *(TARGET / "src/main/java").rglob("*.java")]:
    code = source.read_text(encoding="utf-8")
    assert "dev.shadowsoffire" not in code and "combatnexus.compat" not in code, source
for source in (COMPAT / "src/main/java").rglob("*.java"):
    assert not INTERNAL.search(source.read_bytes().replace(b".", b"/")), source

artifact = next(p for p in (TARGET / "build/libs").glob("*.jar") if not p.name.endswith(("-sources.jar", "-sdk-example.jar")))
with zipfile.ZipFile(artifact) as outer:
    names = outer.namelist()
    assert not any(n.startswith("io/github/naimjeg/combatnexus/compat/") for n in names)
    assert not any(n.startswith("dev/shadowsoffire/") for n in names)
    metadata = json.loads(outer.read("META-INF/jarjar/metadata.json"))
    nested = [entry["path"] for entry in metadata["jars"]]
    assert len(nested) == 1, nested
    with zipfile.ZipFile(BytesIO(outer.read(nested[0]))) as inner:
        assert b'"combatnexus_compat"' in inner.read("META-INF/neoforge.mods.toml")
        for name in inner.namelist():
            assert not name.startswith("dev/shadowsoffire/"), name
            if name.endswith(".class"):
                assert name.startswith("io/github/naimjeg/combatnexus/compat/"), name
                assert not INTERNAL.search(inner.read(name)), name
print("PASS: core/compat source and bytecode boundaries; exactly one nested compat; no bundled third-party classes")
