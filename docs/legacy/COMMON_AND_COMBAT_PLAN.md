# common 下沉、模块拆分与战斗接管审计计划

日期：2026-11-03。范围：当前工作树、Minecraft 26.1.2 / NeoForge 26.1.2.126、Java 25。本文是静态审计与实施计划，不代表已经实施迁移或完成新增行为的运行验证。

实施状态：截至 2026-11-04，已完成两轮 target 下沉，并补齐 common 治疗、护盾、恢复、Effect 生命周期及只读展示服务，由显式 `CombatRuntime` 组合；尚未接入新增游戏行为。具体实现与验证见 [SCAFFOLD.md](SCAFFOLD.md)。下文保留 2026-11-03 重构前静态审计和运行验收目标，不将历史候选列表或测试数量当作当前统计。

后续同日已实施共享事务内核：服务器级 CombatServerRuntime 管理准入/通知，damage 门面委托公共身份、作用域、预算、发布门禁与回调基础设施，治疗和护盾接入同一内核。世界服务仍未自动接通游戏。最新 API 变化、测试删减及运行验证见 SCAFFOLD.md 顶部。

2026-11-04 后续落地：damage 已改为消费 attribute 的请求级完整快照；倍率运算和公式配置归 formula，原版抗性 Effect 读取归 effect。新增快照冻结/来源与效果撤销 GameTest，详见 DEVELOPMENT.md 和 SCAFFOLD.md。世界 CombatRuntime 自动接入及新战斗子域的实际应用仍未完成。

## 1. 结论与统计口径

- 迁移清单的 482 个 Java 文件均有当前落点：common 403 个、42,487 行；target 79 个、21,272 行。行数含注释和空行，不含新增入口类、资源和 src/test。
- target 中有 8 个 `*GameTests.java`，合计 12,158 行，占 target 迁移源码约 57.2%。不能把这部分当作平台业务过重，也不能为了提高 common 比例搬走真实平台测试。
- 已明确列出 **18 个可拆分下沉的生产类候选**：第一批 7 个、当前文件共 2,368 行；第二批 11 个、当前文件共 3,291 行。合计 5,659 行是候选文件覆盖量，**不是可直接迁移行数**；事件包装、事件身份映射、网络发送等仍须留在 target。
- 403 个 common 文件约占迁移文件的 83.6%，但并非平台隔离完成。common 没有直接 `net.neoforged` 引用；按显式 import 的末端类型与 target 类名匹配，有 70 个 common 文件引用 target damage 类型（这是静态依赖线索，不是完整依赖图）。`common/build.gradle` 也明确编入了参考 target 源码。
- 除 target → common 外，现有 common 内还应做 damage → attribute/formula/effect/presentation 的职责迁移；两种迁移不能重复计数。

原始迁移出处保留在 [damage-source-manifest.json](legacy/migration/damage-source-manifest.json)。后续拆类应增加新旧类映射、职责及验证记录，保留原 SHA-256 出处，不将重构后的文件误当成原始字节一致。

## 2. 可继续下沉的具体内容

以下路径相对于 target 的 `src/main/java/io/github/naimjeg/combatnexus/damage/`。

### 第一批：薄适配拆分，7 个文件

| 当前类 | 可下沉内容 | target 保留内容 / 迁移条件 |
| --- | --- | --- |
| `core/registry/DamageChannelRegistry` | 通道定义、校验、查询、缓存与生命周期状态 | `ServerStoppingEvent` 监听；注册依赖通过明确合约提供 |
| `core/template/DamageTemplateRegistry` | Java/数据包模板存储、合并与快照 | 停服监听；保留 reload 原子替换语义 |
| `core/rule/DatapackDamageRuleStore` | 规则快照与更新逻辑 | 停服事件转发 |
| `core/security/DamageModuleItemSecurity` | 结构校验、可信创建路径、执行许可与缓存 | 停服事件转发；数据组件访问合约，不能弱化服务端权限验证 |
| `client/DamageDummyModel` | 模型几何、姿态计算 | `EntityRenderersEvent.RegisterLayerDefinitions` 注册 |
| `client/DamageDummyRenderer` | 渲染实现与状态提取 | renderer 事件注册；共享客户端代码不可由专服入口加载 |
| `client/screen/DamageDummyScreen` | UI、输入和编辑状态 | 用发送接口替换 `ClientPacketDistributor.sendToServer`；保留原服务端校验 |

