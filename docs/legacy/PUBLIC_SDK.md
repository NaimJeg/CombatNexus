> 历史记录：正文保留对应阶段的实现、命令和结果，不代表当前支持或通过范围。当前入口见 [开发约定](../DEVELOPMENT.md)、[领域定义](../DOMAIN_DEFINITIONS.md) 与 [现行审查](../VANILLA_COMBAT_REPAIR.md)。

# CombatNexus SDK 与内部能力合约

2026-11-06。仅维护当前 API 与数据结构，支持 NeoForge 26.1.2、Java 25。注册在启动阶段完成并冻结，不提供运行时动态订阅。本文优先于 `legacy/` 中的旧接口说明。原版与神化尚缺的底层能力见 [接入评估](PUBLIC_API_INTEGRATION_GAPS.md)。

## 获取服务与边界

在服务器线程使用 `CombatAccess.find(ServerLevel)` 获取已有世界的 `Optional<CombatWorld>`。查询不创建、不 tick、不关闭运行时。世界卸载后旧服务和安装句柄失效；新世界必须重新获取服务。

`CombatWorld` 提供事实、定义目录、伤害、治疗、效果、护盾、恢复和来源服务。`DefinitionView` 是分领域 ID 目录及 generation，不持有内部伤害定义。客户端继续通过展示网络取得服务端投影，不获取 `CombatWorld`。

普通扩展只使用 `api` 包及 NeoForge 的 `combat.platform.api.CombatRegisterEvent` / 伤害注册事件。`combat.internal.CombatRuntime`、内部定义 generation、应用 scope/ticket 和 `spi` 不属于 SDK。护盾 reservation、治疗应用端口和恢复条件端口已移到 SPI。旧 `*Definitions.install(CombatRuntime, ...)` 辅助入口已移入内部包。

## 阶段权限与拒绝

| 环境 | 查询 | 计算工作区 | 业务命令 |
| --- | --- | --- | --- |
| 条件、计算、来源提供者 | 冻结事实 | 仅当前处理器授权范围 | 拒绝 |
| 观察通知 | 只读结果和查询 | 无 | 拒绝 |
| 结算反应 | 已结算事实 | 不可改写结果 | 声明的操作权限与父级预算内 |
| 普通服务器入口 | 允许 | 无 | 校验、准入后应用 |
| 世界/实体清理 | 允许 | 无 | 内部生命周期权限 |

Effect、恢复计划、来源安装/撤销及授盾使用与伤害、治疗一致的阶段规则。状态命令取得内部应用权限后才执行；来源安装中的多个领域写入属于同次来源命令。来源清理可以留下明确的待补偿记录，不把部分写入冒充完整成功。

`CommandRejection` 统一表达重复请求、线程、会话、阶段、目标、预算、过期句柄和未完成应用等原因。领域结果继续保留专用语义。请求 ID 单次消费，拒绝后不能复用同一 ID。`SourceResult` 在补偿未完成时可能同时含拒绝原因和仍有效的清理句柄；应检查 `applied()`，不能仅凭存在句柄判断成功。

治疗区分 `ZERO_INPUT`、`ZERO_EFFECT`、`FULL_HEALTH`。应用后重新采样生命，记录实际恢复、溢出及应用证据；不确定写入不自动重试。恢复条件端口只提供 alive / naturalRegeneration，战斗窗口和最后受伤时刻由内核的实际伤害结算维护。

## 注册、排序与异常

本轮新增 `CombatRegistrar.bridges()`、`facts(FactKey<T>, collector)`、`effectiveAttribute(id, priority, policy)` 和 `restoredSource(provider, owner)`。能力声明在发布前拒绝同一能力/位置的重复替换执行者；`CombatAccess.bridgeCoverage()` 返回不可变声明。这张表目前不授予执行权限，也不表示已经具备逐次调用抑制证据。

外部事实由 codec 在采集时编码，`CombatEntityFacts.external()` 每次解码返回独立值；采集异常记录 ID 到 `incomplete()`，计算不会回读实体补齐。属性 `AttributeValueFrame.Value` 现含 base、raw、effective、effectiveReason；effective 默认经过实体原生读取入口，保留原生属性开关。策略按优先级及 ID 排序，第一个有结果的策略生效；普通异常记录后继续。

