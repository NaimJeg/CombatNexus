"""Check the distributable baseline jar against the migration source manifest.

Run after the NeoForge target build. Does not read or build the reference repo.
"""
from pathlib import Path
from zipfile import ZipFile
import json
import re
import tomllib


def main():
    root = Path(__file__).resolve().parents[1]
    manifest = json.loads((root / 'docs/migration/damage-source-manifest.json').read_text())
    props = dict(re.findall(r'^(mod_\w+)=(.*)$', (root / 'gradle.properties').read_text(), re.M))
    jar = root / 'targets/neoforge-26.1.2/build/libs' / (
        f'{props["mod_name"]}-neoforge-26.1.2-{props["mod_version"]}.jar')
    with ZipFile(jar) as archive:
        names = archive.namelist()
        assert len(names) == len(set(names)), 'Duplicate jar entries'
        assert 'LICENSE' in names, 'Missing upstream license'
        for entry in manifest:
            path = Path(entry['destination'])
            assert (root / path).is_file(), f'Missing migrated source: {path}'
            compiled = path.as_posix().split('/src/main/java/', 1)[1].removesuffix('.java') + '.class'
            assert compiled in names, f'Missing migrated class: {compiled}'
        metadata = tomllib.loads(archive.read('META-INF/neoforge.mods.toml').decode())
        assert [m['modId'] for m in metadata['mods']] == ['combatnexus']
        deps = {d['modId']: d for d in metadata['dependencies']['combatnexus']}
        assert deps['minecraft']['versionRange'] == '[26.1.2]'
        assert deps['neoforge']['versionRange'] == '[26.1.2.126,26.1.3)'
        assert metadata['mixins'][0]['config'] == 'combatnexus.mixins.json'
        mixins = json.loads(archive.read('combatnexus.mixins.json'))
        assert mixins['required'] and mixins['injectors']['defaultRequire'] == 1
        for name in mixins['mixins']:
            assert mixins['package'].replace('.', '/') + '/' + name + '.class' in names
        for locale in ('en_us', 'zh_cn'):
            json.loads(archive.read(f'assets/combatnexus/lang/{locale}.json'))
        for name in names:
            assert not any(old in name for old in ('damagenexus', 'cc/sighs/temple', 'assets/temple'))
            if name.endswith(('.class', '.json', '.toml')):
                content = archive.read(name)
                assert b'damagenexus' not in content, f'Old namespace in {name}'
                assert b'DamageNexus' not in content, f'Old type name in {name}'
    print(f'PASS: {jar.name}; {len(manifest)} migrated classes, metadata, Mixin, resources and license')


if __name__ == '__main__':
    main()
