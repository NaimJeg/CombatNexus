"""Check the distributable jar against the active source set and supported target.

Run after the NeoForge target build. Does not read or build the reference repo.
"""
from pathlib import Path
from zipfile import ZipFile
import json
import re
import tomllib


def main():
    root = Path(__file__).resolve().parents[1]
    props = dict(re.findall(r'^(mod_\w+)=(.*)$', (root / 'gradle.properties').read_text(), re.M))
    jar = root / 'targets/neoforge-26.1.2/build/libs' / (
        f'{props["mod_name"]}-neoforge-26.1.2-{props["mod_version"]}.jar')
    with ZipFile(jar) as archive:
        names = archive.namelist()
        assert len(names) == len(set(names)), 'Duplicate jar entries'
        assert 'LICENSE' in names, 'Missing upstream license'
        # Removed migration records are historical; the active source sets define the artifact contract.
        source_count = 0
        source_roots = [root / 'common/src/main/java',
                        root / 'targets/neoforge-26.1.2/src/main/java']
        for source_root in source_roots:
            for source in source_root.rglob('*.java'):
                if source.name == 'package-info.java':
                    continue
                source_count += 1
                compiled = source.relative_to(source_root).as_posix().removesuffix('.java') + '.class'
                assert compiled in names, f'Missing current class: {compiled}'
                content = source.read_text(encoding='utf-8')
                assert 'io.github.naimjeg.combatnexus.damage' not in content, f'Old Java package: {source}'
                if source_root == source_roots[0]:
                    assert 'net.neoforged' not in content, f'Platform API leaked into common: {source}'
                    module = source.relative_to(source_root).parts[4]
                    if module in ('attribute', 'effect', 'formula', 'presentation'):
                        assert not re.search(r'import .*combat\.damage\.(core|internal|registry)\.', content), \
                            f'Cross-module implementation dependency: {source}'
        for filename in ('formulas', 'damage'):
            json.loads(archive.read(f'data/combatnexus/settings/{filename}.json'))
        for directory, sample in [('healing_rules', 'example_healing'), ('regeneration', 'field_recovery'),
                                  ('shields', 'physical_guard'), ('effects', 'agility')]:
            json.loads(archive.read(f'data/combatnexus/combatnexus_{directory}/{sample}.json'))
        assert not any(name.endswith('/EquipmentBehaviors.class') for name in names), 'Legacy equipment entry bundled'
        assert not any(name.endswith('ConfigSpec.class') for name in names), 'Old NeoForge config spec bundled'
        assert not any(name.endswith('/DamageModuleConfig.class') for name in names), 'Old config adapter bundled'
        metadata = tomllib.loads(archive.read('META-INF/neoforge.mods.toml').decode())
        assert [m['modId'] for m in metadata['mods']] == ['combatnexus']
        deps = {d['modId']: d for d in metadata['dependencies']['combatnexus']}
        assert deps['minecraft']['versionRange'] == '[26.1.2]'
        assert deps['neoforge']['versionRange'] == '[26.1.2.126,26.1.3)'
        assert metadata['mixins'][0]['config'] == 'combatnexus.mixins.json'
        assert {m['config'] for m in metadata['mixins']} == {'combatnexus.mixins.json', 'combatnexus.healing.mixins.json', 'combatnexus.control.mixins.json', 'combatnexus.health.mixins.json', 'combatnexus.effects.mixins.json'}
        for entry in metadata['mixins']:
            mixins = json.loads(archive.read(entry['config']))
            assert mixins['required'] and mixins['injectors']['defaultRequire'] == 1
            for name in mixins['mixins'] + mixins.get('client', []):
                assert mixins['package'].replace('.', '/') + '/' + name + '.class' in names
        for locale in ('en_us', 'zh_cn'):
            json.loads(archive.read(f'assets/combatnexus/lang/{locale}.json'))
        for name in names:
            assert not any(old in name for old in ('damagenexus', 'cc/sighs/temple', 'assets/temple'))
            if name.endswith(('.class', '.json', '.toml')):
                content = archive.read(name)
                assert b'damagenexus' not in content, f'Old namespace in {name}'
                assert b'DamageNexus' not in content, f'Old type name in {name}'
    print(f'PASS: {jar.name}; {source_count} active source classes, metadata, Mixin, resources and license')


if __name__ == '__main__':
    main()