`DamageRuleContext.beforeApplication(Step)` 与 `HealingCalculation.beforeApplication(Step)` 收集 `DomainCommand`。处理器异常撤销其追加步骤；计算结束后按顺序提交独立子命令。每步显式选择 CONTINUE / ABORT_PARENT，部分或未知写入无条件停止父计划，已确认写入不回滚。结果统一保存在 `Result.steps()`，每步包含命令类型 ID、完整领域结果、上下文、拒绝和日志；删除治疗专用 `Result.healing()`。子通知继续由服务器 FIFO 排空。

来源恢复完成后，已注册 provider owner 收到 `RestoredSource` 与新的安装句柄。回调处于只读观察作用域，可保存句柄供后续正常命令撤销，回调本身不能写入。查询 `SourceView` 仍不提供句柄。

监听 NeoForge 全局事件总线的 `CombatRegisterEvent`，通过事件的 `CombatRegistrar` 注册行为类型、来源提供者、治疗处理器、观察者和反应。事件返回即关闭注册能力；保留 registrar 不能延长其有效期。重复 ID 或实现类型在启动时拒绝。

同阶段按 priority 降序、完整命名空间 ID 升序执行，不依赖模组加载先后。治疗内置阶段先执行，再执行该阶段注册的扩展处理器；行为数值贡献在相应内置阶段前执行。伤害保留类型化 `DamageModuleRegistrar`，治疗不接受伤害上下文。

普通扩展异常记录诊断，继续后续回调。治疗处理器失败会恢复它修改的数值和贡献记录。周期任务失败消费本次到期，不立即重复执行；来源提供者失败保留已安装来源，`refresh` 返回未完成。致死保护在写入前计算，非法返回值被记录并跳过，不移动到结算后实现。JVM 致命错误不作为普通回调异常吞掉。

## 来源、事实与治疗参与者

`SourceInstance` 区分实例 UUID、提供者 ID、可知 owner 和槽位语义；`SourcePlan.definition` 表示定义身份；`InstallationHandle` 表示当前世界会话的一次安装。两个槽位或两次施法必须有不同实例 UUID。撤销要求原安装句柄，旧世界、恢复前或伪造句柄无效。

一个 `SourcePlan` 可以同时提供多个 Effect、恢复计划及护盾。属性和行为随 Effect 安装。撤销会取消周期/恢复任务并撤销属性；已授护盾依 `REVOKE` 或 `KEEP_UNTIL_EXPIRY` 策略处理。外部提供者按目标冻结输入输出完整 offer 集合；相同 offer 不重复安装，卸下只撤销相应实例。公开 `refresh` 可主动触发，世界 tick 负责已跟踪目标的后续协调。

`EffectView` 合并原版药效与受管效果，包含定义、来源/实例、所有权、语义标签、等级、剩余时间、可知施加者、完整性及允许操作。原版未提供的 owner 保持未知。统一查询不意味着接管原版属性或计时；驱散交还实际 owner，拒绝时返回原因。Effect 定义 JSON 可使用 `tags`，省略为空数组；类别同时提供 `combatnexus:beneficial` / `harmful` / `neutral` 语义。

治疗请求明确 healer，可同时捕获 TARGET、HEALER、EFFECT_OWNER 角色，同一实体只捕获一次。未知 healer 不借用目标作为来源。`healing_power`（默认 1）在贡献之后的 SOURCE 阶段生效，随后受治疗倍率、限制阶段各自记录事实。规则/扩展从 `HealingCalculation.participants()` 读取冻结输入，不重新读取实体属性。

## 四种行为原语与存储

| 原语 | 时机与能力 |
| --- | --- |
| `NumericalBehavior` | 伤害/治疗各自的类型化计算阶段 |
| `SettlementBehavior` | 结算后，按声明权限提交领域命令 |
| `PeriodicBehavior` | 共享到期队列，短期 `PeriodicExecution` 提交治疗或伤害 |
| `ProtectionBehavior` | 写入前保护，按安装和类型记录使用次数 |