这里的“薄适配”不等于整类原样移动。前四项仍引用现有注册器，先允许保持参考 target 构建，再用 registry/data-component 合约消除逆向依赖。禁止把 NeoForge 事件类型换一个包名继续从 common 暴露。

### 第二批：先建合约再下沉，11 个文件

| 当前类（完整类名按所在子包定位） | 拆分方向 |
| --- | --- |
| `DamageEventSnapshot`、`DamageModuleContextSpec`、`DamageModuleContext`、`DamageModuleContextFactory` | 4 个文件：common 使用输入快照、来源、攻击上下文与计算状态；target 负责把 `LivingIncomingDamageEvent` 转为快照，使用结果执行写回。当前约 1,005 行的 Context 是优先解耦点 |
| `DamageRequestSubmissionTracker`、`DamageSettlementState`、`DamageSettlementTracker`、`DamageModuleTransactionTracker` | 4 个文件：common 保存请求匹配策略、结算状态机、发布门禁和诊断事务；target 保留事件/container 身份、Pre/Post 采样及真实 hurt 调用栈绑定。不能丢掉嵌套、取消、一次发布、无 Post 完成等语义 |
| `bridge/vanilla/VanillaDamageSourceProfile` | 数据记录与分类规则共享；`Tags.DamageTypes.IS_MAGIC` 由 target 分类适配提供，不猜测其他平台等价关系 |
| `entity/DamageDummyEntity` | 实体行为可共享；`Tags.DamageTypes.IS_TECHNICAL` 判定由 target 合约提供；实体注册留 target |
| `client/damage/DamageNumberRenderer` | 渲染函数接受相机状态、PoseStack 等明确参数；`RenderLevelStageEvent` 订阅与参数提取留 target |

建议最小合约集合：伤害输入快照、结果写回端口、结算观察值、属性/组件查询、生命周期通知、客户端发送端口。按实际调用拆分，不建立一个包揽所有能力的 PlatformHelper，也不使用反射或运行时版本判断。

### 不计入上述 18 个的后续拆分机会

- `ModAttributes` 的定义清单可共享，DeferredRegister、DeferredHolder 与注册事件保留 target；这也是 attribute 模块工作的一部分。
- `DamageSettlementEventPublisher`、注册事件、启动/重载 handler、广播与 payload handler：可抽少量分发/选择策略，但保留薄平台类，不以文件搬迁为目标。
- `ModConfigSpec`、附件、菜单工厂、六个 Mixin、真实 GameTest 注册、NeoForge 入口和网络注册继续留 target。Mixin 的算法可以调用 common，但注入点属于 26.1.2 适配。
- 原 92 个测试 Java 文件保持覆盖。可在契约稳定后把纯语义测试独立组织；不要把引用 NeoForge 事件的测试直接当作跨平台测试。

## 3. 并入已规划模块的内容

目标顶层模块为 `combat`、`attribute`、`formula`、`effect`、`presentation`。`combat` 统一概括伤害、护盾、治疗与自然恢复四个结算子域；`damage` 专指其中的伤害计算与应用。现有源码仍在 `damage/`，本文引用的文件路径和现有 API 均保持原名；`combat` 是目标架构命名，不表示本轮已经完成包迁移。

