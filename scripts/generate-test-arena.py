"""Regenerate the empty 16 x 8 x 16 GameTest structure (stdlib only).

Minecraft's built-in empty structure is 1 x 1 x 1. Our tests place entities
outside that volume; a real bounding box is required for ticking and isolation.
This resource has no blocks, entities, loot or gameplay registrations.
"""
from pathlib import Path
import gzip
import struct


def utf(value):
    encoded = value.encode('utf-8')
    return struct.pack('>H', len(encoded)) + encoded


def tag(kind, name, payload):
    return bytes([kind]) + utf(name) + payload


def main():
    nbt = b'\x0a\x00\x00'  # unnamed compound root
    nbt += tag(9, 'size', b'\x03' + struct.pack('>4i', 3, 16, 8, 16))
    for name in ('entities', 'blocks', 'palette'):
        nbt += tag(9, name, b'\x0a' + struct.pack('>i', 0))
    nbt += tag(3, 'DataVersion', struct.pack('>i', 4790))
    nbt += b'\x00'
    root = Path(__file__).resolve().parents[1]
    target = root / 'targets/neoforge-26.1.2/src/main/resources/data/combatnexus/structure/test_arena.nbt'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(gzip.compress(nbt, mtime=0))
    print(target.relative_to(root))


if __name__ == '__main__':
    main()
