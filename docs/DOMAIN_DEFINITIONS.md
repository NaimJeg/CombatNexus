# 当前领域定义

适用于 NeoForge 26.1.2；开发与验证入口见 [DEVELOPMENT](DEVELOPMENT.md)。以下依据当前解析器及 `common/src/main/resources/data/combatnexus` 内置资源，历史格式见 [legacy](legacy/DOMAIN_DEFINITIONS.md)。

## 资源位置

定义 ID 为 `<namespace>:<path>`，文件位于 `data/<namespace>/<目录>/<path>.json`。目录名固定，命名空间可由数据包自定。加载定义本身不会授予 Effect、护盾或行为，需显式安装或装备绑定。

| 目录 | 用途 | 每次重载上限 |
|---|---|---|
| `combatnexus_behaviors` | 带 priority、duration_ticks、components 的行为 | 256 |
| `combatnexus_effects` | 生命周期、属性、行为引用与限制 | 256 |
| `combatnexus_healing_rules` | 治疗条件及计算贡献 | 64 |
| `combatnexus_regeneration` | 恢复计划 | 256 |
| `combatnexus_shields` | 护盾容量、过滤与恢复 | 256 |

伤害通道、规则、词条及词缀使用 `combatnexus_channels`、`combatnexus_rules`、`combatnexus_entry_templates`、`combatnexus_affix_templates`，与上述领域一同准备候选。解析器拒绝未知字段、重复 JSON key、非有限数值及多余尾部 JSON。Identifier 字符串最长 256；ID 列表最多 32 项并拒绝重复。

## 行为与顺序

组件的 `type` 为注册 Identifier，其余参数与 type 同层；第三方组件采用自己的注册 codec。`reflection` 和 `overflow_shield` 的触发限制是嵌套 `limit` 对象，不能将其字段平铺到组件。示例：

```json
{
  "priority": 0,
  "duration_ticks": 200,
  "components": [{
    "type": "combatnexus:overflow_shield",
    "ratio": 1,
    "capacity": 20,
    "duration_ticks": 100,
    "limit": {"trigger_scope": "OPERATION", "maximum_triggers": 1}
  }]
}
```

一个行为最多 64 个组件，组件 type 不可重复；priority 范围 -10000..10000，行为 duration_ticks 范围 1..1200000。周期组件的 `interval_ticks` 和可选 `first_delay_ticks` 控制周期与首次延迟；`phase` 按注册 codec 解析。四类内置伤害组件 `instant_damage`、`periodic_damage`、`environment_damage`、`scheduled_environment_damage` 的 `damage_type` 在候选准备时检查 `data/<namespace>/damage_type/<path>.json` 是否存在，允许同包新增资源。错误包含行为文件、从 0 开始的组件位置和引用 ID。执行时仍通过注册表解析伤害类型；资源存在性检查不代替注册表有效性。

Effect 的 `behaviors` 严格按数组顺序展开，每个行为内的 components 也保持顺序；各行为自己的 priority 不重排展开结果，最终计划采用 Effect 的 priority。空引用数组合法；重复引用、跨行为重复组件 type 和展开后超过 64 个组件均拒绝。标签、来源选择器和护盾过滤器仍是集合，没有执行顺序语义。

## Effect

```json
{
  "level": 0,
  "lifetime": {"kind": "TIMED", "ticks": 200},
  "stacking": "combatnexus:replace",
  "category": "BENEFICIAL",
  "dispellable": true,
  "attributes": [{
    "attribute": "minecraft:movement_speed",
    "modifier": "combatnexus:agility",
    "amount": 0.1,
    "operation": "ADD_MULTIPLIED_TOTAL"
  }],
  "behaviors": [],
  "priority": 0,
  "restrictions": [],
  "tags": []
}
```

`level` 为 0..255。lifetime 为嵌套对象：`TIMED` 要求正整数 ticks，`PERMANENT` 和 `INSTANT` 仅有 kind 字段，不能额外传 ticks。永久不等于不可驱散或自动持久化。瞬时 Effect 只接受瞬时组件，不能有属性或限制；其他 lifetime 不能包含瞬时组件。瞬时治疗与伤害的组合按展开顺序执行，因此治疗封顶后再伤害和伤害后再治疗可产生不同生命结果。

属性最多 32 项，attribute 必须已注册，同一 attribute/modifier 组合不可重复。stacking 必须是已注册策略 ID；category 和 restrictions 使用当前枚举值。tags 缺省为空集合，display 缺省为空；显式 display 使用嵌套字段 `native_id`、`ambient`、`particles`、`icon`、`aggregate_sources`、`persistent`、`environment`。物品、活动 Effect 和面板复用共同描述与服务端状态，不另算权威公式。

## 其他领域

治疗规则包含 `priority`、`phase`、`reasons`、`sources`、`minimum_health_ratio`、`maximum_health_ratio`、`additive`、`multiplier`。phase 仅允许 CONTRIBUTIONS / LIMITS，不可代替 RECEIVED 阶段；空选择器表示不限制，生命比例闭区间位于 0..1。

恢复定义包含 `health_per_second`、`damage_delay_ticks`、`respects_natural_regeneration_rule`、`requires_out_of_combat`、`duration_ticks`。护盾定义包含 `capacity`、`priority`、`damage_types`、`damage_type_tags`、`channels`、`recovery_delay_ticks`、`recovery_per_second`、`stacking`；伤害类型/标签与通道过滤同时满足才可承伤。类型及标签检查当前 ResourceManager 中的资源，通道检查本次候选注册表。

## 重载和恢复

行为先解码和验证，再解析 Effect 引用；所有战斗定义组成一个候选代际。任何准备、引用校验或候选编译失败都阻止整批发布，旧代际及既有行为继续有效，不能部分替换。当前 ResourceManager 包含本次生效数据包资源，不仅是重载前注册表。

托管保存协议保留计划、剩余时长、已消费保护次数及周期剩余延迟。真实重启时已保存多行为计划应与本次按数组解析的装备定义一致；装备对账不会因此重新安装或补发已消费状态。此规则不提供旧存档迁移保证。

回归入口：`verify-domain-definitions.js` 检查空/重复引用、展开顺序和真实治疗伤害结果；`prepare-effect-restart.js` / `verify-effect-restart.js` 验证跨进程装备计划、保护消耗与周期状态；`verify-behavior-runtime.py --definitions --definitions-only` 检查整批失败及同包类型引用。由开发入口的 foundation 命令统一运行，重载汇总根据实际 checks 统计。
