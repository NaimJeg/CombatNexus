# 全模块分类与支持政策

2026-11-05，P0–P2 重构记录。本文描述当前源码支持范围。领域中立来源、类型化行为组合、四个领域数据目录和装备多绑定已落地，格式与迁移见 [领域定义](DOMAIN_DEFINITIONS.md)。2026-11-06 的能力补齐及独立环境实测见 [能力验收](CAPABILITY_COMPLETION.md)。

## 如何声明支持

每项能力分别说明执行归属、接入入口、可配置范围、实现状态、生命周期和验证证据。“注册成功”“公式存在”“有脚本”“构建成功”都不等于运行验收。

- 原版执行：模组可以观察和展示，但不重复计算同一收益。
- 模组执行：有明确消费阶段和真实资源应用入口。
- 仅观察：只记录事实，不安装贡献。
- 预留：注册身份存在，框架暂无消费逻辑；第三方消费不代表本框架实现。
- 未实现：数据声明必须拒绝，不能加载成功后静默无效。
- 验证按具体脚本、环境和结果记录，不把历史断言数沿用到当前工作树。

## 分类边界

| 概念 | 当前归属及约束 |
| --- | --- |
| 操作与因果 | combat 统一伤害、治疗、护盾等操作的准入、根链与预算；解释 ID 不恢复写权限 |
| 来源 | CombatSource 保留 ID、可选 owner、领域中立 parentOperation；引用不携带权限 |
| 伤害类型 | 原生注册身份及 bypass 标签，决定原生语义和纳管边界 |
| 通道 | 单个伤害分量所属通道；原生初始通道按优先级降序、ID 升序首命中，未命中 untyped |
| 攻击类别 | 近战、投射物、爆炸、环境及武器动作可重叠，不等于通道；规则如何消费类别由具体执行器决定 |
| 治疗原因 | NATURAL_REGENERATION / EFFECT / ITEM / LIFESTEAL / SKILL / UNKNOWN_EXTERNAL；具体来源由 source ID 表达 |
| 资源 | 生命、原版吸收、自定义护盾池分开；盾牌格挡也不是自定义护盾池 |
| Effect | VANILLA_OBSERVED 与 MANAGED 区分执行归属；未提供完整增益/减益、驱散分类体系 |
| 反应与周期 | 结算反应使用确认事实；周期任务负责触发，healing 拥有治疗计算与写入 |

## 模块支持矩阵

下表描述当前源码边界，运行覆盖以 [能力验收](CAPABILITY_COMPLETION.md) 的具体脚本为准。