五种现有玩法通过上述原语实现。新增类型注册 `BehaviorType<T>(id, implementation, MapCodec<T>)`；codec 和构造器完成参数编译/校验。组件应是不可变参数值，不能在实例对象中保存全局或实体运行状态。一个计划内同类型只能出现一次，需要多来源时用不同来源安装。

周期任务、保护使用次数、触发计数和拥有的护盾状态由共享执行器按安装/类型保存。自定义周期伤害不新增计时器、保存分派或核心玩法分支。周期 capability 只在当前调用内有效，撤销后不能继续提交。

只维护当前数据结构。实体存储不带格式版本，行为直接保存命名空间类型 ID 与 codec 参数，不带版本包装，不使用 Java 简单类名。没有旧格式读取、迁移或降级路径。definition generation 仅用于运行时定义发布和在途快照隔离。加载失败保留原始数据并停止覆盖保存。需显式持久化的来源使用 `SourcePlan.persistent`，恢复后创建新安装句柄；死亡/换维度按既有实体生命周期处理。

## 原版语义出口与覆盖

适配按观察、路由、替换区分所有权，不以 Mixin 数量作为覆盖指标。本轮保留既有原版执行边界，并以统一事实和请求接入；未接管的攻击后效果继续归原版。

| 路径 | 所有权与采样位置 | 原版抑制/重建 | 取消与未知情况 | 运行依据 |
| --- | --- | --- | --- | --- |
| 原版药效状态、属性 | 原版持有实例和 modifier；`EffectFacts` 捕获只读副本 | 不抑制，不重装属性/计时任务 | owner 未知明确标记；驱散交回原版 | SDK 原版/受管组合、native-effect-failures |
| 恢复药效、自然恢复及已接线实体恢复 | 原版保留触发时机；调用点来源 scope，`heal` HEAD 路由标准治疗请求 | 取消原 heal 方法体，应用端发事件并写入一次 | 原版事件取消保留已提交子操作；未知调用者不伪造 owner | native-healing、native-healing-failure、natural-world-tick |
| 附魔数值 | `EnchantmentHelper.modifyDamage` RETURN 捕获输入/输出及来源物品 | 只记录可确认的总贡献，具体消费遵守伤害归属政策 | 无捕获或无法拆分时保留不透明总量，不编造附魔分项 | native-damage-semantics、support-policy |
| 原版伤害、护甲/保护/抗性 | 已有攻击捕获、领域计算、原生应用及结算证据 | 仅受管归属项抑制并重建；合作模式保留原版贡献 | 原生取消不重试；未知 damage type 不冒充已支持原版路径 | native-damage-semantics、preapplication-decisions |
| 击退、耐久、攻击后副作用 | 未接管部分保留原版执行位置 | 不因记录伤害结果而重复执行副作用 | 不承诺伤害取消等价于撤销整次攻击 | 历史覆盖矩阵及本轮未扩大接管范围 |

详细的原版伤害分类与期望值仍由 [覆盖矩阵](legacy/native-damage-semantics.json) 驱动 AutoJS；其他原版路径的后续扫描见 [接入评估](VANILLA_INTEGRATION_ASSESSMENT.md)。当前隔离验收没有证明依赖原 `heal` 方法体或返回注入点的第三方模组组合兼容。

## 示例与验收

[SDK 示例](../examples/sdk-extension/src/main/java/example/combatnexus/SdkExample.java)是独立模组 source set，只导入公共 API。它注册外部饰品来源、周期伤害类型、治疗处理器及受限反应。目标 `check` 编译示例，`sdkExampleJar` 单独打包，`-PsdkExample=true` 仅为运行验收启用，不混入主制品。

运行入口：

```powershell
# 在 targets/neoforge-26.1.2
.\gradlew.bat clean build
.\gradlew.bat sdkExampleJar
# 在仓库根目录
.\gradlew.bat -p common clean build
python scripts/verify-common-boundary.py
python scripts/verify-baseline.py
python scripts/verify-capabilities-runtime.py --suite all --start --reload --restart --port 25594 --game-port 25569
```

