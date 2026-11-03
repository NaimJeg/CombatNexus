# 仅通过公开 API 接入原版与神化：底层缺口评估

2026-11-06。结论：现有公开 API 可以实现普通数值贡献、治疗处理器、受管效果、周期伤害和部分结算反应，但还不能保持原语义地完整接入原版与神化。主要缺口是执行所有权、攻击时序、外部状态事实和领域命令，不是再增加几个固定行为类型。

本轮只实施移除存储版本机制；下列底层扩展是评估及建议，未实现。只维护当前数据结构和当前活动 target，不设计跨版本读取、迁移、降级或兼容分支。

## 范围与证据

“只用公开 API”指适配模块不导入 CombatNexus 的 internal/core/SPI，不自行开启应用权限；允许读取 Minecraft、NeoForge 和神化提供的类型。真正的原生拦截仍由活动 target 接线，转换成公开的事实或受限回调。若连 NeoForge 事件及平台拦截都不允许，则现有 SDK 无法发现图腾、发射、命中、掉落等执行时机，单纯增加业务方法也无法完成接入。

神化基线是项目实际锁定的 Apotheosis 9.1.0、Apothic Attributes 3.1.0、Placebo 10.0.2；不把未安装的 Apothic Enchanting、Apothic Spawners 算作已评估覆盖。主评估覆盖战斗、装备、效果和资源。神化还有采掘、掉落、重铸、宝石切割、世界等级、入侵和世界生成；若“全部”包含这些，需另设对应模块，不能全塞进 damage。

来源核验：

| 来源 | 固定证据 |
| --- | --- |
| Apotheosis 官方源码 | `26.1` 分支提交 `f2e76862ed9f4dcdf19c25400dc3d5478a46dd74`，声明 9.1.0 |
| Apothic Attributes 官方源码 | `26.1` 分支提交 `44d0a8316fcb7965c26e8ff7eda341830ca63023`，声明 3.1.0 |
| 项目 Apotheosis JAR | CurseForge 文件 9054740；SHA-256 `e350c6461741469b8a04116f731fdad7a8117e1f2908f484ecefc283dba73926` |
| 项目 Apothic Attributes JAR | CurseForge 文件 9076588；SHA-256 `373fbbb5fa11d2ec33d4ae774fcce33d3dbdc8f46d868f12dff3faa7fc53e52e` |
| 原版源码 | 本地 `mergeWithSources_1160efb7811dab0c8d656ee6ca0ef5b6c4b6e86b_output.jar`，`version.json` 确认为 26.1.2；含 NeoForge 补丁，但本次未独立验证该源码归档的小版本 |

对 JAR 用 `javap -p -c` 核对了 ExecutingAffix、AdventureEvents、两项目 LivingEntityMixin、AttributeEvents、ALCombatRules、AuxDmgTracker 的关键签名和路径。源码提交与发行 JAR 版本一致，不据此宣称所有文件逐字一致。辅助下载和字节码输出在忽略的 `build/integration-audit` 中，不进入制品。