| 模块/能力 | 执行者及入口 | 数据支持 | 生命周期/限制 | 当前状态与实现证据 |
| --- | --- | --- | --- | --- |
| attribute 数值与来源 | 服务端一次采样；原生 modifier 或受管贡献 | 属性注册、默认值和范围由代码固定；装备 modifier 可供原生系统消费 | 同次值与来源快照，世界会话修订；不按 tick 偷复用战斗输入 | 已实现：[属性目录](../common/src/main/java/io/github/naimjeg/combatnexus/attribute/api/AttributeCatalog.java)、AttributeSnapshotService |
| attribute 单位与预留 | 服务端目录标注，客户端只格式化 | 内置属性单位显式绑定；未知属性不猜单位或消费状态 | 概率、加法比例、转化比例和倍率分开；闪避、易伤及穿甲已有消费者 | 目录与实际客户端面板已验证 |
| formula | 确定性纯函数；无实体访问、事件或随机所有权 | 护甲/抗性 K、保护评分系数可配置；公式结构由代码固定 | 参数随请求定义快照；不支持任意表达式或脚本 | Armor/Resistance/Healing/Multiplier/Penetration/InnateResistance 均有业务调用 |
| damage 原生入口 | Incoming/Pre/Post 与 hurt/生命写入 Mixin | 类型标签、通道、规则、条目/词缀及模板 | 原生捕获匹配后重建；未捕获的传入数值保持不透明；bypass 不纳管 | 已实现，专项接入运行已验证：[原版接入](NATIVE_COMBAT_INTEGRATION.md) |
| damage 减伤 | 每种原生机制唯一归属，见下表 | 三种模式及四个开关 | 仍遵循原生 bypass；独立通道抗性继续计算 | P0 修复协作/混合模式重复减伤；默认模式策略未变 |
| healing | 独立服务及服务端 LivingEntity.heal 拦截 | 接收倍率属性、类型化增量、combatnexus_healing_rules 的阶段/条件/增量/倍率 | 非有限/非法原生输入拒绝；不复活；事件后采样和单次生命写入；直接 setHealth 不算治疗 | 已实现，来源/嵌套/失败运行已验证；DefaultHealingService、NativeHealingAdapter |
| shield | ShieldPoolService；伤害 Pre 后、原版吸收和生命前提交 | combatnexus_shields；ShieldDefinition / ShieldGrant / ShieldPoolSnapshot 分离 | 类型/标签与通道筛选共同匹配；最终总量按分量比例缩放；来源替换/刷新、过期、恢复、清理 | 已实现；直接分摊及原生伤害通道接线有运行断言 |
| regeneration | 自定义 HP/s 调度调用 healing | combatnexus_regeneration 的速率、受伤延迟、脱战、游戏规则条件和期限 | 20 合格 tick 聚合；100 tick 战斗窗口；跳 tick、取消、满血不补发；不接管原版计时 | 已实现；DefaultRegenerationScheduler；有期限安装计划 |
| effect 原版观察 | 原版拥有属性/效果计时；EffectFacts 观察 | 原生 Effect 定义及事实，不再安装第二份 modifier | 原版再生在原生计时点调用受管 heal；观察不能授予受管行为 | 已实现；EffectInstance.Ownership、VanillaEffectContributions |
| effect 受管 | 来源化安装、回执、补偿、撤销；属性端口写原生 modifier | combatnexus_effects 的属性贡献、行为引用、类别与驱散政策 | REPLACE / REFRESH_KEEP_STRONGER；部分安装不激活；死亡清理；显式持久来源及装备账本跨退出/卸载恢复 | 已实现；DefaultEffectService、EffectDefinitions；八类动作限制已接入 |
| effect 控制 | 原生输入/服务器包/生物入口及 CombatActionGate | restrictions 八类枚举、来源并集 | 原版施法、投射物与技能传送接线；外部动作须调用显式 gate | 服务端及真实客户端验证，入口列表见能力验收 |
| combat 内核 | 服务器准入、完成 FIFO、资源 journal、世界 Runtime | 请求预算可配置，硬上限/权限由代码固定 | 计算阶段禁写；部分/未知写入不回滚、不重试；查询不推进 tick | 已实现；CombatServerRuntime、CombatRuntime；不是 ACID 或持久 exactly-once |
| combat 行为/反应 | 来源协调器连接领域服务，按真实结果触发 | 类型化 components：保护、反伤、转盾、周期治疗、治疗增量 | 定义惰性；显式 API/装备多绑定组件安装；SOURCE/ACTION 等限额不授予事务权限 | 已实现；BehaviorPlan、BehaviorEffects；五种固定执行器可组合，同类型不可重复 |
| combat 吸血 | 确认的主伤害生命损失派生治疗 | lifesteal 转化比例属性 | 排除吸收/护盾、派生和反伤；治疗倍率只消费一次 | 已实现；CombatReactions。比例不是触发概率 |
| presentation | 服务端快照投影；本人校验与限频；客户端 C 键面板 | 本地 Tooltip 等级、语言资源；无战斗权威公式 | nonce/session/维度/generation 拒绝旧回复；断线清理；64 KiB 最终上限及构造预算 | 已实现；P0 新增单位/预留标记与附魔归属同步，客户端专项运行已验证 |
| config | 服务端定义与本地运维分离 | settings JSON / operations.json / client.json | 设置整文件覆盖；本地配置重启生效；不读旧 TOML | 已实现；DamageServerSettings、SettingsJson |
| target | NeoForge 注册、原生事实与写入、Mixin、网络和客户端接线 | 只消费已批准定义，不拥有通用业务规则 | common 使用共享源码编译，仍依赖 Minecraft 类型；不承诺平台无关 | 只维护 26.1.2，见平台政策 |

## 属性消费语义

| 属性族 | 单位/消费位置 |
| --- | --- |
| crit_chance | PROBABILITY，暴击决策 |
| crit_damage_additive | ADDITIVE_RATIO，暴击额外增量 |
| 元素、kinetic、melee、projectile 的 damage_additive | ADDITIVE_RATIO，伤害类型/类别缩放；没有额外注册同义 physical_damage_additive |
| resistance_* | RATING，通道目录与伤害类别选择的抗性计算；负值表示易伤 |
| thorns | HEALTH_POINTS，原生荆棘伤害基础值增量；不是通用比例反伤 |
| healing_received | MULTIPLIER，独立治疗管线；已消费，不再保留错误的“预留”注释 |
| lifesteal | CONVERSION_RATIO，确认主伤害生命损失到治疗的转化比例 |
| dodge_chance / vulnerable_damage_additive | 概率/目标伤害加法比例；主攻击前闪避，易伤进入公共倍率桶 |
| armor_penetration_ratio / armor_penetration_flat | 比例/评级；移除 max(护甲×比例, 固定值)，有效护甲最低零 |
| 原版攻速/移速/护甲/韧性/生命上限 | 复用原版 holder；移速保留原版单位，不冒称格/秒 |

