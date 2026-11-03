# CombatNexus

CombatNexus 是面向 Minecraft 的 RPG 战斗与属性模组。

- **战斗计算**：提供伤害管线、自定义伤害通道与规则，支持暴击、吸血、治疗、护盾和恢复。
- **属性与效果**：统一管理战斗属性和状态效果，支持通过数据包配置战斗规则与公式参数。
- **Effect 生命周期**：26 种战斗相关原版效果及 15 类环境状态接入托管生命周期，支持瞬时事务、条件绑定、来源聚合和隐藏层恢复，复用原版效果栏。
- **属性面板**：按 **C** 查看服务端计算的属性、来源明细、护盾与状态效果。
- **伤害展示**：提供客户端伤害展示与调试假人，便于观察战斗数值。
- **物品提示**：词条、词缀与装备 Effect 使用摘要和 Shift 详情；附魔数值读取服务端实际定义。

当前唯一活动 target 为 NeoForge 26.1.2（Java 25）；其他 target 不发布。原版战斗效果、狼铠/守卫者结算、来源与公共 API 的支持矩阵及实际回归结果见 [原版战斗修复](docs/VANILLA_COMBAT_REPAIR.md)，平台接入合约见 [Effect 公共 API](docs/legacy/EFFECTS.md#平台接入与提交事件)。物品说明直接读取当前 ItemStack，不依赖槽位查询或物品回包。仅支持当前新世界，不承诺旧 Java API、旧存档或跨版本网络兼容。

当前构建与验证命令见 [开发入口](docs/DEVELOPMENT.md)，数据包格式、行为顺序及重载失败语义见 [领域定义](docs/DOMAIN_DEFINITIONS.md)。

当前实现、验证接线和验收的剩余工作见 [源码审查与剩余缺口](docs/VANILLA_COMBAT_REPAIR.md#源码审查与剩余缺口)。

饥饿状态在食物值为 0 时持续绑定，由 Effect 每 80 tick 结算饥饿伤害；原版饥饿药水和饱和药水保持原生。食物与饱和度机制不变，饱和回血仅在原版 `heal()` 调用处进入治疗结算。当前范围及验证见 [战斗接管范围收窄](docs/VANILLA_COMBAT_REPAIR.md#战斗接管范围收窄与饥饿状态)。

现有 debug/diagnostic 日志、限频器、伤害 Trace 与冷历史、tooltip debug 段和专用开发命令已移除。旧 `operations.json`、`client.json` 不再加载；规则 `trace_label`、相关 Java 诊断 API 和文档 debug 字段已删除，规则过滤策略 `WARN` 改为 `IGNORE`。展示网络协议为 `combatnexus-description-4`，客户端与服务端须同步更新。业务失败结果、回滚、正式结算关联、来源明细和启动期 Mixin 检查保留。

本次移除验证（2026-11-07）：Windows / Java 25.0.3 / NeoForge 26.1.2.126 / AutoJS 1.0.1 / Rhino 2101.2.8-build.91。`verify-capabilities-runtime.py --suite effect-foundation --start --restart` 通过 520 项；`platform-lifecycle` 加 `verify-callback-failures.js`、`verify-native-damage-semantics.js` 通过 216 项；`verify-carried-effect-tooltips.py` 及其双语物品、Effect、面板和控制子套件通过 575 项。实际记录保存在本地 `build/verification/debug-removal/summary.json`。诊断专用脚本已删除，原回调失败隔离检查保留为 `verify-callback-failures.js`。
