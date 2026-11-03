> 历史记录：正文保留对应阶段的实现、命令和结果，不代表当前支持或通过范围。当前入口见 [开发约定](../DEVELOPMENT.md)、[领域定义](../DOMAIN_DEFINITIONS.md) 与 [现行审查](../VANILLA_COMBAT_REPAIR.md)。

# 领域定义与数据策略

2026-11-05：P1/P2 源码重构记录。活动目标仅 NeoForge 26.1.2，Java 25。2026-11-06 后续运行与格式增量见 [能力验收](CAPABILITY_COMPLETION.md)。

## 分类与执行归属

| 领域 | 定义与消费者 | 原版关系 |
| --- | --- | --- |
| attribute/formula | 显式属性目录、单位、范围、预留标识；公式保持纯计算 | 攻速、移速等复用原版属性；预留属性不宣称生效 |
| damage | channels、rules、entry/affix templates；现有伤害阶段 | 减伤由共享 executor 政策决定，避免原版与框架重复计算 |
| healing | healing_rules；治疗管线 CONTRIBUTIONS / LIMITS | 原生 heal 仍经过治疗入口；received 倍率只消费一次 |
| regeneration | 有期限的恢复定义和安装计划；只调度 healing | 不接管食物或原版再生的计时器 |
| shield | 静态定义、授予请求、只读快照分离 | 按原生伤害类型/标签过滤总伤害，不等于通道护盾 |
| effect | managed 定义、属性贡献、行为引用、类别、驱散政策 | observed 原版效果不安装第二份收益 |
| combat 行为 | 五种类型化组件；各反应独立限额 | 仅显式安装激活，不因资源存在而授予 |
| equipment | 有槽位白名单的多绑定数据组件 | 生命周期适配器安装、撤销，服务器持有来源账本 |
| presentation | 服务端快照投影单位、预留状态、Effect 类别/归属/驱散政策 | 客户端不计算权威公式 |
| config/target | 严格解码、整体代际采用；平台注册、事件、网络适配 | 不伪装多版本兼容 |

## 整体重载

所有目录采用 `data/<namespace>/<directory>/<path>.json`；文件路径决定定义 ID。新增目录与 settings、damage、behaviors 同批准备、校验和发布。任意文件失败保留整个旧代际，新请求捕获新代际，在途请求继续使用捕获句柄。

每文件最多 16,384 字符、4,096 JSON 值节点、32 层嵌套；拒绝重复键、尾随内容和非法 JSON。新增领域定义及行为必须给齐全部字段，不接受未知字段；标识最长 256 字符。damage 顶层、模板/规则结构和注册条件/操作声明的键也校验；扩展 codec 的内部负载由扩展负责。此校验不等于所有物品存储 codec 都有同样严格的未知字段策略。

| 目录 | 总量 | 内容 |
| --- | --- | --- |
| `combatnexus_behaviors` | 256 | `priority`、`duration_ticks`、`components` |
| `combatnexus_healing_rules` | 64 | 阶段、优先级、原因/来源/生命比例条件、增量和倍率 |
| `combatnexus_regeneration` | 256 | 速率、受伤延迟、游戏规则/脱战条件、期限 |
| `combatnexus_shields` | 256 | 容量、优先级、类型/标签、恢复参数、叠加政策 |
| `combatnexus_effects` | 256 | 等级、期限、叠加、类别/驱散、属性和行为引用 |

同路径按资源包优先级覆盖，不合并字段；标签使用原版合并。护盾类型/标签资源和 Effect 行为引用在准备阶段检查。护盾 channels 在候选通道目录中校验，空集不限通道；类型匹配与通道匹配同时满足才吸收。安装护盾时再解析当前注册表标签，冻结匹配类型集合；空标签匹配零种伤害，只有两个选择器均为空才匹配所有伤害。持续资源保留安装时的值；直接安装的 Effect、护盾与恢复计划不因重载自动重置。

## 行为组件

```json
{
  "priority": 0,
  "duration_ticks": 200,
  "components": [
    {"type": "reflection", "ratio": 0.2, "trigger_scope": "OPERATION", "maximum_triggers": 1},
    {"type": "healing_additive", "amount": 1}
  ]
}
```