`verify-common-boundary.py` 检查公开字节码签名不暴露内部/core/SPI 类型，并检查示例没有导入实现包；方法体调用内部实现不构成 ABI 泄漏。AutoJS 的 `verify-sdk.js` 验证外部饰品跨域贡献/独立撤销、多施法者周期伤害、统一原版/受管效果查询、双参与者治疗、观察权限及存储恢复。其余领域脚本继续覆盖正常、边界、失败与原版接入；不恢复 JUnit/GameTest。

本次实际运行结果见下方验收记录；编译结果不替代运行结果，隔离环境也不代表其他战斗模组的组合兼容已通过。

### 实际验收记录（2026-11-06）

环境：Windows、Java 25、NeoForge 26.1.2.126，AutoJS 1.0.1 / Rhino 2101.2.8-build.91。使用独立 `build/acceptance-isolated` 世界、服务端 AutoJS 25594 / 游戏端口 25569、客户端 AutoJS 25595。隔离配置排除 Apotheosis 及其前置，启用 SDK 示例。

| 验证 | 实际结果 |
| --- | --- |
| 活动 target `clean build sdkExampleJar` | 通过；主制品和独立示例制品生成 |
| common 独立 `clean build` | 通过；不生成发布 jar |
| `verify-common-boundary.py` | 925 个 common 编译类；公开签名、示例依赖、平台反向依赖及生命周期边界通过 |
| `verify-baseline.py` | 720 个活动源类及元数据、Mixin、资源、许可证通过 |
| `verify-capabilities-runtime.py --suite all --start --reload --restart` | 29 组，1,396 项通过，0 失败；其中 SDK 43 项、定义重载 10 项、真实重启恢复 4 项 |
| `verify-architecture-client.py --port 25595 --game-port 25569` | 3 项通过：连接、C 键、实际服务端快照显示 |
| `verify-presentation-runtime.py --server-port 25594 --client-port 25595` | 5 项通过：预算拒绝与恢复、过期响应隔离、换维度、死亡重生 |
| AutoJS/Python 语法、`git diff --check` | 通过 |

原始结果位于活动 target 的 `build/verification/isolated/`：`summary.json`、各场景 JSON、`reload.log`、`persistence-restart.json`、`architecture-client.log`、`presentation-runtime.log`。这些生成文件不入库；本记录保存可复跑命令和实际范围。未进行第三方战斗模组联合兼容验收，未启用 1.21.1 target。

后续移除存储版本机制后，target/common 干净构建、边界/制品/语法检查和服务端 1,396 项检查重新通过。客户端 8 项为上一轮实际记录，本轮未修改网络和展示、未重新运行；clean 后旧客户端日志不再保留。两处跨 tick 验收夹具改到地形上方，详见接入评估的验证记录。

## 语义事务与桥接补充（2026-11-06）

当前桥接 API、六类神化范围、命令与状态政策以 [桥接实施记录](BRIDGE_IMPLEMENTATION.md) 为准，取代上文历史记录中的未实施说明。

- `CombatRegistrar` 收集 facts、effectiveAttribute、damageFormula、sourceDescriptions、runtimeState、observeMoment、observeDefinitions、restoredSource 和桥接归属，启动后冻结。
- `DamageRuleContext.beforeApplication` 与 `HealingCalculation.beforeApplication` 只收集类型化步骤；回调失败恢复计划。执行结果保留拒绝、父子身份及部分写入证据。每步必须显式选择 CONTINUE / ABORT_PARENT。
- `CombatWorld` 提供 effects.nativeEffect、absorption、health、projectiles、entities、cooldowns、states、scheduler；全部实际写入由核心准入，不向扩展开放 ticket。`currentAction`、facts、sourceDescriptions、definitions 和 bridges.history 都不授予写权限。
- 发射快照使用防御复制的武器与冻结射手事实；随机采样在公式回调内有效，shared 必须引用仍有效的动作身份。
- `CommandResult.context` 与 `HealingSettlement.context` 保存操作身份；`HealingSettlement.beforeApplication` 和伤害 evidence 保留子步骤。图腾、吸收、生命、效果等通过 ApplicationJournal 区分精确写入、整次调用观察和未知写入。
- `StateKey` 定义作用域、codec、持久化和显式清理边界；`ScheduledIntent` 为可序列化领域意图，不接受 Runnable。恢复回调重新交付 owner 的安装句柄，普通查询不拥有撤销权。

