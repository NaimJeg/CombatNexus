"""Check the public API boundary of the newly introduced combat adapters and public extension probe."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "targets/neoforge-26.1.2/src/main/java/io/github/naimjeg/combatnexus"
FILES = [TARGET / name for name in (
    "effect/platform/mixin/ConsumableOriginMixin.java", "effect/platform/mixin/BeaconOriginMixin.java",
    "effect/platform/mixin/ConduitOriginMixin.java", "effect/platform/mixin/EnchantmentEffectOriginMixin.java",
    "combat/platform/DamageApplicationAdapter.java", "combat/damage/platform/mixin/PlayerApplicationMixin.java",
    "combat/damage/platform/mixin/LivingEntityApplicationMixin.java",
    "combat/platform/GuardianCombatAdapter.java", "combat/platform/EquipmentCombatAdapter.java",
    "combat/damage/platform/mixin/LivingEntityDamageSettlementMixin.java", "combat/damage/platform/mixin/ArmadilloDamageMixin.java",
    "combat/damage/platform/mixin/GuardianDamageMixin.java", "combat/damage/platform/mixin/WolfArmorMixin.java",
    "presentation/platform/VanillaCombatItemDescriptions.java", "presentation/platform/VanillaCombatEffectDescriptions.java",
)] + [ROOT / "scripts/autojs/effect-extension/java/probe/EffectExtension.java"]
FILES += list((ROOT / "common/src/main/java/io/github/naimjeg/combatnexus/combat/damage/builtin/bridge").glob("*.java"))
FILES += [ROOT / "common/src/main/java/io/github/naimjeg/combatnexus/combat/damage/bridge/vanilla/VanillaPreEventScalingBridge.java"]
FILES += list((TARGET / "effect/platform").rglob("*.java"))
FILES += [TARGET / name for name in (
    "combat/platform/CombatWorldLifecycle.java", "combat/platform/CombatPersistence.java",
    "presentation/platform/CombatPresentationNetwork.java",
    "combat/platform/CombatControlEvents.java", "combat/platform/WorldHealingPort.java",
    "combat/healing/platform/NativeHealingAdapter.java",
    "combat/healing/platform/mixin/LivingEntityHealingMixin.java",
    "combat/health/platform/mixin/LivingEntityHealthAccessMixin.java",
    "combat/health/platform/mixin/LivingEntityAbsorptionMixin.java",
    "combat/health/platform/mixin/LivingEntityMomentsMixin.java",
    "combat/health/platform/mixin/DragonDamageMixin.java",
)]
FILES = sorted(set(FILES))
for path in FILES:
    text = path.read_text(encoding="utf-8")
    assert not re.search(r"io\.github\.naimjeg\.combatnexus[\w.]*\.(?:core|internal|spi)\.", text), path
print(f"PASS: {len(FILES)} scoped adapters/probes use public APIs; unrelated legacy adapters are not covered")