| 目标模块 | 现有资产 | 迁移边界与新增工作 |
| --- | --- | --- |
| `attribute` | `ModAttributes` 中 26 个属性注册定义、`DamageModuleAttributes`、`DamageAttributeMappings`、属性读取/聚合逻辑、假人属性视图的可复用部分 | 定义 ID/单位/范围/同步与来源；原版 attack_speed、movement_speed、armor、armor_toughness、max_health 等直接映射。注册留 target，攻击分类与伤害通道消费留 damage。假人可编辑协议不是玩家只读快照 |
| `formula` | `ArmorMitigationProcessor` 的数学部分、`ResistanceMitigationProcessor.reductionFor/totalRating`、`DamageComponent`、`PreMultiplierSet`、属性缩放和暴击倍率中的确定性计算 | 抽输入→输出及计算明细，不把 Entity、事件、可变 damage Context 和注册器带入 formula。暴击随机采样、强制/禁止决策和优先级仍由 damage 编排；保持现有护甲/负抗性语义 |
| `effect` | `VanillaMobEffectBridge`、力量/虚弱贡献采集、抗性 Effect 的来源与等级读取、现有 Effect 条件查询 | effect 提供带生命周期和稳定来源 ID 的属性贡献、周期效果及动作限制，包含减速、眩晕、禁锢等 buff/debuff；combat 消费结算贡献，平台动作入口执行限制。迅捷、疲劳、生命恢复及控制效果需新增完整生命周期支持，不能凭现有桥接声称已经实现 |
| `presentation` | `client` 下 43 个文件（common 32 / target 11）、`api/client/phrase` 25 个、`api/display` 7 个、`presentation` 1 个：合计 76 个文件的展示资产池 | 先迁通用数字/百分比格式、文案协议、来源明细和界面组件。伤害专用解释器作为 damage 的 presentation 适配；假人模型与编辑器可留诊断功能。76 是审阅资产池，非全部应无条件搬入新模块 |
| `combat`（由现有 damage 职责拆分演进） | 请求、归属、通道、规则/词条/转换、暴击决策、阶段编排、结算与派生预算 | 内部分为 damage、shield、healing、regeneration 四个子域；通过公开 attribute/formula/effect 合约消费快照。通用属性、公式、效果生命周期与展示继续拆出，不承担完整移动、资源或动作状态机 |

优先修复 API 反向依赖：`DamageModuleAttributes → ModAttributes`、`DamageSettlementSnapshot → DamageSettlementState`、模板 API → target registry；`DamageModuleApi` 等门面可委托内部服务，但不能对新模块暴露内部实现类型或要求其导入内部包。涉及 Snapshot 的受限构造/调用者检查时保留可信发布边界，不能简单删除检查。

`healing_received` 已注册且注释明确为 Reserved，当前并没有治疗消费实现。应在 attribute 中保留定义与兼容决策记录，待治疗管线接入；面板须标注未生效，不能把注册成功视为功能完成。

### Effect 的 buff/debuff 与执行边界

`effect` 包含属性型、周期型和控制型效果，统一管理添加、等级、持续时间、叠加、刷新、驱散、免疫及来源。它负责描述有效贡献和限制，结算子域或服务端动作入口负责实际应用。

| 类型 | 例子 | 执行边界 |
| --- | --- | --- |
| 属性修饰 | 减速、迅捷、虚弱、治疗增益/重伤 | 通过 attribute 合约复用原版属性或已有扩展属性，避免重复收益 |
| 周期效果 | 中毒、流血、再生 | effect 管理来源与有效期，经公开合约提交 damage/healing；自然恢复由 regeneration 调度，原版已调度的效果不再重复 tick |
| 临时护盾 | 有持续时间的护盾增益 | effect 管理施加与撤销原因，shield 管理余额、抵扣及破裂 |
| 禁锢 | 限制移动 | 分别定义行走、跳跃、位移技能与外力位移政策；是否允许转向和攻击须明确 |
| 眩晕 | 限制移动、攻击、使用物品等动作 | 在服务端对应动作入口校验，覆盖玩家及生物 AI，不仅设置移速为零 |
| 沉默、缴械 | 分别限制施法、武器攻击 | 按动作类别限制；施法由提供者通过公开限制合约接入，不默认禁止所有动作 |

控制状态按有效来源合并；移除或到期一个效果，只撤销该来源的限制，不解除其他效果仍施加的限制。客户端只展示或预测反馈，服务端负责最终拒绝和状态同步。效果层不承担完整动作状态机；当前 target 适配执行动作限制，后续如建立 action 模块再迁入对应执行器。

验收覆盖减速叠加/撤销、不同控制重叠和先后到期、刷新/升级/驱散/免疫、玩家输入与生物 AI、位移和使用物品限制，以及死亡重生/换维度/存档恢复后的状态清理。上述效果均为规划范围，尚未实现。