最多五种组件，同类型不得重复。`priority` 范围 -10,000..10,000，期限/间隔 1..1,200,000 tick。

| type | 除 type 外的全部字段 |
| --- | --- |
| `lethal_protection` | 无；每次安装的可消费保护 |
| `reflection` | `ratio` 0..1、`trigger_scope`、`maximum_triggers` |
| `overflow_shield` | `ratio` 0..1、`capacity` 0..1,000,000、`duration_ticks`、`trigger_scope`、`maximum_triggers` |
| `periodic_healing` | `amount` 0..1,000,000、`interval_ticks` |
| `healing_additive` | `amount` -1,000,000..1,000,000 |

触发范围为 OPERATION / TARGET / ACTION / ROOT / SOURCE，上限 1..64；ACTION 必须有真实动作证据。反伤与转盾分别持有限额，不共享一个万能限额字段。类型集合由受审查的 Java 执行器固定，不支持任意脚本或任意事务回调。

## 治疗与恢复

治疗规则全部字段：`priority`、`phase`、`reasons`、`sources`、`minimum_health_ratio`、`maximum_health_ratio`、`additive`、`multiplier`。阶段仅 CONTRIBUTIONS 或 LIMITS；原因使用 `HealingRequest.Reason` 枚举，来源最多 32 项，空列表表示不限。生命比例为冻结输入的 health/maxHealth，闭区间 0..1。增量 ±1,000,000，倍率 0..10。

各阶段内按优先级降序、ID 排序；先汇总匹配增量，再截断到非负，最后应用倍率。CONTRIBUTIONS 同桶包含行为增量，随后消费 received，LIMITS 最后计算。来源明细跟随实际阶段保存；规则不能写资源或派生子请求。内置 `example_healing` 仅匹配同名显式来源，不改变普通治疗。

恢复全部字段：`health_per_second` 0..1,000,000、`damage_delay_ticks` 0..1,200,000、`respects_natural_regeneration_rule`、`requires_out_of_combat`、`duration_ticks`。通过 `RegenerationDefinitions.install` 安装，有期限计划到期丢弃未满 20 合格 tick 的积累；取消、跳 tick、满血不补发。

## 护盾与 Effect

护盾全部字段：`capacity`、`priority`、`damage_types`、`damage_type_tags`、`channels`、`recovery_delay_ticks`、`recovery_per_second`、`stacking`。选择器各最多 32 项；容量/恢复速率 0..1,000,000，延迟 0..1,200,000。叠加为 REPLACE_SAME_SOURCE 或 REFRESH_SAME_SOURCE；后者保留已有余额（截断到新容量），不借刷新补满。`ShieldDefinitions.grant` 显式给初始余额和期限；余额不得超过容量，目标必须存活。快照不能再当作写请求。

Effect 全部字段：`level` 0..255、`duration_ticks`、`stacking`、`category`、`dispellable`、`attributes`、`behaviors`、`priority`、`restrictions`。叠加为 REPLACE / REFRESH_KEEP_STRONGER；类别 BENEFICIAL / HARMFUL / NEUTRAL 仅是政策数据，不自动赋予免疫。不可驱散的 Effect 拒绝 DISPELLED，来源撤销和生命周期清理仍有效。

属性最多 32 项，每项必须包含 `attribute`、`modifier`、`amount`、`operation`；使用已注册属性与原版 modifier 操作，拒绝重复 attribute/modifier 对。行为引用最多 32 个 ID，展开后仍满足最多五种唯一组件。组合使用 Effect 自己的 priority/duration，引用只提供组件参数。`restrictions` 接受 WALK / JUMP / TURN / ATTACK / USE_ITEM / CAST / SKILL_MOVEMENT / EXTERNAL_MOVEMENT，不允许重复或未知值；多个来源取并集。`EffectDefinitions.install` 是显式安装入口。

可复制的完整资源见 [agility](../common/src/main/resources/data/combatnexus/combatnexus_effects/agility.json)、[physical_guard](../common/src/main/resources/data/combatnexus/combatnexus_shields/physical_guard.json)、[field_recovery](../common/src/main/resources/data/combatnexus/combatnexus_regeneration/field_recovery.json)、[example_healing](../common/src/main/resources/data/combatnexus/combatnexus_healing_rules/example_healing.json)。

