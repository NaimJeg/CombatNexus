"""Run behavior acceptance scripts using the existing AutoJS runner on an explicitly started disposable server."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import time

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--port', type=int, default=25590)
parser.add_argument('--definitions', action='store_true', help='Also exercise aggregate datapack adoption in the disposable world')
parser.add_argument('--definitions-only', action='store_true')
parser.add_argument('--run-directory', type=Path, default=ROOT / 'targets/neoforge-26.1.2/build/boundary-run')
args = parser.parse_args()


def run(script):
    result = subprocess.run([sys.executable, str(ROOT / 'scripts/run-autojs.py'), str(script),
                             '--port', str(args.port), '--timeout-ms', '30000'], text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stdout + result.stderr)
    envelope = json.loads(result.stdout)
    value = json.loads(envelope['value'])
    print(json.dumps({'script': str(script.relative_to(ROOT)), **value}, ensure_ascii=False))
    return value


deadline = time.monotonic() + 30
while not run(ROOT / 'scripts/autojs/prepare-architecture.js')['prepared']:
    assert time.monotonic() < deadline, 'Native lookup area did not become ready'
    time.sleep(0.2)

for name in ('verify-ownership-boundaries', 'verify-domain-pipelines', 'verify-combat-reactions', 'verify-behavior-sources',
             'verify-domain-definitions', 'verify-preapplication-decisions', 'verify-combat-scheduling', 'verify-action-trigger-policy'):
    if not args.definitions_only: run(ROOT / 'scripts/autojs' / (name + '.js'))

if args.definitions:
    pack_root = args.run_directory / 'world/datapacks'
    assert pack_root.is_dir(), 'Use the disposable server.init.gradle world'
    scripts = ROOT / 'targets/neoforge-26.1.2/build/verification'
    scripts.mkdir(parents=True, exist_ok=True)
    probe = scripts / 'behavior-reload-probe.js'

    def evaluate(code):
        probe.write_text(code, encoding='utf-8')
        return run(probe)

    def state():
        return evaluate('(function(){var d=Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions").forServer(AutoJS.server());'
                        'return JSON.stringify({generation:d.generation(),count:d.behaviors().size(),healing:d.domains().healingRules().size(),regeneration:d.domains().regeneration().size(),shields:d.domains().shields().size(),effects:d.domains().effects().size()});})()')

    checks = []
    initial = state()
    with tempfile.TemporaryDirectory(prefix='behavior-verification-', dir=pack_root) as temporary:
        pack = Path(temporary).resolve()
        assert pack.is_relative_to(pack_root.resolve())
        name = 'file/' + pack.name
        (pack / 'pack.mcmeta').write_text(json.dumps({'pack': {'description': 'Behavior definition verification',
                                                            'min_format': [101, 1], 'max_format': [101, 1]}}))
        directory = pack / 'data/combatnexus/combatnexus_behaviors'
        directory.mkdir(parents=True)
        sample = json.loads((ROOT / 'common/src/main/resources/data/combatnexus/combatnexus_behaviors/periodic_healing.json').read_text())
        data = directory / 'verification.json'
        data.write_text(json.dumps(sample))
        domain_files = {}
        for domain, filename in [('healing_rules', 'example_healing'), ('regeneration', 'field_recovery'), ('shields', 'physical_guard'), ('effects', 'agility')]:
            original_file = ROOT / f'common/src/main/resources/data/combatnexus/combatnexus_{domain}/{filename}.json'
            target_file = pack / f'data/combatnexus/combatnexus_{domain}/verification.json'
            target_file.parent.mkdir(parents=True, exist_ok=True)
            target_file.write_text(original_file.read_text(), encoding='utf-8')
            domain_files[domain] = (target_file, json.loads(original_file.read_text()))

        def reload(enabled):
            return evaluate('(function(){var s=AutoJS.server();s.getPackRepository().reload();'
                            'var p=new (Java.loadClass("java.util.ArrayList"))(s.getPackRepository().getSelectedIds());p.remove(' + json.dumps(name) + ');'
                            + ('p.add(' + json.dumps(name) + ');' if enabled else '')
                            + 'var f=s.reloadResources(p);return JSON.stringify({done:f.isDone(),failed:f.isCompletedExceptionally()});})()')

        try:
            assert reload(True) == {'done': True, 'failed': False}
            adopted = state()
            assert adopted['generation'] > initial['generation'] and adopted['count'] == initial['count'] + 1
            for key in ('healing', 'regeneration', 'shields', 'effects'):
                assert adopted[key] == initial[key] + 1, key
            checks.append('valid aggregate domain batch adopted')
            for domain, (target_file, content) in domain_files.items():
                bad = dict(content)
                bad['misspelled_field'] = True
                target_file.write_text(json.dumps(bad))
                assert reload(True) == {'done': True, 'failed': True}
                assert state() == adopted, domain + ' invalid batch published'
                checks.append(domain + ' invalid batch retains generation')
                target_file.write_text(json.dumps(content))
            target_file, content = domain_files['effects']
            bad = dict(content, behaviors=['combatnexus:missing'])
            target_file.write_text(json.dumps(bad))
            assert reload(True) == {'done': True, 'failed': True}
            assert state() == adopted, 'Missing behavior reference adopted'
            checks.append('Missing behavior reference rejected')
            target_file.write_text(json.dumps(content))
            target_file, content = domain_files['shields']
            bad = dict(content, damage_type_tags=['combatnexus:missing'])
            target_file.write_text(json.dumps(bad))
            assert reload(True) == {'done': True, 'failed': True}
            assert state() == adopted, 'Missing shield tag adopted'
            checks.append('Missing shield tag rejected')
            target_file.write_text(json.dumps(content))
            bad = dict(content, channels=['combatnexus:missing'])
            target_file.write_text(json.dumps(bad))
            assert reload(True) == {'done': True, 'failed': True}
            assert state() == adopted, 'Missing shield channel adopted'
            checks.append('Missing shield channel rejected')
            target_file.write_text(json.dumps(content))
            target_file, content = domain_files['effects']
            bad = dict(content, restrictions=['WALK', 'WALK'])
            target_file.write_text(json.dumps(bad))
            assert reload(True) == {'done': True, 'failed': True}
            assert state() == adopted, 'Duplicate restriction adopted'
            checks.append('Duplicate restriction rejected')
            target_file.write_text(json.dumps(content))
            sample['components'][0]['interval_ticks'] = 0
            data.write_text(json.dumps(sample))
            assert reload(True) == {'done': True, 'failed': True}
            assert state() == adopted, 'Invalid reload replaced the adopted generation'
            checks.append('Invalid behavior batch retains generation')
            sample['components'][0]['interval_ticks'] = 20
            data.write_text(json.dumps(sample))
            components = [
                {'type': 'combatnexus:instant_damage', 'amount': 1},
                {'type': 'combatnexus:periodic_damage', 'amount': 1, 'interval_ticks': 20},
                {'type': 'combatnexus:environment_damage', 'amount': 1, 'minimum_health': 0, 'interval_ticks': 20},
                {'type': 'combatnexus:scheduled_environment_damage', 'amount': 1, 'minimum_health': 0,
                 'interval_ticks': 20, 'first_delay_ticks': 0},
            ]
            for component in components:
                component['damage_type'] = 'combatnexus:missing_verification_type'
                data.write_text(json.dumps(dict(sample, components=[component])))
                assert reload(True) == {'done': True, 'failed': True}, component['type']
                assert state() == adopted, component['type'] + ' invalid batch published'
                checks.append(component['type'] + ' missing damage type retains generation')
            # The old published definitions must still execute after failed candidate preparation.
            retained = evaluate('''(function(){
                function cls(n){return Java.loadClass("io.github.naimjeg.combatnexus."+n);}
                var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
                cls("combat.spi.CombatWorldLifecycleAccess").beginTick(world);
                var cow=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
                cow.setPos(0,100,0);level.addFreshEntity(cow);
                try {
                    var Id=Java.loadClass("net.minecraft.resources.Identifier"),id=Id.parse("combatnexus:reload_retained");
                    var definition=cls("effect.api.EffectDefinition").decode(cls("config.SettingsJson").read(new (Java.loadClass("java.io.StringReader"))(
                        '{"level":0,"lifetime":{"kind":"TIMED","ticks":200},"stacking":"combatnexus:replace","category":"BENEFICIAL","dispellable":true,"attributes":[],"behaviors":["combatnexus:last_stand"],"priority":0,"restrictions":[]}')),
                        cls("combat.internal.definition.CombatDefinitions").forServer(AutoJS.server()).behaviors());
                    world.effects().apply(definition.instantiate(cow.getUUID(),id,id,level.getGameTime()),cls("effect.api.EffectService$Stacking").REPLACE);
                    cow.setHealth(5);cow.invulnerableTime=0;cow.hurtServer(level,cow.damageSources().generic(),1000);
                    return JSON.stringify({protected:cow.getHealth()===1,consumed:!world.behaviors().view(cow.getUUID()).get(0).protectionAvailable()});
                } finally {cls("combat.spi.CombatWorldLifecycleAccess").clear(world,cow.getUUID(),cls("effect.api.EffectService$Removal").WORLD_EXIT);cow.discard();}
            })()''')
            assert retained == {'protected': True, 'consumed': True}
            checks.append('old published behavior still executes after rejected batches')
            damage_type = pack / 'data/combatnexus/damage_type/verification_type.json'
            damage_type.parent.mkdir(parents=True, exist_ok=True)
            damage_type.write_text(json.dumps({'message_id': 'generic', 'scaling': 'never', 'exhaustion': 0}))
            for component in components:
                component['damage_type'] = 'combatnexus:verification_type'
            data.write_text(json.dumps(dict(sample, components=components)))
            assert reload(True) == {'done': True, 'failed': False}
            assert state()['generation'] > adopted['generation']
            checks.append('all four damage components accept a type resource added in the same pack')
            print(json.dumps({'passed': len(checks), 'checks': checks}))
        finally:
            assert reload(False) == {'done': True, 'failed': False}