扫描了神化注册的 20 种 affix codec、15 种 gem bonus codec、Apothic Attributes 的属性注册及其主要事件/Mixin；这表示建立了接入类别清单，不是 35 种类型均完成运行验证。注册清单见 [AffixRegistry](https://github.com/Shadows-of-Fire/Apotheosis/blob/f2e76862ed9f4dcdf19c25400dc3d5478a46dd74/src/main/java/dev/shadowsoffire/apotheosis/affix/AffixRegistry.java)、[GemBonus](https://github.com/Shadows-of-Fire/Apotheosis/blob/f2e76862ed9f4dcdf19c25400dc3d5478a46dd74/src/main/java/dev/shadowsoffire/apotheosis/socket/gem/bonus/GemBonus.java)。

## 当前可以复用什么

- `CombatAccess / CombatWorld`：普通服务器命令和查询入口。
- `DamageModuleRegistrar / HealingRegistrar`：类型化计算与结算扩展。
- `SourceInstance / SourcePlan / InstallationHandle`：共同来源及跨域撤销。
- `EffectView / ParticipantInputs`：原版/受管效果查询和治疗参与者快照。
- 四类行为原语：数值、结算反应、周期、写入前保护。
- 事务准入、父子因果链、完成队列、应用证据及世界生命周期。

这些应保留。后续增加的适配能力也必须使用它们，避免另建一套神化专属事务、来源和计时系统。

## P0：先补执行归属和真实时机

### 1. 可注册的原生能力归属协议

当前 [VanillaCompatibilitySettings](../common/src/main/java/io/github/naimjeg/combatnexus/combat/damage/config/VanillaCompatibilitySettings.java)只枚举护甲、附魔、药效、固有减伤，执行者固定为原版/框架/禁用。公开注册器没有逐项声明“观察、路由、替换”的能力，也不能证明第三方某次贡献已执行或已抑制。

神化既在事件中修改数值，也在 Mixin 中执行副作用。例如 Apothic Attributes 在 `LivingHealEvent` 再乘受治疗倍率；如果适配器把同一倍率提前搬进 HealingPipeline，而外部事件继续执行，就会重复消费。捕获附魔总增量时，神化还会从 `EnchantmentHelper` 返回点加入词缀/宝石贡献，无法仅凭总量声称它是某个原版附魔的贡献。[AttributeEvents](https://github.com/Shadows-of-Fire/Apothic-Attributes/blob/44d0a8316fcb7965c26e8ff7eda341830ca63023/src/main/java/dev/shadowsoffire/apothic_attributes/impl/AttributeEvents.java)、[神化 EnchantmentHelperMixin](https://github.com/Shadows-of-Fire/Apotheosis/blob/f2e76862ed9f4dcdf19c25400dc3d5478a46dd74/src/main/java/dev/shadowsoffire/apotheosis/mixin/EnchantmentHelperMixin.java)。

需要补：按命名空间能力 ID 注册执行者、触发点、原生抑制位置、输入/输出事实和取消语义；一次操作带唯一关联身份及“已执行/待执行/不可拆分”证据。只有平台适配器确认抑制成功，才能启用替换执行。SDK 消费受限上下文，不取得任意写权限。不靠事件优先级或计算差值猜测所有权。

验收：同一件神化装备的伤害、治疗、附魔加成，在观察和替换模式下各执行一次；其他未接管副作用仍执行一次，取消无重放。

### 2. 完整攻击动作与投射物上下文

当前 [CombatAction](../common/src/main/java/io/github/naimjeg/combatnexus/combat/api/action/CombatAction.java)只有动作、会话、施动者身份。结算回调发生在一次领域操作完成后；这不等于发射、命中、物品格挡、横扫、攻击后效果、耐久处理等原生时机。

神化在发射时复制词缀/宝石到投射物，在碰撞、受伤和攻击后分别调用不同 hook。BloodyArrow 在发射时支付生命并修改箭伤，Catalyzing/Retreating 在物品格挡时赋药效或移动实体；它们不能挪成普通伤害结算反应。见 [AdventureEvents](https://github.com/Shadows-of-Fire/Apotheosis/blob/f2e76862ed9f4dcdf19c25400dc3d5478a46dd74/src/main/java/dev/shadowsoffire/apotheosis/AdventureEvents.java)、[BloodyArrowBonus](https://github.com/Shadows-of-Fire/Apotheosis/blob/f2e76862ed9f4dcdf19c25400dc3d5478a46dd74/src/main/java/dev/shadowsoffire/apotheosis/socket/gem/bonus/special/BloodyArrowBonus.java)。原版 `Player.attack` 和 `LivingEntity` 攻击路径也把部分后处理放在 `hurtServer` 外。

需要补：发射、碰撞、格挡、命中确认、攻击后、击杀确认等类型化事件；不可变动作事实包含蓄力、攻击强度、武器/槽位、发射时来源、直接实体与逻辑施动者、命中位置、伤害主体/部位。明确全盾吸收、零伤害、闪避和取消分别是否算命中。投射物携带发射快照，命中时换装不能偷换来源；多目标共享动作但保留独立操作身份。

还需要“写入前执行领域子操作”的明确边界：[MageSlayerBonus.onHurt](https://github.com/Shadows-of-Fire/Apotheosis/blob/f2e76862ed9f4dcdf19c25400dc3d5478a46dd74/src/main/java/dev/shadowsoffire/apotheosis/socket/gem/bonus/special/MageSlayerBonus.java)遇魔法伤害时先治疗，再返回降低后的伤害。当前数值计算禁止命令，而结算后再治疗会改变生死结果。应在计算阶段产出类型化操作计划，由已准入的应用阶段按明确顺序执行，记录子操作与父操作取消关系；不能放开计算回调直接写世界。

验收：射箭后换装、移除射手、跨维度、横扫多目标、击中方块、全格挡、取消后攻击副作用；来源不丢失、不串用，时机不移动。

### 3. 外部事实采集及属性语义适配

当前输入会采集实体拥有的注册属性，这使外部数值有机会进入快照；但 [AttributeReader.capture](../common/src/main/java/io/github/naimjeg/combatnexus/attribute/api/AttributeReader.java#L42)直接取 `AttributeInstance.getValue()`。神化 [LivingEntityMixin](https://github.com/Shadows-of-Fire/Apotheosis/blob/f2e76862ed9f4dcdf19c25400dc3d5478a46dd74/src/main/java/dev/shadowsoffire/apotheosis/mixin/LivingEntityMixin.java)在 `LivingEntity.getAttributeValue()` 处应用玩家属性开关。因此快照可能保留被关闭的数值；不能只说“已经读到全部属性”。

`CombatEntityFacts` 还只固定捕获生命、着火、原版装备槽及效果。没有公开采集器将神化 attachment、宝石纯度/槽位、词缀等级、冷却、世界等级等加入同一请求快照。属性贡献只记录 modifier ID/数值/operation，不能完整归因到词缀实例、宝石孔位和同次安装。

需要补：启动注册的只读事实采集器、有效属性值提供者、属性来源解释器和显示描述器；统一在计算前捕获。区分原始属性计算值、实际生效值及差异依据；外部属性不复制注册成第二份收益。采集失败标明不完整，不临时回读活实体冒充快照。

数值映射必须保留原义：神化暴击率允许超过 1，并具有多次暴击及衰减；穿甲/破甲、保护穿透和韧性使用独立公式，不能映射几个同名 ID 就算完成。当前暴击 collector 仅提供默认/强制/抑制决策，结果主要是一次布尔暴击。需要可注册的公式/决策策略、记录多次抽样与倍率的事实，并明确神化按目标 tick 共享闪避的规则。证据：[ALObjects](https://github.com/Shadows-of-Fire/Apothic-Attributes/blob/44d0a8316fcb7965c26e8ff7eda341830ca63023/src/main/java/dev/shadowsoffire/apothic_attributes/api/ALObjects.java)、[ALCombatRules](https://github.com/Shadows-of-Fire/Apothic-Attributes/blob/44d0a8316fcb7965c26e8ff7eda341830ca63023/src/main/java/dev/shadowsoffire/apothic_attributes/api/ALCombatRules.java)。

验收：属性开关、两颗相同宝石、同词缀不同等级、多重暴击、负护甲、保护穿透及同 tick 多段闪避；计算、来源解释、面板三者一致。

## P1：补齐领域操作及来源状态

### 4. 死亡保护、处决和生命状态校正

[ProtectionBehavior](../common/src/main/java/io/github/naimjeg/combatnexus/combat/api/behavior/ProtectionBehavior.java)只有前后生命参数和次数，缺少伤害来源、绕过标签、物品消耗与保护后副作用。原版图腾既设生命为 1，也执行 `DeathProtection` 的清除/施加效果；末影龙水晶恢复直接 `setHealth`，龙死亡阶段又可能回设生命。不能把所有 `setHealth` 都变成治疗。

神化 [ExecutingAffix](https://github.com/Shadows-of-Fire/Apotheosis/blob/f2e76862ed9f4dcdf19c25400dc3d5478a46dd74/src/main/java/dev/shadowsoffire/apotheosis/affix/effect/ExecutingAffix.java)先检查死亡保护，随后更新归属、记录战斗追踪、设血为零并执行死亡。将它替换成“99999 伤害”会改变防御、图腾和死亡时机。神化还有装备更新后保持怪物生命百分比的写入，它也不是治疗。

需要补：带原因与来源的生命状态校正、真实恢复、致死尝试/处决三类合约；死亡保护在明确边界竞争执行，支持条件、资源消耗、优先级和一次性回执；击杀通知来自确认死亡而非血量推断。SDK 提交意图，由领域应用端执行原生细节。

验收：图腾与受管保护同时存在、绕过图腾、处决被保护、保护副作用只执行一次、末影龙阶段切换、水晶归因和装备最大生命变化。

### 5. 原版效果、吸收资源和空间副作用

目前 `EffectView` 已统一查询，但 `EffectCommands.apply` 只接受受管效果，无法用公开命令向原版 owner 安装或刷新药效。保护清理能力只移除护盾；`PeriodicExecution` 只提交伤害/治疗。也没有击退、移动、点燃、生成/修改投射物及消耗物品的类型化命令。

神化 Overheal 按已确认伤害授予**原版吸收**，并维护额外吸收额度；这与“治疗溢出转成框架护盾”并非同一玩法。AuxDmgTracker 为附加伤害类型保存无敌帧和 lastHurt，再恢复主伤害状态，不能用随意清零无敌帧替代。见 [吸收应用](https://github.com/Shadows-of-Fire/Apothic-Attributes/blob/44d0a8316fcb7965c26e8ff7eda341830ca63023/src/main/java/dev/shadowsoffire/apothic_attributes/mixin/LivingEntityMixin.java)、[AuxDmgTracker](https://github.com/Shadows-of-Fire/Apothic-Attributes/blob/44d0a8316fcb7965c26e8ff7eda341830ca63023/src/main/java/dev/shadowsoffire/apothic_attributes/util/AuxDmgTracker.java)。`ghost_health` 在此次源码中可确认注册和说明，但未找到其恢复/消费执行器，不能据名称臆造完整资源行为。

需要补：原版效果 owner 的施加/刷新/移除命令与生命周期事实；吸收额度的归属、上限、授予/消耗证据；按伤害类别的命中间隔政策；有限的空间/实体副作用命令。全部遵守阶段、因果和预算规则，不能把 `Runnable` 包成一个万能写入 API。

验收：奶桶/死亡清除、原版药效刷新、属性不重复；Overheal 与原版吸收共存及上限变化；冷热附伤、点燃/减速、反伤循环和格挡后位移。

### 6. 持久状态、冷却与来源恢复授权

共同来源已存在，但持久化仅覆盖执行器已知的周期剩余时间、保护次数、触发计数和资源池。组件 codec 保存的是参数，不是插件运行状态。`TriggerLimit` 是次数限制，不是时间冷却；`DueQueue` 是队列数据结构，不是世界调度服务。神化 [AbilityCooldowns](https://github.com/Shadows-of-Fire/Apothic-Attributes/blob/44d0a8316fcb7965c26e8ff7eda341830ca63023/src/main/java/dev/shadowsoffire/apothic_attributes/api/AbilityCooldowns.java)按实体及 ID 记录触发时间、应用冷却缩减，生命周期也不能被来源卸下/重新安装重置。

一个具体 SDK 缺口：[SourceService](../common/src/main/java/io/github/naimjeg/combatnexus/combat/api/source/SourceService.java)只有 install/revoke/snapshot/refresh；恢复产生新句柄，而 `SourceView` 不含授权。通过普通 install 创建的持久施法来源，原插件重启后没有公开恢复回调或认领接口取得撤销权限。提供者自身内部协调能工作，不代表所有调用方都已具备恢复闭环。

需要补：安装/实体/动作作用域的类型化运行状态与当前格式 codec；独立冷却服务、一次性与周期调度、取消/刷新政策；由已注册来源 owner 接收恢复句柄或受控认领。明确状态是否随卸装、驱散、死亡、换维度清除。查询身份不能直接变成撤销授权，也不引入版本转换机制。

验收：冷却中卸装/重装、死亡不该清零的冷却、持续伤害刷新、重启后准确驱散某一施法者、另一个来源不受影响。

### 7. 定义目录、数据编译和展示扩展

[DefinitionView](../common/src/main/java/io/github/naimjeg/combatnexus/combat/api/DefinitionView.java)目前只列 ID 和 generation。SDK 缺少“按已采用定义 ID 施加”的完整公开路径，插件仍需自行构造 `SourcePlan`。治疗只有处理器注册；Effect owner、额外事实、展示描述等没有同等级注册能力。`AttributeUnits` 也是固定目录。

需要补：类型化只读定义描述、按 ID 发起命令、公开参数编译/校验与重载通知、外部事实/显示描述注册。适配器将神化当前 registry/data 解析成不可变值，原子发布；在途请求保留原快照。定义 generation 继续用于运行时一致性。客户端接收描述与结果，不持有神化运行对象或重算权威公式。

验收：重载中词缀/宝石定义改变、删除、校验失败；旧请求解释保持完整，新请求使用新定义，客户端显示来源和真实单位。

## 接入覆盖清单

| 内容 | 当前公开 API 可做的部分 | 尚缺底层 |
| --- | --- | --- |
| 原版基础伤害、护甲、普通治疗 | 请求、管线、结算、查询 | 外部可注册归属与原生采样协议 |
| 原版附魔、药效、食物恢复 | 部分总贡献、效果查询和治疗路由 | effect component 语义采集、原版效果 owner 命令、触发来源完整性 |
| 图腾、龙水晶、龙部位和死亡阶段 | 普通伤害/治疗基础 | 死亡保护、生命校正、部位/主体事实及精确平台接线 |
| 属性/多属性/属性开关词缀，属性/多属性/全属性宝石 | 读取多数数值、安装受管贡献 | 有效属性采样、原生所有权与实例来源解释 |
| 减伤、附魔、MobEffect 词缀/宝石 | 部分数值和受管效果 | 公式政策、动态附魔语义、原版效果命令 |
| cleaving、psychic、thunderstruck、executing | 能提交额外伤害 | 范围/动作事实、空间操作、处决和冷却 |
| magical、spectral、bloody_arrow、anti_gravity_arrow | 普通伤害来源角色 | 发射/碰撞上下文、投射物参数、生命成本和状态转移 |
| catalyzing、retreating、leech_block | 治疗及部分结算反应 | 物品格挡事件及对应效果/位移命令；物品格挡不等于护盾池抵扣 |
| draw_speed、arrow_velocity、elytra_flight、cooldown_reduction | 可读取属性 | 使用物品、运动/飞行及冷却的独立消费端 |
| mageslayer | 治疗与减伤分别存在 | 写入前子治疗的精确时序，不能挪到结算后 |
| enlightened、festive、telepathic、stoneforming、omnetic、radial；durability、drop_transform、frozen_drops | 现有 combat SDK 不能完整承载 | 掉落、采掘、耐久/物品使用等领域；需逐 hook 保留原义 |
| 词缀/宝石生成、重铸、切割、世界等级与入侵、世界生成 | 可把最终物品或生物贡献送入战斗 | 内容生产模块继续归神化；若要求接管这些模块，需独立设计，不能宣称战斗 SDK 已覆盖 |

外部注册的未知 affix/gem/effect component 不可能仅靠类型名字自动完整翻译。适配器应登记已支持的语义类型；未知类型保留原执行并报告覆盖不足，不能偷偷执行一遍后再安装第二份。

## 推荐推进顺序与通过条件

| 顺序 | 工作 | 必须通过的公共 API 示例 |
| --- | --- | --- |
| 1 | 归属协议、动作时机、外部事实采集 | 一个原版适配模块和一个神化适配模块，零 internal/core/SPI 导入，贡献恰好一次 |
| 2 | 数值策略、投射物快照、来源明细 | 神化多重暴击、穿透、属性开关、发射后换装均保持原义 |
| 3 | 死亡/保护、原版效果、资源和空间命令 | 图腾+处决、Overheal、格挡位移、冷热附伤，不依赖插件直接写实体 |
| 4 | 状态、冷却、恢复认领、定义与展示注册 | 重启后恢复并撤销正确实例，冷却不被卸装刷新，解释与数值一致 |
| 5 | 逐类型完整覆盖及非战斗模块边界 | 对 20 类 affix、15 类 gem bonus 和原版代表性路径逐项登记实现/保留原生/未覆盖；不能只统计注册数 |

运行验收继续使用 AutoJS：先隔离原版语义，再启用锁定神化组合，逐项比较无适配/观察/替换结果。增加公开 API 示例的编译边界检查及实际应用次数、来源、取消、死亡、保存断言。仅成功启动神化，或现有隔离套件全部通过，都不能作为神化完整接入证明。

## 本轮实际修改与验证

已移除 `BehaviorType` 的格式版本参数、行为存储包装、实体存储版本字段及旧装备字段的迁移提示；当前格式直接保存类型 ID 和参数，严格校验当前字段。没有增加旧格式读取或转换路径。SDK 文档和脚本同步更新。

活动 target `clean build sdkExampleJar`、common 独立 `clean build`、公开签名/源码边界、制品检查、脚本语法及 `git diff --check` 通过。隔离 AutoJS 29 组、1,396 项检查通过，包含当前格式保存恢复、重载及真实重启。本次未运行神化组合验收；本评估属于源码/字节码证据及设计判断。

第一次跨 tick/重启验收受到随机世界地形影响：固定 Y=100 测试点实际为石头、地表高度 160，测试实体遭遇窒息。将这两处跨 tick 夹具移至 Y=512 后重新执行全套通过；未改变业务逻辑或放宽断言。客户端网络/展示未修改，本次未重复其运行检查。