上述为当前 API 变更，不提供旧存储格式或跨版本兼容门面。实际运行证据以桥接实施记录的最终制品验收表为准。


## 内部语义通用化（2026-11-06）

本节为当前合约，替代上文对应的旧接口形状。直接更新当前 API 和存储，不提供旧接口、旧意图类型名或旧存档的兼容读取。

### 命令与意图

- `DomainCommand` 独立于应用前计划，适配伤害、治疗、原版/受管效果、吸收、生命修正、致死、冷却、实体、投射物、授盾、恢复计划、来源安装/撤销和注册意图。领域服务仍拥有实际准入与写入。
- `CommandOutcome<T>` 分开表达 applied 和 completed，保留领域载荷；`StepEvidence` 带命令类型 ID。`IntentReceipt` 保留全部子结果及意图自身回执，失败不会丢弃已完成的子写入。
- `CombatRegistrar.intent(IntentType<T>)` 注册命名空间 ID、实现类型、MapCodec、操作权限和执行器。重复 ID/实现类型拒绝，启动后冻结。新增组合意图不修改核心 switch；新增底层写能力仍需领域服务和核心适配。
- `ScheduledIntent` 为开放的不可变参数接口。存储的 type 为注册命名空间 ID；未知类型或非法参数拒绝。内置 healing/effect/ignition/absorption/damage/shield 均通过注册表执行。参数不得保存实体、回调、Runnable 或授权。
- `IntentExecution` 提供目标、来源、当前 tick、领域命令提交及伤害便捷入口，仅当前调用有效。INTENT 是独立准入操作，子命令共享父链与预算，权限与父级取交集；嵌套意图不能扩大权限。
- 周期行为使用 `PeriodicExecution.execute(intent)`（原 heal/damage 便捷方法直接委托同一领域执行器，保留既有后继反应权限），延迟任务使用 `ScheduledTask.intent`，应用前计划使用 `DomainCommand.Intent`，共用执行器。两种调度保留各自生命周期和剩余时间存储。完整拒绝由意图实现决定后续步骤；部分/未知写入立即停止。异常保留子证据，不自动重放。

### 结算与保护

- `SettlementBehavior` 声明 subscriptions、permissions，通过 `settle(SettlementContext)` 接收 `SettlementEnvelope`。通过 `result(Class<T>)` 读取领域载荷；伤害、治疗、护盾消费及状态命令共用分派。状态命令载荷为 `CommandResult<?>`。
- `SettlementPolicy` 是注册的世界级策略，与来源行为共用分派，无虚构的 Effect 安装。属性吸血为内置策略，从 `ReactionInputs` 的冻结攻击者属性读取比例；应用计划与证据删除 lifestealRatio。资格和收益上限保持原政策。
- 来源集合在应用前捕获，派发时核对安装仍有效；新来源不响应自身安装，已撤销来源不响应自身撤销。重复分派按操作身份及领域隔离，伤害内的护盾消费保留 SHIELD 结算。`ShieldSettlement.brokenSources` 标识本次耗尽的来源池。
- 观察回调只读，行为权限受父级限制，通知沿用 FIFO。世界清理不产生玩法反应。恢复受伤时间保留为内核记账及诊断事实，不再包装为玩法策略。
- `ProtectionBehavior.protect(ProtectionContext)` 返回 OptionalDouble：空值放行，有值表示拟保护生命。上下文包含 before/proposed、操作类型、来源、行为来源及冻结的伤害分类/参与者。致死请求仅填充已知信息。首个有效决定生效，仅提高拟写生命才耗次数；不引入资源支付保护。

