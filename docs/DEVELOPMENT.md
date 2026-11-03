# CombatNexus 当前开发入口

遵循根目录 [AGENTS.md](../AGENTS.md)。本文件记录当前开发方式；[legacy](legacy/DEVELOPMENT.md) 保留历史实现和验收正文，旧命令与次数不代表当前通过范围。

## 支持与边界

唯一活动目标为 `targets/neoforge-26.1.2`：Minecraft 26.1.2、NeoForge 26.1.2.126、Java 25。NeoForge 1.21.1 仅为未启用骨架；Fabric、Forge 不维护，未移植 target 不发布。模组 ID 为 `combatnexus`，根包为 `io.github.naimjeg.combatnexus`。仅支持当前新世界，不声明旧存档、旧 Java API 或跨版本网络兼容。

`common` 放没有已证实平台差异的业务与资源；target 放真实 API 差异、Mixin、事件及网络接线。共用源码采用 source-set sharing，target 不依赖 `project(':common')`。common 独立编译只借用活动 NeoForge API 类路径，不纳入 target 源码或资源，不产生发布 Jar。平台依赖逐步用明确合约隔离，不使用反射或运行期版本分支。伤害归 `combat.damage`，Effect、治疗、护盾、恢复与展示各守自身领域。

属性、Effect 与面板共享服务端快照和来源明细；客户端不计算另一套权威结果。复用已有原版属性，避免同一 Effect 原版与托管流程重复收益。定义格式和重载规则见 [领域定义](DOMAIN_DEFINITIONS.md)。物品说明直接读取当前 ItemStack；当前战斗支持矩阵与其他剩余事项见 [审查记录](VANILLA_COMBAT_REPAIR.md#源码审查与剩余缺口)。

`docs/DamageNexus-main` 是只读迁移参考，不参与编译或修复；[迁移清单](legacy/migration/damage-source-manifest.json) 记录历史路径映射，并非当前源码目录清单。命名空间变更须同时检查注册、数据、Mixin、网络、配置、语言、公开 API 与验证脚本。

## 构建和验证

Windows PowerShell，在活动 target 目录执行：

```powershell
.\gradlew.bat clean build
```

再回仓库根目录执行：

```powershell
.\gradlew.bat -p common clean build
python scripts/verify-baseline.py
python scripts/verify-common-boundary.py
python scripts/verify-capabilities-runtime.py --syntax-only
python scripts/verify-capabilities-runtime.py --suite effect-foundation --start --restart --reload --port 25595 --game-port 25570
```

运行器为独立目录写入 EULA 和回环地址服务端配置，通过 AutoJS 执行真实游戏操作，再停止实例。报告在 `targets/neoforge-26.1.2/build/verification/isolated/`，其中 `summary.json` 汇总各脚本，`reload.log` 保存重载逐项证据，`persistence-before.json` 和重启报告记录跨进程恢复。端口应未被其他实例占用。Mixin、事件、网络或资源改变需要相应运行验证，编译不替代运行正确性。客户端展示专项沿用 `scripts/verify-carried-effect-tooltips.py`。

不恢复 JUnit、GameTest 或其专用夹具；新增行为以 AutoJS 覆盖正常、边界和失败语义，记录环境与实际结果，不降低 Mixin require。旧 SDK source set、Jar、开发映射及运行参数已移除。`scripts/autojs/effect-extension.init.gradle` 仅为验证创建临时 Effect 扩展，源码在 `scripts/autojs/effect-extension/java`，边界脚本检查其公开 API 使用；它不进入发布 Jar，也不代表旧 SDK 全套场景。仍依赖 `example.combatnexus.*` 的历史套件尚未恢复，见审查记录 G2。

## 固定依赖与来源

版本以根及 target 的 Gradle 文件为准；不得使用动态版本，也不提交凭据、缓存、个人绝对路径或 build 输出。

| 依赖 | 固定版本 / 文件 | 官方来源 |
|---|---|---|
| NeoForge | 26.1.2.126 | https://maven.neoforged.net/releases/net/neoforged/neoforge/ |
| ModDevGradle | 2.0.141 | https://github.com/neoforged/ModDevGradle |
| mod-publish-plugin | 2.1.1 | https://github.com/modmuss50/mod-publish-plugin |
| AutoJS | 1.0.1 | https://maven.sighs.cc/repository/maven-public/com/sighs/autojs/ |
| Rhino | 2101.2.8-build.91 / 8463898 | https://www.curseforge.com/minecraft/mc-mods/rhino/files/8463898 |
| spark | 1.10.187 / 8918574 | https://www.curseforge.com/minecraft/mc-mods/spark/files/8918574 |
| JEI | 29.53.0.115 / 9384706 | https://www.curseforge.com/minecraft/mc-mods/jei/files/9384706 |
| 通用拼音搜索 | 4.6.9 / 9163773 | https://www.curseforge.com/minecraft/mc-mods/just-enough-characters/files/9163773 |
| Jade | 9156414 | https://www.curseforge.com/minecraft/mc-mods/jade/files/9156414 |
| 可选 Apotheosis | 9.1.0 / 9054740 | https://www.curseforge.com/minecraft/mc-mods/apotheosis/files/9054740 |
| 可选 Placebo | 10.0.2 / 8502256 | https://www.curseforge.com/minecraft/mc-mods/placebo/files/8502256 |
| 可选 Apothic Attributes | 3.1.0 / 9076588 | https://www.curseforge.com/minecraft/mc-mods/apothic-attributes/files/9076588 |

AutoJS、Rhino 与开发观察工具为运行依赖；可选组合通过 `verificationCompatibility` / `verificationAttributesOnly` 开启。本轮结果不外推至未运行组合。

## 2026-11-07 定义顺序与接线修复验收

Windows / Java 25 toolchain / Minecraft 26.1.2 / NeoForge 26.1.2.126 / AutoJS 1.0.1 / Rhino 2101.2.8-build.91。隔离开发服务端，无可选神化组合。执行上方两次 clean build、发布包检查、公共边界检查、脚本语法检查及 `git diff --check`，均通过；target 构建不再出现 compileSdkExampleJava 等空 SDK 任务。公共边界覆盖 1058 个 common 编译类及真实临时扩展源码；发布包检查覆盖 805 个活动源码类、资源、元数据和 Mixin 配置。

`effect-foundation --start --restart --reload --port 25595 --game-port 25570` 最终退出码 0，共 550 项通过。其中定义脚本 40 项（包含新增顺序与真实治疗/伤害组合），整批重载 16 项，真实重启 20 项。四类内置伤害组件的缺失类型均阻止发布并保留代际；旧行为仍可执行，同包新增类型可被四类组件引用。重启验证明确断言 JSON 顺序、保存计划与新加载计划相等、装备对账不补发已消费保护，以及剩余寿命和周期延迟保留。

首轮新增脚本失败后已修正：复制治疗规则的数据包不能复用假设单份规则的完整定义脚本，改为独立执行旧代际保护行为；周期状态改从现有平台保存入口和保存 codec 读取，不从只读行为视图调用 save。以上 550 项为修正后完整重跑结果，首轮不计为通过。最终证据在本地 `targets/neoforge-26.1.2/build/verification/isolated/summary.json`、`reload.log`、`persistence-restart.json`，不入库。之后仅修正重载检查标签文字和补充本文档，未更改已验证的行为。

本轮未执行客户端视觉、可选兼容组合或仍依赖旧 SDK 类的套件；相关未完成事项仍见审查记录 G2、G4、G6。全部临时服务端由运行器停止。