## 装备组件与生命周期

`combatnexus:equipment_bindings` 是持久化并同步的原生数据组件，值为最多八项列表：

```json
[
  {"id":"combatnexus:guard","kind":"BEHAVIOR","definition":"combatnexus:last_stand","slots":["mainhand"]},
  {"id":"combatnexus:speed","kind":"EFFECT","definition":"combatnexus:agility","slots":["mainhand"]}
]
```

绑定记录拒绝未知或缺失字段，非法内容返回 codec 解码错误。绑定 ID 唯一、最长 96 字符；kind 为 BEHAVIOR / EFFECT；slots 非空、不重复，使用原版槽位名。稳定来源由槽位和绑定 ID 派生。来源账本比较物品类型、绑定和解析定义，不因数量/耐久变化补发已消费或已驱散收益。更换定义、物品类型或卸下会撤销旧来源；重新装备获得新安装身份。

实体加入、世界加载和换维度后重新发现；重载重新解析受观察的槽位，未改变的定义不刷新。失效引用使该槽位旧来源撤销，并保留观察记录供后续重载解析；不会静默保留旧收益。观察槽位和安装槽位各上限 4,096。安装失败的来源留在账本以便后续撤销；不自动重试或声称多绑定安装是 ACID。底层 Effect 继续使用 prepare/apply/receipt/reconcile 处理部分写入。

## API 与格式迁移

- `CombatSource.parentDamageId` 改为 `parentOperation: Optional<CombatOperationRef>`，同时保留操作 ID 与类型。仅解释因果，不能替代 `CombatParent` 授权。
- `BehaviorPlan` 改为 priority + typed components；旧扁平行为 JSON 删除，无别名。
- `ShieldDefinition` 保存政策，`ShieldGrant` 保存来源/初始余额/到期，`ShieldPoolSnapshot` 只读。
- `RegenerationPlan` 保存 definition 和 expiresAtTick；`DefinitionGeneration` 增加 domains。
- `EffectInstance` 增加 policy，面板快照保留实例的政策与 ownership。
- 旧 `custom_data.combatnexus_behavior` 明确拒绝，需迁移到 equipment_bindings；原 EquipmentBehaviors 入口移除，平台通过 CombatEquipmentAccess SPI 接入。

不提供旧字段别名、旧 Java 门面或旧存档兼容承诺。客户端和服务端使用同一构建。完整动作控制、跨重启业务状态持久化、未移植 target 和第三方整合包验收仍在支持范围之外。

## 验证与剩余验收

新增 [verify-domain-definitions.js](../scripts/autojs/verify-domain-definitions.js)，迁移已有行为、护盾、恢复与代际脚本；恢复脚本增加到期边界。`verify-behavior-runtime.py --definitions` 增加四目录成功采用、各域非法键、缺失引用和失败保留旧代际检查。

需在隔离环境执行：

```text
python scripts/verify-behavior-runtime.py --port 25590 --definitions
python scripts/run-autojs.py scripts/autojs/verify-recovery-and-lifecycle.js --port 25590
python scripts/run-autojs.py scripts/autojs/verify-support-policy.js --port 25590
python scripts/run-autojs.py scripts/autojs/verify-support-policy-client.js --port 25591 --side client
```

本轮仅静态检查与构建；新增数据组件的实际存取/传输、装备重载与换维度事件、Mixin、C 键展示、原生治疗及第三方兼容仍需运行验证。历史结果不得用于证明本次变更已验收。

最终实际检查：目标目录 `gradlew clean build` 通过（7 项任务）；根 wrapper `gradlew -p common clean build` 通过（5 项任务，common jar 跳过）；common 边界 849 个 class、成品 667 个活动源码类及资源检查通过。35 个 AutoJS 脚本语法、Python AST、common JSON、本文链接与 `git diff --check` 通过。根目录本身没有 clean/build 任务，一次误在根目录执行的 clean build 失败，随后按目标目录重跑通过；不将该失败计为源码问题。JDK 25.0.3 / NeoForge 26.1.2.126，AutoJS 实际运行仍为 0，未测性能。