独立示例 `SemanticExample` 注册组合意图、周期组合、火焰条件保护、护盾耗尽治疗与状态结算行为。`verify-semantic-extension.js` 验证三种入口、结果、权限收窄、预算、过期能力、失败证据和来源生命周期。真实重启验收使用外部意图 codec，验证参数恢复。

验收命令：活动 target `gradlew clean build sdkExampleJar`；common 独立 `clean build`；边界及基线检查；`python scripts/verify-capabilities-runtime.py --suite all --start --reload --restart --port 25594 --game-port 25569`；兼容组合使用 `--suite bridges --compatibility --packaged --start`。实际结果见本节末记录。


### 本轮实际验收

环境：Windows，NeoForge 26.1.2.126 / Java 25 工具链，AutoJS 1.0.1。隔离服务端使用 AutoJS 25594 / 游戏 25569；最终制品兼容服务端使用 25596 / 25571。未修改客户端网络或界面，本轮未重复客户端验收。

| 检查 | 实际结果 |
| --- | --- |
| 活动 target `clean build sdkExampleJar` | 通过，核心、内嵌 compat 与独立 SDK 示例制品生成 |
| common `clean build` | 通过，无发布 jar |
| `verify-common-boundary.py` | 通过，1,075 个 common 编译类，公开签名及平台/生命周期边界通过 |
| `verify-baseline.py` | 通过，810 个活动源类及制品、Mixin、资源、许可证通过 |
| 隔离 `--suite all --start --reload --restart --port 25594 --game-port 25569` | 33 条场景记录，1,508 项通过，0 失败；其中语义扩展 46 项、定义重载 10 项、真实重启 10 项。神化场景在隔离配置中明确跳过 |
| 最终制品 `--suite bridges --compatibility --packaged --start --port 25596 --game-port 25571` | 应用前计划 30 项、原生命令 29 项、六类神化场景 49 项、自然 tick 23 项，共 131 项通过，0 失败 |
| AutoJS/Python 语法、`git diff --check` | 通过 |

新验收额外覆盖周期治疗继续触发溢出盾、应用前子通知按完成发布顺序进入 FIFO、声明不足的嵌套意图、保留失败组合的子写入证据，以及外部意图参数真实重启恢复。组合回执使用 `IntentReceipt`，其自身失败与子步骤证据分别保留；示例历史只保存数值和 ID 投影，不保留完整结算对象。

原始结果位于活动 target 的 `build/verification/isolated/` 与 `build/verification/compatibility-packaged/`，含 summary.json、各场景结果及服务器日志，不入库。兼容结果仅证明上述脚本覆盖的组合路径，不扩大目标版本或全部第三方模组支持声明。

## 原版接入事实更新（2026-11-06）

- `DamageSettlementSnapshot.successfulHit()`：完整且未取消的 APPLIED 结算，生命、自定义盾、原版吸收至少有一项消耗。吸血仍另行要求主伤害确认生命损失。
- `DamageRequestKind.SWEEP`：原生横扫派生请求，可闪避、禁止 proc，独立目标结算共享动作与攻击者输入。新增 codec 值 `sweep`，扩展方穷尽 switch 需要处理此值。
- `CombatMoment.Hit` 增加 `shieldLost` 与可选 `action`；零资源消耗不发布成功 Hit。
- `CombatMoment.Death` 增加可选 `directEntity` 和 `operation`；保护成功不发布死亡。
- `CombatMoment.Effect` 改为 `before/after`（可选 duration/amplifier 状态）、`EffectChange` 和可选 `source`；不再使用 present。来源缺失不猜测，普通倒计时不当作刷新。
- 新增 `CombatMoment.Protection`：目标、可选操作身份、伤害类型、保护结果及独立应用日志；设血不是治疗。新增 `PartHit`：龙部位、主体、伤害类型与接受结果；接受结果不等价于确认死亡。

这些是当前 API 的破坏性变更，不提供旧记录构造器或旧字段别名。事件观察仍走有界完成队列，事实不签发写权限。详见 [行为与验收](VANILLA_INTEGRATION_ASSESSMENT.md#全项接入实施2026-11-06)。显示界面与网络本轮不变。