## 4. 原版战斗系统的接管覆盖与疑似缺口

判断分三类：已接管；保留原版、需要观测和测试；明确缺少新模块能力。没有监听一个事件不等于游戏机制失效，也不意味着必须重写原版。

已存在的基础包括：近战冷却/暴击桥接、投射物捕获、重锤与矛捕获、难度缩放、攻击附魔、力量/虚弱、抗性 Effect、保护附魔、护甲与自定义抗性、荆棘桥接、请求归属、派生预算及结算快照。删除前的 GameTest 曾包含吸收/过量伤害和原版受伤冷却拒绝场景，不能把这些列为从零缺失。

| 优先级 / 项目 | 当前证据与风险 | 计划归属及验证 |
| --- | --- | --- |
| **P0：减伤所有权不唯一** | `DamageEventWriter` 在 COOPERATIVE 下不抑制原版；护甲、保护附魔、抗性 Effect 处理器仍计算自身贡献。配置说明已写明可能 double mitigation。这是既有行为，不是新迁移引入的回归 | damage/formula/effect：按减伤类别明确“原版执行”或“本模块执行”。三种模式分别测试仅护甲、仅保护、仅抗性及组合；行为修复单独提交，不混入迁移 |
| **P0：先天减伤被抑制但未找到重建器** | 默认 FULL_REPLACEMENT 将 `INNATE_RESISTANCE` 归零，当前 builtin 没有对应重建处理器。只影响实际使用该 Reduction 的路径，不能据此宣称所有生物免疫都被移除 | damage 适配：构造真实先天减伤贡献测试，再覆盖相关实体；未重建时是否保留原版需明确产品语义 |
| **P0：盾牌/格挡阶段和结算解释** | incoming 已写入本模块减伤后的值；平台格挡继续在之后发生。未发现独立格挡策略。部分格挡、盾牌耐久、破盾可能面对已减伤数值，顺序需要确定 | damage 记录 blocked amount/reason，未来 action/guard 策略负责格挡。测试正背面、全/部分格挡、穿盾标签、破盾、耐久与零生命伤害 proc |
| **P0：结算不等于最终死亡** | `DamageSettlementState.capturePost` 记录生命/吸收差；原版图腾判断在 hurtServer 的后续阶段。现有伤害快照不能自动充当击杀确认 | damage 增加明确死亡/救活结果接口；测试图腾、死亡取消、Post 中治疗、嵌套伤害、过量击杀；击杀奖励不能仅依据一次 healthAfter |
| **P1：受伤无敌帧与追加伤害** | 已保留 vanilla cooldown 且删除前有拒绝测试；当前数值写回发生在无敌帧比较之前。多通道/子请求与 lastHurt 的顺序仍需矩阵验证 | damage：同 tick 小/大伤害、DOT、反伤、AOE、bypasses_cooldown；不能为支持 proc 全局关闭无敌帧 |
| **P1：冻结额外倍率、头盔减伤与耐久** | 本地 26.1.2 LivingEntity 源码在 incoming 后还有冻结特殊倍率、DAMAGES_HELMET 的减伤/耐久逻辑；未发现独立桥接解释 | damage 先记录为原版后续调整，验证来源归因与最终值；换算顺序变化须单独评估，不能把差额全判作其他模组篡改 |
| **P1：治疗、再生、吸血** | `healing_received` 未消费；没有统一治疗请求/结算管线。生命恢复不是负伤害 | attribute/effect + combat 的 healing/regeneration 子域：实际治疗、治疗增减、自然恢复、药水/瞬时效果、吸血、过量治疗政策。使用有效结算量，限定派生来源与递归 |
| **P1：Effect 的免疫与非伤害贡献** | 抗火等路径可在 incoming 前返回；迅捷、缓慢、疲劳、急迫、虚弱、生命提升、伤害吸收、瞬时治疗/伤害等不全是伤害倍率 | effect/attribute：先观测原版修饰项和免疫，再决定是否替换；覆盖升级/刷新/过期、死亡重生、换维度、存档恢复；同一贡献只执行一次 |
| **P1：横扫、副手、附魔副作用** | 普通伤害可进入总入口，但没有专用横扫/副手动作快照即不能证明主副手、攻击强度、目标数和词条归属正确。附魔伤害桥接也不等于接管火焰附加、击退、战利品等副作用 | damage + action/兼容适配：主目标/次目标、换手、投射物飞行中换装、穿透多命中、荆棘与附魔触发去重 |
| **P2：击退、触及、命中判定、攻速调度** | 矛捕获包含击退标记，但没有统一运动/动作策略；attack_speed 已列入未来属性映射 | attribute 复用原版属性；action 负责距离、节奏、运动与命中，服务端校验。不要把移速/攻速简单乘到伤害上 |
| **明确保留：环境与特殊伤害边界** | `DamageSourcePolicy` 排除无注册 ID、bypass 自定义标签、BYPASSES_INVULNERABILITY；虚空、kill 风格伤害的排除有代码注释依据 | 文档明确不接管的范围；环境伤害/药水/爆炸/宠物归属补矩阵。非 LivingEntity 和直接 setHealth 不保证经过 damage API，不用全局拦截 setHealth 来补洞 |

