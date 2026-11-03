"""Reject common -> target references in sources and compiled bytecode.

Run after `gradlew -p common clean build`. No JUnit/GameTest fixtures involved.
"""
from pathlib import Path
import re
import subprocess


def verify_behavior_ownership(root):
    """Architecture rules for the new behavior boundary, not a runtime correctness claim."""
    java = root / 'common/src/main/java/io/github/naimjeg/combatnexus'
    # Method bodies may call internal implementations; the public ABI may not expose their types.
    classes = root / 'common/build/classes/java/main'
    public_classes = ['.'.join(path.relative_to(classes).with_suffix('').parts)
                      for path in classes.rglob('*.class') if 'api' in path.relative_to(classes).parts]
    assert public_classes, 'Compile common before checking public ABI'
    for offset in range(0, len(public_classes), 60):
        result = subprocess.run(['javap', '-public', '-classpath', str(classes), *public_classes[offset:offset+60]],
                                check=True, capture_output=True, text=True)
        for line in result.stdout.splitlines():
            assert not re.search(r'io\.github\.naimjeg\.combatnexus\.[\w.]*\.(?:core|internal|registry|spi)\.', line), \
                f'Public signature exposes implementation type: {line}'
    extension_sources = list((root / 'scripts/autojs/effect-extension/java').rglob('*.java'))
    assert extension_sources, 'Effect extension sources are missing'
    for source in extension_sources:
        code = source.read_text(encoding='utf-8')
        assert not re.search(r'io\.github\.naimjeg\.combatnexus\.[\w.]*\.(?:core|internal|registry|spi)\.', code), \
            f'Effect extension depends on implementation: {source}'
    # Only explicitly read-only operations belong on the business views. New methods require review.
    for relative, allowed in (
            ('combat/api/behavior/BehaviorSources.java', {'capture', 'view', 'valid'}),
            ('combat/api/reaction/CombatReactionService.java', {'facts'})):
        source = java / relative
        code = re.sub(r'/\*.*?\*/|//[^\n]*', '', source.read_text(encoding='utf-8'), flags=re.S)
        methods = set(re.findall(r'\b(\w+)\s*\([^)]*\)\s*(?:;|\{)', code))
        assert methods == allowed, f'Business view changed its read-only contract: {source}: {methods}'
    # Damage application and native action wiring must use SPI, not source/executor casts.
    consumers = list((java / 'combat/damage/core/settlement').glob('*.java'))
    consumers += list((java / 'combat/damage/core/pipeline').glob('*.java'))
    consumers += list((root / 'targets/neoforge-26.1.2/src/main/java').rglob('*.java'))
    for source in consumers:
        code = re.sub(r'/\*.*?\*/|//[^\n]*', '', source.read_text(encoding='utf-8'), flags=re.S)
        assert 'combat.internal.coordination' not in code, f'Consumer bypasses behavior SPI: {source}'
    print('Behavior ownership boundary OK: read-only APIs; domain/native consumers use SPI')


def verify(root):
    verify_behavior_ownership(root)
    verify_lifecycle_ownership(root)
    common = root / 'common/src/main/java'
    target_classes = set()
    target_packages = set()
    for target in (root / 'targets').glob('*/src/main/java'):
        for source in target.rglob('*.java'):
            package = re.search(r'package\s+([\w.]+)\s*;', source.read_text(encoding='utf-8')).group(1)
            assert package == '.'.join(source.relative_to(target).parent.parts), f'Package/path mismatch: {source}'
            assert 'platform' in package.split('.'), f'Target implementation outside platform package: {source}'
            target_packages.add(package)
            target_classes.add('.'.join(source.relative_to(target).with_suffix('').parts))
    for source in common.rglob('*.java'):
        text = source.read_text(encoding='utf-8')
        code = re.sub(r'/\*.*?\*/|//[^\n]*', '', text, flags=re.S)
        package = re.search(r'package\s+([\w.]+)\s*;', code).group(1)
        assert package not in target_packages, f'Common/target split package: {source}'
        assert 'net.neoforged.' not in code, f'Loader dependency: {source}'
        for name in target_classes:
            assert not re.search(re.escape(name) + r'\b', code), f'Target reference {name}: {source}'
            owner, simple = name.rsplit('.', 1)
            if owner == package:
                assert not re.search(r'\b' + simple + r'\b', code), f'Target sibling {name}: {source}'
    classes = root / 'common/build/classes/java/main'
    assert classes.is_dir(), 'Compile common independently before checking bytecode'
    forbidden = [name.replace('.', '/').encode() for name in target_classes]
    compiled = list(classes.rglob('*.class'))
    assert compiled, 'No compiled common classes'
    for path in compiled:
        data = path.read_bytes()
        for name in forbidden:
            # A descriptor can reference the class itself or any nested type.
            assert not re.search(re.escape(name) + rb'(?=[;$<\x00-\x20]|$)', data), \
                f'Target bytecode dependency {name.decode()}: {path}'
        relative = path.relative_to(classes).as_posix().removesuffix('.class').split('$')[0]
        assert relative.replace('/', '.') not in target_classes, f'Target source compiled into common: {path}'
    print(f'Common boundary OK: {len(compiled)} classes; no target/loader source references')