易伤默认值由 0.20 改为 0，新增两种穿甲属性默认 0；单位集中在 AttributeCatalog.Definition，AttributeUnits 不再根据属性名后缀推断内置单位。未知第三方属性不被宣称为“预留”或“已接入”。

## 减伤执行归属

`VanillaCompatibilitySettings.executor(Reduction)` 是模组桥接处理器与原生抑制端的共同决策来源。

| 机制 | FULL_REPLACEMENT | COOPERATIVE | CONFIGURABLE 开关 true / false |
| --- | --- | --- | --- |
| 护甲/韧性及原生护甲有效倍率桥接 | 模组计算，抑制原生护甲减伤 | 原版计算，模组不重算 | 模组 / 原版 |
| 保护附魔 | 模组转换临时抗性，抑制原生附魔减伤 | 原版计算，模组不转换 | 模组 / 原版 |
| 抗性提升 Effect | 模组计算，抑制原生 Effect 减伤 | 原版计算，模组不重算 | 模组 / 原版 |
| 固有减伤 INNATE_RESISTANCE | 在原生阶段等价计算女巫自身免疫及 85% 特定类型减伤 | 原版计算 | 等价替换 / 原版 |

true/false 指对应 `suppress_*` 字段。FULL_REPLACEMENT/COOPERATIVE 仍忽略这些原始布尔值，以模式为准。字段、默认值和模式名称不变；COOPERATIVE 及保留原生机制的 CONFIGURABLE 不再叠加同一机制的模组计算，是明确的数值修复。

自定义通道抗性、规则贡献仍独立生效，COOPERATIVE 不等于恢复纯原版伤害。模组的 `multiply_armor_effectiveness` 只修改模组护甲计算，原生拥有护甲时不承诺影响原版护甲。保留原生减伤会在 Incoming 之后处理传入总量，不能承诺维持模组每个分量的通道/真实伤害绕过规则。额外模组事件回调仍可能改变结果，需要联合运行验证。

物品提示使用服务端同步的 managedArmor / managedEnchantments：模组不拥有保护附魔时不显示模组抗性换算；不拥有护甲或尚未同步时不生成模组 breach 解释。原版附魔文本由原版保留。

## 重载、数据与平台政策

settings、channels、damage rules、entry/affix templates、behaviors、healing rules、regeneration、shields、effects 作为完整定义代际采用；失败保留旧代际，首次失败不回退其他服务器。原版标签遵循原版合并规则，自定义同路径定义遵循资源覆盖。领域定义严格键集合、范围/引用/容量校验；统一 JSON 读取拒绝重复键、超深和超预算结构；damage 结构与注册条件/操作的声明键也受检查。物品存储 codec 和扩展内部负载的边界见领域定义文档。

在途请求固定旧快照，新请求采用新代际。受管行为保存安装值；装备来源的定义变化由装备协调器处理，不把热重载等同于所有持续资源自动重置。默认来源仍为会话状态；显式 PERSISTENT 来源及装备账本采用版本 1 实体存储，离线暂停剩余时长，死亡清除。详见能力验收。

仅维护 NeoForge 26.1.2 / Java 25；1.21.1 禁用骨架不得发布；Fabric/Forge 不在范围内。神化等 runtimeOnly 依赖解析成功不代表联合兼容通过。注册 ID 未变不等于旧存档兼容；不增加旧字段别名、包门面或独立协议版本。

Java API 变化：AttributeCatalog.Definition 新增必填 unit；FormulaSettingsPayload 新增两个执行归属布尔值并更新 codec，客户端与服务端应使用同一构建；未增加版本握手。面板 JSON 增加已知模组属性的 reserved 标记及 Effect 政策。其余破坏性 API/数据格式变化见领域定义迁移章节；无旧格式别名。

## 验证

当前独立环境验证、性能测量方法和实际结果见 [能力验收](CAPABILITY_COMPLETION.md)。历史 P0–P2 仅编译的记录不再代表当前工作树。其他 MOD 联合兼容按用户决定暂不验收。

```text
python scripts/verify-capabilities-runtime.py --start --restart
python scripts/verify-capabilities-runtime.py --syntax-only
```

CI 执行独立服务端回归及真实重启，并归档脚本结果、工作树摘要与日志。客户端使用真实窗口和本地 AutoJS 进行联调，未把服务端 CI 当作客户端覆盖。`test NO-SOURCE` 不计行为覆盖。