原版调度对照：本地缓存中带源码的 Minecraft 26.1.2 `LivingEntity.hurtServer/actuallyHurt/heal`、`Player.actuallyHurt`；精确 NeoForge 26.1.2.126 sources 中的 `DamageContainer`、`LivingShieldBlockEvent`。缓存中的 MC 合并源码只用于顺序核对，不作为仓库编译输入，也未据此宣称覆盖所有实体重写。正式实现必须对当前运行 target 再做 AutoJS 脚本运行验证。

## 5. 常见模组能力及扩展边界

以下是能力参考与接口需求，不代表这些模组已在本项目兼容，也不代表所引分支支持当前 NeoForge target。实现兼容时另行固定双方版本并做组合运行测试。

| 能力族 / 官方项目参考 | 本项目已有基础与缺口 | 建议模块 |
| --- | --- | --- |
| [Apothic Attributes 的属性定义](https://github.com/Shadows-of-Fire/Apothic-Attributes/blob/26.1/src/main/java/dev/shadowsoffire/apothic_attributes/api/ALObjects.java)：吸血、过量治疗转吸收、治疗倍率、可恢复额外生命等 | 已有暴击、通道增伤/抗性；新增能力必须明确实际生命/吸收伤害、上限及恢复延迟，不能仅注册空属性 | attribute、formula、combat |
| [Better Combat](https://github.com/ZsoltMolnarrr/BetterCombat#%EF%B8%8F-features)：连招、双持、攻击前摇、多目标挥击、武器数据定义 | 已有 damage 请求不足以表达整次动作。需要 actionId、使用手、武器快照、连招段、目标序号与单次触发预算；优先接入既有动作提供者 | 建议 action + damage 适配 |
| [Epic Fight](https://github.com/Antikythera-Studios/epicfight)：冲击、硬直、防眩晕、重量、体力消耗等 | 不是伤害通道或一个 resistance 数值可以代替；需要动作/控制状态和资源消耗生命周期 | 后续 action、resource、effect |
| [Iron's Spells 官方文档](https://github.com/iron431/Irons-Spellbooks-Docs/blob/main/index.md)及[属性语言定义](https://github.com/iron431/irons-spells-n-spellbooks/blob/1.21/src/main/resources/assets/irons_spellbooks/lang/en_us.json)：施法、法力、冷却相关能力 | 伤害管线已有来源/请求/通道基础；需要施法者/召唤物归属、法术与学派 ID、持续施法、打断、冷却、资源支付，不能只靠 magic 标签认定完整法术兼容 | damage 归属 + effect；后续 action/resource |
| 通用 RPG 需求：固定/比例穿透、闪避/格挡、DOT/HOT、反伤、命中/暴击/击杀触发、装备套装与饰品来源 | 已有 armor effectiveness 操作、规则/词条和外部装备来源 API，可复用；尚缺统一穿透顺序、治疗消费、效果生命周期和动作语义 | attribute/formula/effect；复杂触发后续独立 proc，装备来源先复用现有 SPI |

护盾、治疗、生命自然恢复是近期必做范围，统一归入 `combat` 的 shield、healing、regeneration 子域，与 damage 并列。不新增独立 vitality 模块，也不把这些行为继续塞入现有伤害流水线。action、resource、proc 暂不创建，在出现具体业务和稳定合约时再建立；通用 affix/equipment 也不应仅因 damage 已有词条就提前整体抽象。

## 6. 必做补充：护盾、治疗与生命自然恢复

这是明确的实现范围，不能只列为远期能力参考。由统一的 `combat` 模块承载四个职责独立的子域，近期先完成服务端最小闭环，再接面板。以下为待实施的合约和语义方案，不是当前已有 API。

### 6.1 模块职责

| 模块 | 责任 |
| --- | --- |
| `attribute` | 生命上限的原版映射、`healing_received`、治疗输出倍率、自然恢复量/倍率、护盾容量/恢复参数的定义、单位和快照；只有消费者同时落地才注册新属性 |
| `formula` | 有限值校验、治疗增减与封顶、护盾抵扣计算、恢复速率换算；纯函数不负责扣血/回血和 tick 调度 |
| `effect` | 治疗增益/重伤、再生、临时护盾等带来源和生命周期的贡献；驱散、升级、刷新、过期时正确替换/撤销 |
| `combat.damage` | 伤害计算与应用：伤害请求、减免和结算；在确定的抵扣阶段调用 shield 合约，将护盾吸收量纳入结算，提供吸血所需的确认结果 |
| `combat.shield` | 护盾抵扣、破裂、恢复：管理护盾池状态、容量、消耗、到期及只读快照 |
| `combat.healing` | 治疗修正、实际恢复和溢出：独立的治疗请求、应用和结算，不作为负伤害进入 damage 流水线 |
| `combat.regeneration` | 自然恢复条件、周期调度：消费 attribute/effect 快照，最终提交给 healing；不直接改生命值 |
| `presentation` | 显示生命、原版吸收、自定义护盾、每秒恢复与条件，以及本次实际治疗/护盾损失；客户端不运行权威公式 |
| target | `LivingHealEvent` 等事件、原版调用来源捕获、tick/存档/同步和伤害最终应用位置的适配；如需 Mixin，注入点仍留 26.1.2 target |

当前生命、吸收量、护盾余额属于运行状态，不应全部注册成属性。属性表达容量或倍率，状态由服务端持有。生命上限复用原版，不能额外创建同义 max_health。

### 6.2 治疗闭环

- 设计 `HealingRequest` / `HealingSettlement`：目标、来源实体/ID、原因（自然恢复、Effect、物品、吸血、技能、未知外部）、请求量、修正量、实际恢复量、溢出、取消原因及关联伤害 ID。
- 建议固定顺序为基础治疗 → 来源侧增益 → 受疗侧增减 → 非负/有限值校验 → 生命上限裁剪 → 应用 → 结算。倍率基值与百分数单位由 attribute 明确；满血实际治疗为零，不误触发“有效治疗”奖励。
- 接入原版 `heal` 和外部模组的常规治疗调用，保证 `healing_received` 只消费一次。公开治疗 API 调用原版 heal 时使用有作用域的关联标记，避免进入事件后再重复计算；未知外部来源标为 unknown，不推测施法者。
- `LivingHealEvent` 可修改/取消治疗，但不能仅凭该事件宣称已经得到最终有效治疗。需在真实应用前后捕获或由统一应用端确认结果；检查其他监听器、嵌套治疗和上限变化。
- 死亡实体不经普通治疗复活；复活、图腾独立处理。默认溢出治疗丢弃，转吸收/转自定义护盾必须显式启用并有容量上限。重伤按受疗修饰项处理，不表示负治疗伤害。
- 吸血默认以确认的有效生命伤害为基础；是否计入吸收/自定义护盾损失是独立策略。需排除未应用、被取消和超出实际损失的伤害，并限定反伤/派生请求是否能触发。

### 6.3 自然恢复与周期治疗

必须区分三种来源，统一进入治疗结算，但不统一重写其触发节奏。

| 路径 | 首期接入方式 | 必须保持的边界 |
| --- | --- | --- |
| 原版饱食度自然恢复 | 保留 `FoodData.tick` 的调度，捕获自然恢复来源并接入治疗入口 | 遵守 natural health regeneration 游戏规则、饱食度/饱和度与消耗；不额外每 tick 再执行一份“原版回血” |
| 原版生命恢复 Effect | 保留或明确替换 `RegenerationMobEffect` 的唯一执行路径，记录 Effect ID/等级 | 不同时保留原版 heal 和新 scheduler；刷新/升级/移除后没有旧贡献残留 |
| 自定义每秒生命恢复 | combat.regeneration 的服务端调度器根据 attribute/effect 快照提交周期治疗 | 明确单位 HP/s，按游戏 tick 累积小数；默认不做离线补偿。是否受原版自然恢复开关、战斗状态或受击延迟影响必须独立配置并说明 |

本地 26.1.2 源码确认：原版自然恢复有饱和度充足时的 10 tick 分支及高饱食度时的 80 tick 分支；Regeneration Effect 另行调用 heal。具体恢复结果还受事件修正影响，不能将原版自然恢复粗略固定为单一 HP/s。

自然恢复、护盾恢复与吸血必须有不同来源 ID；离开世界/死亡/重生/换维度时重置或迁移 scheduler 状态，防止重复计时器。受击暂停恢复时需定义“受击”按已应用伤害、生命损失还是护盾损失计时，建议自定义护盾的恢复延迟由有效护盾损失也触发。

### 6.4 护盾语义与伤害接入

“护盾”在此明确覆盖独立的可消耗护盾池，不仅是盾牌格挡或吸收心。三者分别处理：

- 盾牌格挡：属于命中/格挡阶段，保留原版 `LivingShieldBlockEvent` 的方向、破盾和耐久语义。
- 原版伤害吸收：保留 vanilla absorption 消费及 `MAX_ABSORPTION` 约束，纳入 combat 的只读状态与结算观察；不再另扣相同额度。
- 自定义护盾池：按稳定来源 ID 保存当前量、上限、优先级、允许抵扣的伤害类别、到期时点、恢复延迟/速率及叠加规则。默认同来源替换/刷新，不能每 tick 增添永久池。

建议首版顺序：沿既有伤害管线确定最终待应用伤害 → 自定义护盾 → 原版吸收 → 生命。盾牌/无敌帧仍在平台既有位置；不在此次新增功能中悄悄重排现有减伤。真伤是否穿护盾必须单独定义：建议默认只跳过相应减伤，不自动穿所有护盾；技术性/明确 bypass 伤害遵从已有排除策略。

护盾扣除应发生在拒绝/格挡等前置判断之后，并尽量贴近最终应用，不能在 Incoming 一开始就永久扣护盾，再让后续事件取消伤害。先验证当前固定版本的可靠落点；必要时用预留/确认/释放或精确应用点适配。每次伤害只提交一次护盾损失；全额护盾吸收也必须有终态结算，即使最终生命伤害为零。

新增快照字段区分 `shieldDamage`、`absorptionDamage`、`healthDamage`、未应用量，不能覆盖旧字段含义。伤害已结算通知不能为补护盾数据发第二遍。护盾破裂、到期、驱散和主动移除是不同原因，只有真实消耗跨越零点才触发一次破盾事件。按通道护盾还需要保存抵扣后的通道分配；若平台后续只剩总量，首版先支持总量护盾，不伪造精准通道结果。

### 6.5 最小验收矩阵

1. 治疗：满血、缺血、零/负/非有限输入、受疗倍率、重伤、取消、过量、死亡目标、嵌套回调；原版/外部/API 三条路径倍率均只生效一次。
2. 自然恢复：游戏规则开/关、饱和度有/无、饱食度阈值、满血、再生 Effect 升级刷新移除、自定义小数 HP/s、受击延迟及世界生命周期；原版食物消耗与回血节奏保持或有明确变更记录。
3. 护盾：部分/完全吸收、多池顺序、同来源刷新、恢复上限、到期/驱散、只破一次、取消/无敌帧不误扣、真伤/bypass、原版吸收同时存在、子请求/反伤、存档重载与重生策略。
4. 联动：护盾承伤是否触发吸血/受击/暂停恢复；全额护盾吸收仍产生一次结算；图腾和死亡取消不误判击杀；溢出治疗转护盾不构成无限触发循环。
5. 同步：服务端快照含修订号与来源，客户端无法提交治疗量、护盾余额或恢复时间；专服不加载展示类。

## 7. 实施顺序与验收

1. **P0，先锁定接管语义。** 增补上述减伤模式、先天减伤、格挡顺序、死亡/图腾测试；把已知配置叠算行为写成明确基线，再以独立变更决定是否修复。输出按阶段、机制、执行者、来源、验证场景组织的覆盖表。
2. **结构迁移第一批。** 拆 7 个薄适配候选；不改变注册 ID、数据协议和伤害数值。停服后重启、重载失败回滚、专服加载与客户端展示均有相应验证。
3. **attribute 合约及第二批核心解耦。** 定义属性目录/只读快照/来源 ID；解耦 Context 的 NeoForge 事件；状态机采用观察值，事件关联留 target。先让 damage 消费快照，再迁现有属性定义与读取。保持现有 public API 兼容门面或明确记录破坏性变化。
4. **formula 纯计算。** 用当前结果编写 AutoJS 语义验证脚本，再抽护甲、抗性和倍率公式。覆盖零/负值、NaN/Infinity、溢出、封顶、顺序、转换、真伤与多来源；测试证明抽取前后语义一致。
5. **effect 与 combat 子域（必做）。** 先建立治疗请求/结算、原版自然恢复与再生来源接入，再建立护盾池及伤害抵扣结算；完成第 6 节的所有权和验证矩阵。同时接原版修饰项的观测与唯一贡献策略，覆盖迅捷、疲劳及减速、眩晕、禁锢等控制效果的服务端执行与生命周期。随后接自定义每秒恢复、`healing_received`、吸血与穿透。穿透数值属于 attribute/formula，治疗与护盾提交分别属于 combat.healing 和 combat.shield，自然恢复调度属于 combat.regeneration；effect 只通过公开合约提交或提供贡献，不调用结算内部实现。
6. **presentation。** 先迁共享格式与来源展示，定义玩家专用只读快照，再做可配置按键面板。保留假人编辑端点的权限隔离；验证快照修订号、目标限制、限频、断线/换维度失效及客户端不计算权威值。
7. **按需扩展动作与模组兼容。** 首先覆盖副手/多目标/法术和召唤物的归属，再决定 action/resource/proc 的实际范围；不把对某一模组名称或标签的识别当作完成兼容。

每个代码阶段在 `targets/neoforge-26.1.2` 执行 `gradlew clean build`，并执行 common 编译及迁移基线检查；涉及事件、Mixin、网络、资源还要使用 AutoJS 脚本在专服/客户端验证对应场景。现有 JUnit/GameTest 已按用户决定删除，替代脚本尚未建立；后续记录脚本与实际结果，不降低 Mixin require。新旧类映射变化需同步更新基线验证脚本及其预期，不能让旧清单与新布局静默脱节。

本轮只调整规划与开发约定文档，没有移动实现、调整算法或重跑构建。历史 [VALIDATION.md](legacy/migration/VALIDATION.md) 记载 398 项 JUnit、43/43 GameTest；它们是前一轮记录，不是本轮新增机制的通过证明。

平台事件顺序参考：[NeoForge 26.1 Living Entities 文档](https://docs.neoforged.net/docs/entities/livingentity/#damage-events)。它支持事件边界判断，具体数值和实体特例以本项目固定版本源码及运行验证为准。


### 2026-11-04 依赖边界落实

common 独立编译已移除 target 源码/资源输入。结算 hurt 栈、事件发布门禁和初始化序列已下沉；target 负责 live Incoming 事件适配、注册及事件总线发布。内置处理器实例/顺序/ID 归 common，假人注册句柄由 target 延迟注入；common 不再引用 target 类。诊断事务的 NeoForge container/附件适配仍保留 target。新增源码与字节码边界检查，并以 AutoJS 正常、边界及拒绝场景验证本轮迁移；详细范围见 SCAFFOLD.md。