def verify_lifecycle_ownership(root):
    java = root / 'common/src/main/java/io/github/naimjeg/combatnexus'
    for relative in ('effect/api/EffectService.java', 'combat/shield/spi/ShieldPoolService.java',
                     'combat/healing/api/HealingService.java', 'combat/regeneration/api/RegenerationScheduler.java',
                     'presentation/api/CombatPanelService.java'):
        code = (java / relative).read_text(encoding='utf-8')
        assert not re.search(r'\b(?:create|tick|clear|reset|close|drainChanges)\s*\(', code), relative
    assert 'recordDamage(' not in (java / 'combat/regeneration/api/RegenerationScheduler.java').read_text()
    kernel = (java / 'combat/internal/transaction/CombatServerRuntime.java').read_text()
    assert not re.search(r'void\s+configure\s*\(', kernel), 'Budget writes must follow definition adoption'
    assert not (java / 'combat/damage/config/DamageServerSettings.java').exists()
    assert not (java / 'combat/damage/config/DamageSafetySettings.java').exists()
    runtime = (java / 'combat/internal/CombatRuntime.java').read_text(encoding='utf-8')
    assert 'return panelView;' in runtime
    assert 'CombatDispatchScope.delivering(callback, level.getServer())' in runtime
    assert 'regeneration.recordDamage(snapshot.target().getUUID(), level.getGameTime())' in runtime
    require_tick = runtime.split('private void requireTick()')[1].split('private void requireOwner()')[0]
    assert 'beginTick()' not in require_tick and 'throw new IllegalStateException' in require_tick
    assert 'if (now == tick) return;' in runtime
    target = root / 'targets/neoforge-26.1.2/src/main/java/io/github/naimjeg/combatnexus'
    lookup = (java / 'combat/internal/platform/PlatformWorldLifecycle.java').read_text(encoding='utf-8').split('public static CombatRuntime existing(')[1].split('public boolean loaded')[0]
    assert 'beginTick' not in lookup
    assert not (java / 'combat/damage/core/definition/CombatDefinitions.java').exists()
    assert not (java / 'combat/damage/core/definition/CombatDefinitionReloadListener.java').exists()
    definitions = (java / 'combat/internal/definition/DefinitionGeneration.java').read_text(encoding='utf-8')
    assert 'record DefinitionGeneration' in definitions and 'DamageDefinitions damage' in definitions
    for relative, field in (('effect/internal/DefaultEffectService.java', 'active'),
                            ('combat/shield/internal/DefaultShieldService.java', 'pools')):
        code = (java / relative).read_text(encoding='utf-8')
        assert re.search(r'Map<UUID, Map<Key, \w+>> ' + field, code), relative
        projection = code.rsplit('snapshot(UUID target, java.util.function.IntConsumer budget)', 1)[1].split('return List.copyOf(result);')[0]
        assert 'group(target)' in projection and 'budget.accept(1); result.add(' in projection
    network = (target / 'presentation/platform/CombatPresentationNetwork.java').read_text(encoding='utf-8')
    assert network.index('previous[kind] = now') < network.index('PANELS.put(player,')
    assert 'request.slot() != -1' in network
    assert 'ItemDescriptionProjection' not in network and 'CreativeDescriptionRequest' not in network
    response = network.split('private static void respond(')[1]
    assert response.index('player.containerMenu.containerId != request.containerId()') < response.index('CombatWorldSessions.current(level)')
    assert 'generation != request.generation()' in network
    client = (target / 'presentation/platform/client/CombatPresentationClient.java').read_text(encoding='utf-8')
    assert 'payload.generation() != generation' in client and 'generation = payload.generation()' not in client
    item_query = client.split('public static Optional<JsonObject> item(')[1].split('private static void requestPanel')[0]
    assert 'ItemDescriptionProjection.capture(mc.player, stack)' in item_query
    assert 'sendToServer' not in item_query and 'containerMenu' not in item_query
    assert 'pending.nonce().equals(payload.nonce())' in client
    assert 'session.equals(payload.session())' in client
    item = (java / 'combat/damage/client/tooltip/ItemDamageRuleTooltip.java').read_text(encoding='utf-8')
    assert 'new RuleNarrativePlanner' not in item and 'SelectionResolver.resolve' not in item
    assert 'ItemEffectTooltipProjector.project(' not in item and 'ItemPotionTooltipProjector.project(' not in item
    projection = (java / 'combat/damage/api/display/ItemPresentationProjector.java').read_text(encoding='utf-8')
    assert 'DescriptionDocument.CODEC' in projection
    assert 'DamageEntryDefinition.CODEC' not in projection and 'DamageAffixDefinition.CODEC' not in projection
    assert not (target / 'combat/damage/platform/api/client/phrase/RegisterRulePhrasesEvent.java').exists()
    print('Lifecycle ownership OK: pure lookup/capture, internal controls, target projections, combat generation ownership')


if __name__ == '__main__':
    verify(Path(__file__).resolve().parents[1])
