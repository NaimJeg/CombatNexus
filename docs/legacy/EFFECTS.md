> 历史记录：正文保留对应阶段的实现、命令和结果，不代表当前支持或通过范围。当前入口见 [开发约定](../DEVELOPMENT.md)、[领域定义](../DOMAIN_DEFINITIONS.md) 与 [现行审查](../VANILLA_COMBAT_REPAIR.md)。

# 统一 Effect 与原版投影

仅维护 Minecraft 26.1.2、NeoForge 26.1.2.126、Java 25。仅支持新世界，旧存档不兼容：不导入旧原版活动效果或燃烧字段。新架构自己的托管存档保存有效层、隐藏层、来源、展示定义、剩余时间和周期相位。

## 所有权

`EffectDefinition` 和 `EffectInstance` 保存已采用的生命周期、来源、叠加注册 ID、属性、行为、解除策略及 `EffectDisplay`。时限到期撤销；瞬时在应用事务中执行并返回子操作回执，不进入活动集合；永久没有到期时间，但仍受条件解除及实体生命周期清理控制。

原版 40 个注册项中，仅明确白名单的 26 个战斗效果由 `NativeEffectBridge` 托管。急迫和挖掘疲劳保留：原版每级分别提供 +10% / -10% 攻速，采掘消费者继续原生。夜视、反胃、水下呼吸、鹦鹉螺之息、海豚的恩惠、潮涌能量、幸运/霉运、村庄英雄、三种预兆及饥饿/饱和药水共 14 项保留原版生命周期。再生、中毒、凋零使用通用周期行为，瞬间治疗/伤害使用瞬时事务。当前具体清单及运行结果见 [范围收窄](../VANILLA_COMBAT_REPAIR.md#战斗接管范围收窄与饥饿状态)。

原版添加、强制更新、移除、清空、瞬时应用和燃烧入口接入公共事务。服务端 `MobEffectInstance` 只作查询与同步投影，停止原生计时和周期行为。原生表现回调继续维护粒子、乘客同步、玩家进度等；其属性安装/撤销被抑制，贡献只由托管回执安装一次。`getEffect` 仅为托管效果返回投影副本；非托管实例和集合/map 查询保持原版语义。清空操作先通过托管所有者处理其状态，再让原版处理非托管效果，取消不重复通知。

`combatnexus:stronger_with_hidden` 按实体＋效果聚合，保留弱层的原始到期时间和施加者。不同施加者不叠加多份原版属性。按来源 `REMOVED` 只撤销该来源，必要时恢复另一来源；原版清除使用整组驱散。隐藏层恢复、相同来源刷新、来源变化和展示标志变化均同步更新。

## 公共扩展

通过 `CombatAccess.find(level).effects()` 使用 `apply`、`bind`、`fact`、`remove`、`clear`、`snapshot`；写入返回现有 `EffectResult` 或 `CommandResult` 及事务回执。绑定身份为实体、效果和来源，重复相同事实不重装，参数更新保留周期相位。

启动期 `CombatRegistrar` 提供 `effectStacking`、`effectCondition`、`effectCapability`、`effectDisplay`、`environmentEffect` 和通用 `behavior` 注册。`EffectFact` 持有条件 ID 与不可变数值事实。能力触发包括应用、周期、受伤、死亡和结束。注册完成后冻结；定义通过 ID 引用，不增加专用核心分支。

周期策略为 `RESTART`、`PRESERVE`、`REMAINING_DURATION`。后者用于原版剩余时长取模；无限效果采集实体 tick 初始相位。保存恢复保留下次触发距离，跳过的 tick 不补发，失败或取消不重试伤害/治疗。`DamageRequest.minimumHealth` 在实际生命扣除处限制扣血；它不会给目标安装全局免死状态。

外部扩展示例在 `scripts/autojs/effect-extension/`，由验证 init 脚本单独加载，不进入发布 Jar。它注册自定义周期治疗、条件永久效果、叠加策略、显示身份和通用触发能力，不修改核心或添加专用 Mixin。

## 平台接入与提交事件

当前 Java API 的平台合约位于 `combat.api.platform`。启动注册回调通过 `CombatRegistrar.worldLifecycle()`、`nativeDamage()`、`nativeEffects()`、`effectOrigins()` 和 `equipmentDamage()` 取得接入句柄。`WorldLifecycleAccess` 接收 `ServerLevel` 和 `NativeWorldHooks`，负责加载、时钟通知、保存恢复、装备变化、原生治疗、投射物和玩家生命周期；其公开签名不包含 Runtime、内部上下文或 SPI。业务查询 `CombatAccess.find` / `CombatWorldSessions.current` 不创建世界，也不推进时钟。

`NativeWorldHooks` 提供原生治疗执行、Effect 到期许可和提交后的投影通知。平台只实现原生投影接口；吸收生命存储通过不透明 `NativeAbsorptionStorage.State` 交给内部所有者维护。线程、当前世界会话和事务阶段由内部调度校验。启动句柄不是可自行构造的应用权限；计算回调不能用生命周期句柄推进世界，装备承伤回执接收器在同步回调退出后失效，伤害贡献写入在计算结束后失效。独立示例 `scripts/autojs/effect-extension/java/probe/EffectExtension.java` 只引用公开 API，并验证这些约束。

通过 `registrar.observeMoment(id, CombatMoment.Effect.class, observer)` 订阅变化。托管事件的 `managedChange()` 包含不可变的完整前后实例、来源与移除原因。成功提交统一通知；普通倒计时、失败提交和补偿不通知。原版包装仅观察非托管效果，投影不会重复通知。隐藏层恢复是一次状态变化；换维度先以 `DIMENSION_CHANGE` 撤销旧会话，再由目的地恢复；死亡后的重生清理不会再次发布已经移除的实例。

`EffectDisplay.provenance()` / `EffectView.provenance()` 保存可确认实体、物品、装备槽、附魔注册 ID、维度和方块位置。来源描述与叠加身份独立，不能通过补来源增加原版收益。消耗品、药箭、附魔施加、海龟壳及装备绑定、信标和潮涌核心均在实际入口采集；持续效果云保留原生传入的云实体，不推测主人。无证据的字段为空。

这次变更移除旧 `EffectProjectionAccess`、`NativeHealthTarget` / `NativeAbsorptionTarget` SPI 接线及 target 的 Runtime 查询入口，不增加旧 Java 接口兼容门面。既有非本轮平台状态存储适配仍待独立迁移；本轮边界检查的文件集合由 `verify-vanilla-combat-boundary.py` 明确列出。

## 环境定义

`EnvironmentEffects` 处理稳定绑定与伤害脉冲；平台采集实际环境事实与原生触发尝试。`EnvironmentDamage` 保存基础量、伤害类型、最低生命和周期参数，提示从这些参数生成。

窒息、火中、岩浆、营火、热地板、仙人掌由 `ScheduledEnvironmentDamage` 接管：首次绑定立即尝试，之后每 10 tick 尝试一次；重复事实更新保持周期相位，原生接触伤害尝试被抑制。每次执行重新检查条件，离开即解除；取消或免疫仍消耗本次周期，不补发。浆果移动、挤压随机判断、氧气耗尽等其余状态保留各自原生条件与触发节奏。饥饿状态另由 Effect 调度：食物值为 0 时永久绑定，首次延迟 80 tick、随后每 80 tick 尝试 1 点基础伤害，恢复食物后解除；原版饥饿扣血被抑制。简单/普通生命底线分别为 10/1，困难为 0，和平基础量为 0。调度参数和首次延迟进入行为 codec 与标准化文本。

这些调度请求使用 `DamageRequest.Builder.cooldownPolicy(DamageCooldownPolicy.INDEPENDENT)`，不读取或写入共享 `invulnerableTime` 和 `lastHurt`，不会影响普通攻击的无敌帧和差额判定。默认请求仍使用 `VANILLA`。该策略只分离受伤冷却；实体伤害免疫、伤害类型保护和事件取消仍走正常结算门禁，不修改全局伤害类型标签。效果是否绑定、周期何时触发、伤害是否获准结算是三个独立判断。

| 显示 ID（combatnexus） | 生命周期与解除 |
|---|---|
| burning | 时限；点燃、延长、缩短、灭火更新同一身份 |
| suffocation、cramming | 永久；脱离方块或人数低于阈值解除 |
| drowning、freezing、starvation | 永久；氧气、水下保护、冻结量、食物和难度事实决定激活 |
| fire_contact、lava_contact、campfire_contact、hot_floor_contact | 永久；接触解除或火焰保护不适用时解除 |
| cactus_contact、berry_contact | 永久；离开接触范围解除，保留浆果成熟度与实体排除规则 |
| drying、water_sensitive、world_border | 永久；干涸、水体接触、世界边界及安全区事实决定激活 |

环境状态不可被牛奶驱散。着火与火源接触独立，不合并原有伤害语义。永久环境绑定不写入可恢复活动集合；重登、卸载恢复和换维度后重新采样。时限燃烧正常保存。

## 显示

沿用原版效果栏与 `GatherEffectScreenTooltipsEvent`。既有原版效果复用原 ID；环境效果注册只负责显示身份的 `MobEffect`，图标通过 GUI atlas 引用原版图标资源，不复制原版纹理。

安装、更新、隐藏层恢复和移除同步原版效果数据及服务端采用的 `TooltipBlock`。默认说明条件、基础量、周期及持续方式；Shift 展示叠加、来源与解除规则。最终伤害/治疗仍由服务端结算。未知施加者显示未知，不能根据受害者反推来源。

时限保留原版倒计时，永久使用原版无限格式，瞬时没有活动图标。宽窄布局保留名称、时间、其他模组提示行和既有属性单位格式。断线清空定义缓存；换维度重新采用目的地同步。

## 运行验证

使用 AutoJS 1.0.1、Rhino 2101.2.8-build.91；不使用 JUnit/GameTest。隔离运行不加载神化联合兼容，不能外推为所有第三方模组组合的验证。

```text
python scripts/verify-capabilities-runtime.py --suite effect-foundation --start --restart --port 25595 --game-port 25570
python scripts/verify-effect-display.py --server-port 25597 --client-port 25596 --game-port 25571
```

第一条命令启动并停止一次性服务端，包含 40 项注册遍历、来源与属性、周期、环境、瞬时结算、失败补偿、扩展、自然 tick 和真实重启。第二条连接已启动的隔离服务端与客户端，检查真实网络同步、中英文、宽窄提示、重连、换维度和死亡重生；它验证真实 tooltip 事件，不以鼠标悬停截图作为断言。

服务端报告在 `targets/neoforge-26.1.2/build/verification/isolated/`，客户端在 `build/verification/effect-display.json`；清理构建前可将服务端证据复制至 `build/verification/effect-server/`。构建产物和运行世界均不入库。实际通过范围以报告中逐项断言为准，构建成功不代替行为验收。

### 2026-11-07 实际结果

活动 target 与 common 的 `clean build` 通过；common 边界检查、发布 Jar/资源/Mixin 检查、AutoJS/Python 语法检查通过。隔离服务端共通过 **417** 项，真实客户端共通过 **59** 项，没有新增 JUnit/GameTest。

| 服务端脚本 | 通过数 |
|---|---:|
| effect-lifetimes / instant-effects / effect-damage-floor | 19 / 15 / 7 |
| effect-receipts / effect-phases / combat-scheduling | 13 / 19 / 11 |
| native-effect-ownership | 163 |
| environment-effects / environment-facts | 26 / 13 |
| effect-extension / effect-behaviors | 10 / 38 |
| native-effect-failures / domain-definitions | 7 / 33 |
| 自然世界 tick / effect-natural / effect-restart | 23 / 11 / 9 |

行为断言包括 12 类原版属性贡献、挖掘、呼吸、失明移动限制、发光、村民交易折扣、灾厄转试炼预兆、一次触发的饱和与吸收、中毒/饥饿生命底线、治疗反转，以及渗浆、蓄风、虫蚀、盘丝的数量或触发条件。随机行为使用受控种子验证次数，不以统计抽样推断实现正确。

客户端在中英文下分别验证 26 项，加上 7 项真实同步及实体生命周期检查，共 59 项。包含全部 15 个环境图标的 GUI atlas 解析；没有新增状态面板。显示验证基于真实控件事件和资源，不声称覆盖未运行的第三方提示模组组合。

换维度验证发现并修复了卸载标志被误判为死亡的问题：源世界退出保存使用事件所属维度；目的世界重新安装；`removed` 状态不等于死亡，只有实际死亡才删除托管存档。永久环境条件不随转移恢复。原版表现回调继续维护玩家进度、乘客同步和粒子，属性安装由托管贡献负责。

### 2026-11-07 环境周期接管增量

同一 Windows / Java 25 / NeoForge 环境下，活动 target 和 common 的 `clean build`、common 边界、发布包和脚本语法检查均通过。执行 `python scripts/verify-capabilities-runtime.py --suite effect-foundation --start --port 25595 --game-port 25570`，隔离服务端通过 **428** 项；本次不带 `--restart`，不将先前 9 项重启结果计入。真实客户端显示脚本通过 **63** 项。

新增 `scripts/autojs/verify-environment-cadence.js` 的 20 项自然 tick 断言：六项定义均采用 10 tick 调度；第 0、10、20 tick 尝试伤害，第 10 tick 取消后不重试；重复事实和原生接触调用不增加触发；离开停止、重新进入立即触发；真实伤害免疫阻止扣血，解除免疫不补发。还验证独立伤害不改变普通攻击的共享冷却与差额判定，以及行为参数编解码。客户端新增四项双语断言，检查服务端同步的 0.5 秒周期与独立冷却说明。

本次服务端 JSON 证据在清理构建前保存至 `build/verification/environment-cadence/`；客户端证据在 `build/verification/effect-display.json`。构建报告不替代以上运行断言。
## 物品携带效果提示修复（2026-11-07）

物品提示新增 `ItemPotionTooltipProjector`，读取 `POTION_CONTENTS`、`CONSUMABLE` 中的状态效果，以及高级提示下的 `SUSPICIOUS_STEW_EFFECTS`。保留原版名称、等级、按物品倍率缩放的时长和属性数值；补充效果行为说明及食用触发概率。遵循组件隐藏设置，迷之炖菜普通提示继续隐藏效果。

40 种原版效果的物品行为说明通过现有登录/重载定义同步发送，独立于玩家当前状态栏缓存。抗性说明使用服务端公式提供的每级 20% 参数，并说明 100% 上限及绕过条件；客户端不计算最终伤害。未同步或未知效果显示说明暂不可用。停止为 `STORED_ENCHANTMENTS` 追加附魔说明，附魔书保留原版名称和等级；装备的 `ENCHANTMENTS` 说明保持。

验证命令：`python scripts/verify-carried-effect-tooltips.py`。该脚本启动隔离服务端和客户端，分别使用 AutoJS 25585/25586 与游戏端口 25584，运行 `scripts/autojs/verify-carried-effect-tooltip-client.js`，结束后停止本次启动的实例。

实际环境：Windows、Java 25.0.3、Minecraft 26.1.2、NeoForge 26.1.2.126、AutoJS 1.0.1、Rhino 2101.2.8-build.91。中英文真实 ItemStack tooltip 事件共 275 项通过：46 种注册药水、40 种原版效果描述、三种神龟配方的四种载体、金苹果、腐肉概率、水瓶、组件隐藏、迷之炖菜高级提示、缺失定义、附魔书与装备，以及断线清理。报告位于 `build/verification/carried-effect-tooltips/report.json`，同目录保留启动日志；没有执行鼠标悬停截图验收或可选神化兼容组合。

活动 target 与 common 的 `clean build` 通过，公共边界、发布包及脚本语法检查通过。首轮输出检查发现翻译字符串中常量百分号影响参数替换，修正后增加参数及残留占位符断言，并重新完成构建与运行验证。

### 物品提示 Shift 折叠增量（2026-11-07）

药水与食物的新增行为说明、概率、属性及新增效果标题统一折叠到 Shift 详情；药水原版“当生效后”及其属性数值段也仅在 Shift 时显示。普通提示保留原版效果名称、等级和时长，并且只追加一次 Shift 提示。水瓶不显示空详情提示，迷之炖菜仍遵循高级提示门禁。属性段通过原版生成的完整组件序列匹配移除，保留其他提示行；展开直接使用原版属性段。

同一 AutoJS 脚本改用 NeoForge ClientTooltipFlag 验证普通与 Shift 两种真实物品提示事件，中英文 333 项通过。当前脚本使用 AutoJS 25587/25588、游戏端口 25589，启动前检查端口可用，避免占用已有实例。环境同上。报告仍为 `build/verification/carried-effect-tooltips/report.json`。

活动 target 的 `build`、common 的 `clean build`、公共边界、发布包及脚本语法检查通过。活动 target 的 `clean build` 已尝试，但运行中的客户端占用 `build/moddev/artifacts/minecraft-patched-26.1.2.126.jar` 导致 clean 失败；本次保留该客户端，没有将增量构建宣称为 clean build 通过。

### 统一描述迁移

Effect 行为说明已迁入启动期描述器，共用 DescriptionDocument 与伤害词条/词缀展示。生命周期、等级和来源显式组合，活动同步增加会话及 generation；物品实例由服务端投影，旧客户端重算路径删除。此前“悬停不查询、脱离槽位副本立即展示”的记录仅适用于迁移前版本。当前 API 与验证入口见 [统一战斗描述](ITEM_TOOLTIPS.md)。

以上早期 40 项全托管与饱和瞬时写入记录为历史验收。当前不提供 `InstantFood` / `InstantExecution.feed`，食物值、饱和度和消耗度交还原版；食物回血只包装原生 `heal()` 调用，不另计时。
