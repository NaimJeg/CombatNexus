# 原版战斗修复增量（2026-11-07）

本记录对应本轮实际修改，不代表原版战斗接入计划全部完成。仅维护 NeoForge 26.1.2 / Java 25；不声明旧存档或跨版本网络兼容。当前源码审查确认的剩余工作见文末“源码审查与剩余缺口”；前文按阶段保留，不再从阶段待办推断当前状态。

## 实施状态

| 项目 | 执行 | 来源 / 公共 API | 说明 | 当前证据 |
| --- | --- | --- | --- | --- |
| 狼铠 | 原版耐久、破损分支纳入正式结算；优先于框架护盾 | 新增 `EquipmentDamageReceipt`，独立记录 BODY、物品 ID、承接量、实际耐久消耗与破损；`DamageApplicationEvidence` / `CombatMoment.Hit` 携带不可变回执列表 | 启动注册的装备组件描述器 | AutoJS 真实 Wolf.hurtServer，详见下节 |
| 守卫者 / 远古守卫者 | 原生提前反伤取消，成功结算后公共策略提交一次 REFLECTED 子伤害 | 静止状态由注册事实冻结；读取冻结伤害标签与原始直接攻击者；使用回调父权限及既有预算 | 不改原版生物提示 | 实际受伤入口、全盾、取消、闪避、移动、递归与排除标签 |
| 托管 Effect 事件 | 复核发现提交端原已发布事件，不能记为新增修复；继续只在提交成功后发布 | `CombatMoment.Effect.managedChange()` 新增完整不可变前后实例及移除原因；原版非托管观察为空 | 无展示改动 | 添加、刷新、升级、清除事件唯一性与原始来源 |
| 物品提示 | 继续直接读取当前 ItemStack；保留已有工作区的本地读取改造 | `CombatRegistrar.itemDescriptor` 注册类型化 DataComponent 描述器，通用投影负责合并文档 | 火焰附加、火矢、荆棘、节肢杀手、引雷、风爆、突进、激流、忠诚、多重射击、穿透、快速装填、无限；混合效果只描述战斗部分；狼铠、格挡、死亡保护 | 真实客户端双语、无槽位本地投影及失败保留原行 |
| 药水提示 | 不执行效果或最终公式 | 公开 `EffectTooltip.capability(id, amplifier)` 复用启动注册能力描述 | 按等级描述中毒、凋零、再生、瞬间治疗与伤害的基础量、周期、生命底线；明确喷溅衰减及最终结算由服务端负责 | 本地携带效果及客户端回归 |
| 数值与未知定义 | 修复原版 multiply.factor 误读 | 不创建物品查询网络 | 未识别的数值操作或条件使本次追加失败；未知无关附魔组件跳过；缺失定义不追加占位说明 | 客户端构造真实 MultiplyValue / 随机条件附魔，验证原版及其他模组行保留 |

反伤运行还复现了派生伤害不允许子操作时，恢复延迟记账被误拒绝的问题。`CombatRuntime` 已验证当前结算回调与世界；内部 `recordDamage` 不再把这种框架记账当作新的子玩法操作。对外恢复服务仍不暴露此控制接口。

## Java API 变化

这是当前 API 的变更，没有旧接口兼容门面。构造 `DamageApplicationEvidence` / `CombatMoment.Hit` 的内部调用已增加装备回执列表；`CombatMoment.Effect` 增加 `Optional<EffectService.Change>`。托管变化保留 source 叠加身份、展示 owner、隐藏层和移除原因，不再只能从 duration/amplifier 摘要猜测。

平台在启动期通过 `registrar.equipmentDamage()` 获取 `EquipmentDamageAccess`。`apply(target, source, nativeCall)` 只包装当前原生装备承伤；内部创建回执接收器并绑定调用线程、当前 hurt scope、世界与应用阶段，回调退出后失效。它不开放 Runtime、可构造的应用许可，也不负责创建世界会话。没有框架结算时执行原生调用但不制造权威回执。独立扩展示例位于 `scripts/autojs/effect-extension`，只引用公共 API，并检查接收器过期拒绝。

类型化物品描述器示例：

```java
registrar.itemDescriptor(id, DataComponents.BLOCKS_ATTACKS,
    (stack, blocking) -> List.of(new TooltipBlock(stack.getHoverName(),
        List.of(Component.translatable("example.blocking")), List.of())));
```

注册期结束后禁止追加注册。描述器读取副本；隐藏组件不调用描述器。生成完整文档副本后才更新提示，异常保留原提示。原版业务文本留在 target 的描述适配器中。

## 验收边界

本轮接入范围是活动 target 的原版战斗效果与相关公共合约。明确绕过框架或没有注册 ID 的伤害源保留原生所有权，包括原生守卫者反伤时机；这些来源没有框架父事务，不伪造派生结算。未注册来源的实测发现环境入口提前读取注册 ID 会抛异常，已修复并加入真实 hurt 回归。

世界、持久化、Effect 投影、治疗、生命/吸收生命观察、投射物事件、重生与换维度通知已迁移至公开平台合约。旧通用状态存储和投射物存储 Mixin 未在本轮改动，不将其计入 50 文件边界检查。源码边界脚本明确列出检查集合。

展示验证使用真实客户端事件和本地 ItemStack，覆盖 16 种补充说明的首级/最高级、普通/Shift、双语及现有数值条件解析；不宣称穷举任意数据包条件，也不宣称做过人工 JEI 鼠标悬停验收。JEI、创造目录、箱子和背包共用同一无需槽位或回包的物品描述路径。未知条件、缺失定义和描述异常保留原提示。

此前段落和计数均为相应阶段记录，最新结果以文末为准；不扩大到其他 target、非战斗能力或第三方模组全面兼容。

## 运行与构建

环境：Windows、Java 25.0.3、Minecraft 26.1.2、NeoForge 26.1.2.126、AutoJS 1.0.1、Rhino 2101.2.8-build.91；客户端同时加载仓库既有 JEI。没有恢复 JUnit/GameTest，也没有降低 Mixin require。

运行命令：

```text
python scripts/verify-capabilities-runtime.py --suite vanilla-integration --start --port 25595 --game-port 25570
python scripts/verify-capabilities-runtime.py --suite effect-foundation --start --restart --port 25595 --game-port 25570
python scripts/verify-carried-effect-tooltips.py
python scripts/verify-vanilla-combat-boundary.py
```

旧 vanilla-integration 脚本引用了已移除的 SDK 示例，现改用独立临时 Effect 扩展示例。同步更正两个陈旧断言：图腾保护后再生的首次脉冲，及实体 tick 不推进世界统一 Effect 时钟。客户端旧“服务端物品实例预算拒绝”断言改为缺失本地定义的保留原提示验证，不再把它宣称为网络预算覆盖。环境展示场景为悬空仙人掌补齐沙子与实心支撑，并将实体放在方块中央。

## 实际结果

- 活动 target 目录执行 `gradlew clean build` 通过；`common` 独立 `clean build` 通过。
- `verify-common-boundary.py`、`verify-baseline.py`、`verify-vanilla-combat-boundary.py`、AutoJS/Python 语法及 `git diff --check` 通过。发布包包含 861 个活动源码类；临时扩展示例不进入发布包。
- 战斗套件 92 项及自然 tick 23 项，共 115 项通过。包含普通/破损/绕过/不可损坏/与护盾共存/取消/闪避狼铠承伤、零生命损失不吸血，守卫者及远古守卫者的静止/移动/全盾/取消/闪避/排除标签/递归，以及公共接收器有效期。
- 客户端共 357 项通过：携带效果 199，中英文物品各 45，活动 Effect 63，面板 5。包含实际 ItemStack 的 `multiply.factor`、未知条件保留原行、三个组件描述器、火矢说明及瞬间治疗 II / 中毒 II / 再生 II 数值。没有人工 JEI 鼠标悬停验收。
- Effect foundation 与真实重启共 442 项通过。这是现有行为回归，不等同于上方待办的所有公共事件矩阵都已新增断言。
- 运行报告保存在 `build/verification/vanilla-combat-repair/vanilla-integration` 和 `build/verification/vanilla-combat-repair/effect-foundation`；包含脚本结果、环境清单与服务器日志。客户端报告位于 `build/verification/carried-effect-tooltips`、`build/verification/item-tooltips-{en_us,zh_cn}.json`、`build/verification/effect-display.json`。

面板脚本还补充了服务端 `isChangingDimension` / `hasClientLoaded` 等待，并断言致死调用确实成功，避免把客户端已切换维度误当作服务端已解除转场免疫。

最初启动失败原因是脚本仍引用已删除的 SDK 示例；早期客户端失败还包括 Rhino 重载选择、旧服务端物品投影拒绝预期、无支撑仙人掌夹具。修正后的最终结果单独记录，未将这些失败或历史文档中的通过次数冒充当前通过证据。一次 clean 因失败验证留下的临时 Java 服务端持有文件而失败；确认该进程属于隔离验证实例并结束后，重新 clean build 成功。

本轮汇总：`build/verification/vanilla-combat-repair/summary.json`；整份计划的完成状态仍为 false。

## 继续实施：公开输入、原生包装和来源

`DamageContextView.nativeInput()` 返回不可变 `NativeDamageInput`：基础伤害桶、攻击缩放与附魔增量、各减伤环节执行归属、冻结的护甲效率和保护评分，以及父类入口前已发生的调整。`activeChannels()` 返回不可变副本。原版 bridge 处理器统一使用公开上下文、`tryAddVanillaBaseReconstructedDamage` 和 `recordContribution`，不再强转内部上下文；贡献写入在计算结束后拒绝。描述工厂现在位于 `damage.api.display`，不提供旧包兼容门面。

启动期 `registrar.nativeDamage()` 返回 `NativeDamageAccess`，内部负责 hurt scope、完成/异常清理、护盾消耗回执、生命写入观察、攻击 moment 和横扫作用域。`LivingEntityDamageSettlementMixin`、`LivingEntityApplicationMixin`、`PlayerApplicationMixin` 与 `DamageApplicationAdapter` 只使用公开合约，不接触 Runtime 或 SPI。业务查询仍不创建或推进世界。犰狳在真实父类调用处观察原始量和实际传入量，记录到冻结输入，不重新应用减伤。

`SourceProvenance` 分别保存可确认的实体、物品、槽位、附魔 ID 和维度＋方块坐标；空字段表示未知。它独立于 `EffectSnapshot.source` 与叠加键，随 `EffectDisplay`、隐藏层、提交事件、持久化及 `EffectView` 传递。`registrar.effectOrigins()` 提供同步原生调用包装，退出后恢复外层来源。金苹果等 Consumable 记录使用者与物品；不猜测槽位。信标与潮涌核心记录实际位置；附魔施加效果记录 `EnchantedItemInUse` 的物品、槽位和已知持有者，此入口尚不知道附魔注册 ID，因此该字段保留未知。

新增 AutoJS 断言包括：犰狳 9 点伤害实际减为 4 点且仅计算一次、调整作用域不泄漏、公共贡献写入过期拒绝；洞穴蜘蛛、蜜蜂、凋灵骷髅、尸壳的真实近战及闪避；流浪者、沼骸和 Parched 实际发射箭并推进飞行命中，分别验证缓慢、中毒和虚弱及一次公共事件、原生施加者；真实金苹果消费及来源作用域结束。没有用直接 `addEffect` 代替这些攻击入口。

复现过程中修正了犰狳父类调用 Mixin 接收者签名，未降低 require。mob 测试最初把实体移到未加载区块，导致实体查找不成立；修正夹具位置后重跑通过，该失败不记作业务修复。

继续补充的真实入口覆盖河豚膨胀接触、凋灵头颅与潜影弹飞行碰撞、女巫按距离选择缓慢药水并发射后触发喷溅，以及效果云 tick。女巫用例将已发射药水移到目标前方以固定碰撞，未验证随机投掷命中率。持续效果云原版传入云实体自身，测试确认来源不被替换成云主人。狼铠使用固定随机种子的耐久 III，核对实际减免和全部回执之和。

### 继续实施实际结果

- 活动 target 目录 `gradlew clean build`、common 独立 `clean build` 通过。
- common 边界检查 1155 个编译类，发布检查 874 个活动源码类通过。增量公开边界覆盖 29 个适配器/处理器/扩展示例；没有宣称剩余平台接线已全部迁移。脚本语法与 `git diff --check` 通过。
- 战斗脚本 139 项＋自然 tick 23 项，共 162 项通过；Effect foundation 与实际重启 442 项通过。
- 客户端 361 项通过：携带效果 199，双语物品/来源渲染各 47，活动 Effect 63，面板 5。
- 报告保存在 `build/verification/vanilla-combat-continuation`。清理构建后重新运行两套服务端验证并分别归档，避免报告被 clean 或另一套件覆盖。客户端最终轮次包含新增来源渲染。
- 实例均已停止。完整计划仍未完成，待办以上方清单为准。

## 再次继续实施：平台合约与真实来源

`WorldLifecycleAccess` 与 `NativeWorldHooks` 承接 Runtime 创建和生命周期推进；`NativeEffectsAccess` 承接投影查询、环境采样和非托管事件观察。`NativeDamageAccess` 增加生命写入证据和不透明吸收生命状态。target 的世界、持久化、Effect、治疗、死亡/保护/部位命中、投射物事件、重生与换维度接线不再引用内部实现或 SPI。公开扩展示例验证错误线程及计算阶段不能推进生命周期，既有过期贡献和装备回执约束继续验证。

附魔迭代作用域采集注册 ID；真实节肢杀手攻击确认施加者、铁剑、主手与附魔 ID。药箭记录实际原始物品；海龟壳记录实际 EQUIPPABLE 槽位。信标/潮涌核心通过实际方块实体 tick 供给效果，确认维度和坐标且不拆分叠加身份。装备绑定的 Effect 保存实际持有者、物品和槽位。

公共事件新增直接 API、普通倒计时无事件、隐藏层恢复一次、自然到期一次、取消到期后重试、失败提交与失败补偿无事件、成功重试一次的断言。真实火球飞行碰撞生成龙息云，推进原生实体年龄后验证一次瞬间伤害；致死伤害经死亡 tick 触发一次渗浆，并发布一次 DEATH 移除事件。手动云 tick 必须同步推进实体年龄，不能用未越过原版等待期的调用断言无伤害。

验证脚本增加 `platform-lifecycle` 套件；客户端完整运行现在加载同一公开扩展示例，并验证真实换维度和死亡重生的公共事件。最新结果见下表；中途编译或夹具失败未记为业务修复。

### 当前支持与证据矩阵

| 项目 | 执行 | 来源 | 说明 | 公开 API | 运行证据 |
|---|---|---|---|---|---|
| 狼铠 | 独立实际耐久回执；优先于护盾；零生命消耗不吸血 | 物品＋BODY 槽 | 组件说明 | EquipmentDamageReceipt / EquipmentDamageAccess | vanilla：普通、破损、绕过、不可损坏、耐久附魔、护盾、取消、闪避 |
| 守卫者/远古守卫者 | 托管成功结算后一次派生反伤；父预算防递归 | 冻结参与者与原生直接攻击者 | 不新增物品提示 | 公共结算策略和 DamageRequest | vanilla：静止、移动、护盾、取消、闪避、标签、递归、非托管边界 |
| 原版战斗药水与 mob | 周期、瞬时及原生攻击入口 | 已知实体；未知不推测 | 等级、基础量、周期、生命底线 | EffectCommands 与不可变事件 | vanilla 实际近战/投射物/云/龙息/致死触发；foundation 周期/隐藏/失败 |
| 消耗品、附魔、药箭、装备授予 | 沿用真实入口及叠加身份 | 持有者、物品、实际槽、附魔 ID | 本地组件/附魔与活动效果来源 | EffectOriginAccess / SourceProvenance | 真实金苹果、节肢杀手、药箭、海龟壳；platform 装备授予 |
| 信标/潮涌核心 | 实际方块实体 tick | 维度＋位置 | 活动效果说明 | 公开来源包装 | native-block-sources：两种实际授予、位置与叠加 |
| Effect 公共事件 | 成功提交一次；生命周期结束后送达通知队列 | 不可变前后实例含来源 | 活动效果仍使用服务端采用状态 | CombatMoment.Effect.managedChange | 自然到期/恢复/取消、失败补偿、死亡、转场、重载、实际重启 |
| 本轮平台接线 | 生命周期、原生调用和资源观察内收 | 不泄漏 Runtime | 面板查询仍不创建世界 | 50 文件静态检查；示例仅公开 API | 错误线程、计算阶段、过期回调、独立 common 与发布包检查 |

生命周期通知队列曾等待后续战斗操作才送达；现在生命周期权限退出后执行已有提交后通知。自然 tick 断言不再由脚本手工排空队列。换维度现在以原生移除标记发布 `DIMENSION_CHANGE`，真实客户端断言一次旧会话移除和一次目的地恢复；死亡后的重生不重复通知。实际定义重载不重发未变化的已采用 Effect。

### 最终实际结果（2026-11-07）

- 活动 target 目录 `gradlew clean build`、common 独立 `clean build`；common 边界 1170 个编译类、发布包 886 个活动源码类、本轮公开 API 边界 50 文件、AutoJS/Python 语法及 `git diff --check`。
- 战斗套件 179 项：主体 151、自然 tick 23、真实信标/潮涌核心 5。包括此次复现并修复的未注册来源环境入口异常，以及非托管守卫者原生路径。
- Effect foundation 与真实重启 452 项，包含新增 8 项提交事件断言和 2 项重启事件断言。自然事件用例依靠生产生命周期送达通知，不手工排空队列。
- 平台生命周期 106 项：查询边界 41、治疗失败 6、恢复/生命周期 13、所有权/失败补偿/装备来源 23、自然 tick 23。
- 客户端 560 项：携带效果 199，中英文物品各 144，活动 Effect 68，面板 5。补充的 16 种附魔逐一验证首级/最高级及普通/Shift；真实重载、转场与死亡重生验证公共事件。客户端运行包含既有 JEI，但不计人工悬停视觉验收。
- 服务端最终报告分别归档于 `build/verification/vanilla-combat-final/vanilla-final`、`foundation-final`、`platform-final`；各目录 `summary.json` 记录实际通过脚本与工作区摘要。客户端报告在同目录 `client`，汇总为 `build/verification/vanilla-combat-final/summary.json`。三套服务端包含共同的自然 tick 用例，不能把计数相加解释为互不重叠场景。

全部临时实例由验证脚本结束。没有新增 JUnit/GameTest、降低 Mixin require 或恢复物品查询网络。编译、自动运行和人工视觉覆盖分别记录，不将其中任何一项替代其余证据。

## 源码审查与剩余缺口

审查日期：2026-11-07。对象为当前工作区（包含既有未提交修改），活动目标为 NeoForge 26.1.2。沿投射物捕获/查询/碰撞、平台状态存储、compat 注册、规则异常处理及 AutoJS 启动链核查；不是全部源码的逐文件审计。本节优先于前文阶段性的“待办以上方清单为准”。

本次只修改文档，未修复下列业务或验证接线，未启动游戏、执行 clean build 或重跑历史验收。下列“源码确认”表示静态调用链成立；“验收缺口”表示尚缺对应证据，不等于已复现运行故障。P1 为建议优先补齐的实现/验证链，P2 为后续架构或专项验收；可选兼容扩展另列。

### G1 · P1 · 投射物快照没有保存恢复链（源码确认）

- [ProjectileLaunchMixin](../targets/neoforge-26.1.2/src/main/java/io/github/naimjeg/combatnexus/combat/health/platform/mixin/ProjectileLaunchMixin.java) 只保存 `@Unique ProjectileLaunch` 字段与一次性捕获方法，没有读写存档接线。[ProjectileFacts.launch](../common/src/main/java/io/github/naimjeg/combatnexus/combat/projectile/api/ProjectileFacts.java) 直接读取该字段。
- [CombatControlEvents.captureLaunch](../targets/neoforge-26.1.2/src/main/java/io/github/naimjeg/combatnexus/combat/platform/CombatControlEvents.java) 明确排除 `loadedFromDisk()`；因此投射物从磁盘重新构造后不会恢复或重新捕获原发射快照。
- [PlatformWorldLifecycle.collision](../common/src/main/java/io/github/naimjeg/combatnexus/combat/internal/platform/PlatformWorldLifecycle.java) 只在快照存在时发布 `CombatMoment.Collision`；[WorldServices.damageFormula](../common/src/main/java/io/github/naimjeg/combatnexus/combat/internal/WorldServices.java) 无发射快照时转而查询当前攻击者事实。依赖快照的碰撞通知和发射时属性语义因此无法跨磁盘卸载/重启保持。此结论不表示所有原版箭伤害停止执行。
- 完成条件：明确并实现原发射事实、动作身份和相关抽样状态的保存恢复政策；AutoJS 覆盖实际区块卸载重载、真实重启、发射后换装、射手离线/不存在及多目标命中。不能在读档时采样当前装备冒充原快照，也不能重复触发发射成本。

### G2 · P1 · 旧 SDK 示例删除后，部分验收套件仍依赖它（源码确认）

- 2026-11-07 定义与接线修复：已删除 [target 构建](../targets/neoforge-26.1.2/build.gradle) 的旧 `sdkExample` source set、编译检查依赖、示例 Jar 和开发模组映射。公共边界检查改为扫描真实的 `scripts/autojs/effect-extension/java`，并在该目录缺失时失败。
- [运行器](../scripts/verify-capabilities-runtime.py) 和客户端脚本已移除 `sdkExample` 参数，打包模式删除旧 SDK loadedMods 映射；foundation、vanilla、platform、all 仍使用临时 Effect 扩展。此清理不补齐下列旧 SDK 场景。
- [verify-bridge-plans.js](../scripts/autojs/verify-bridge-plans.js) 直接加载 `example.combatnexus.PlanExample` / `SdkExample`；[verify-semantic-extension.js](../scripts/autojs/verify-semantic-extension.js)、[verify-sdk.js](../scripts/autojs/verify-sdk.js)、[verify-native-commands.js](../scripts/autojs/verify-native-commands.js)、[verify-native-healing.js](../scripts/autojs/verify-native-healing.js) 和普通 persistence-restart 脚本也仍引用旧示例。当前源码中没有这些类，新 `probe.EffectExtension` 不提供替代实现。
- 影响：干净环境下这些套件缺少所需扩展，历史 `all`、最终制品桥接及普通持久化命令尚未形成当前可复跑链。脚本语法通过不能发现 Java 类缺失；新 foundation 的真实重启通过也不覆盖普通 persistence-restart。
- 完成条件：将仍需验证的扩展迁入临时验证 source set，更新旧类、定义及启动引用，删除无效构建接线；从干净环境实际执行受影响套件，保存环境与结果。保留公共 API 示例隔离，不恢复已移除 JUnit/GameTest，不把缺失场景静默跳过。

### G3 · P2 · 平台公开合约迁移仍有明确剩余范围（源码确认）

- [LivingEntityRuntimeStateMixin](../targets/neoforge-26.1.2/src/main/java/io/github/naimjeg/combatnexus/combat/health/platform/mixin/LivingEntityRuntimeStateMixin.java) 直接构造 `RuntimeStateStore` / `EntityScheduleStore`，依赖 `NativeRuntimeStates` / `NativeSchedules` SPI，并调用 `CombatScheduleAccess.tick`；投射物存储 Mixin 同样实现 `NativeProjectile` SPI。
- [IncomingDamageHandler](../targets/neoforge-26.1.2/src/main/java/io/github/naimjeg/combatnexus/combat/damage/platform/event/IncomingDamageHandler.java) 仍访问 `CombatServerRuntime`、`CombatExecutionScope` 和伤害管线内部；[TooltipDefinitionSync](../targets/neoforge-26.1.2/src/main/java/io/github/naimjeg/combatnexus/combat/damage/platform/config/TooltipDefinitionSync.java)、[ServerSettingsAdapter](../targets/neoforge-26.1.2/src/main/java/io/github/naimjeg/combatnexus/combat/damage/platform/config/ServerSettingsAdapter.java) 仍依赖内部定义存储。
- [边界脚本](../scripts/verify-vanilla-combat-boundary.py) 显式枚举本轮适配器，未覆盖上述旧入口。50 文件通过证明该集合收敛，不能代表全部 target 已经仅用公开 API。既有平台内部接线是迁移债务，不仅凭 import 判定运行错误。
- 完成条件：按状态存储/调度、投射物存储、伤害准入及定义接线分别明确合约，逐组迁移并扩展检查集合；验证死亡/转场/重启、错误线程/阶段及失败保存原数据，避免开放可伪造的事务权限。

### G4 · 可选扩展及验收缺口 · 神化覆盖与当前组合回归

- [ApotheosisBridge](../targets/neoforge-26.1.2/combatnexus_compat/src/main/java/io/github/naimjeg/combatnexus/compat/apotheosis/ApotheosisBridge.java) 注册属性开关、Executing、MageSlayer、BloodyArrow；[AttributesBridge](../targets/neoforge-26.1.2/combatnexus_compat/src/main/java/io/github/naimjeg/combatnexus/compat/apotheosis/AttributesBridge.java) 注册多重暴击与 Overheal。当前是六类适配，不是全部 affix/gem 翻译。
- `ApotheosisBridge.nativeCapability` 存在但没有调用点；覆盖注册表不能提供所有未接管词缀/宝石的逐类型“保留原生”清单。不能根据少数能力注册推导全量接入。
- [桥接历史记录](legacy/BRIDGE_IMPLEMENTATION.md) 和 [SDK 历史结果](legacy/PUBLIC_SDK.md) 包含真实组合通过证据，不能改写为“神化从未验证”。但这些结果早于统一 Effect/最新战斗修复；最新组合重跑还受 G2 的旧示例依赖影响。
- 完成条件：先恢复六类既有适配的最终 Jar 组合回归（无神化、仅 Attributes、完整锁定组合），再按实际需求决定是否扩大适配；全量适配若进入范围，应建立逐类型执行归属和验收矩阵。未接管玩法继续原生执行本身不是缺陷。

### G5 · P2 · stackingValue 异常仅有实现，缺少故障运行断言

- [DamageRuleStackingResolver.safeStackingValue](../common/src/main/java/io/github/naimjeg/combatnexus/combat/damage/core/rule/DamageRuleStackingResolver.java) 捕获异常、记录诊断并跳过该次取值；没有有效值时返回 0。[verify-diagnostics.js](../scripts/autojs/verify-diagnostics.js) 验证空规则、日志和条件元数据，但没有实际抛出 `stackingValue()` 异常的扩展。
- [历史说明](legacy/DEVELOPMENT.md) 已记录 Rhino 默认方法注入限制。本次检索现有 AutoJS Java/JS 扩展未发现补齐用例，不将源码 catch 分支当作运行通过。
- 完成条件：用临时 Java 扩展实现真正抛错的 operation，AutoJS 驱动 HIGHEST_VALUE / LOWEST_VALUE 比较；明确所有值失败回退 0 的选择语义，核对日志、后续规则执行及次数。当前诊断文本声称比较失败保留已有规则，而返回 0 后仍参与比较，尤其 LOWEST_VALUE 可能选择失败候选，这一语义差异应一并验证和定案。

### G6 · P2 · 人工视觉验收仍未执行

本地 ItemStack、JEI 渲染器和真实客户端 tooltip 事件已有自动验证；[物品提示记录](legacy/ITEM_TOOLTIPS.md) 及上文最新客户端记录仍明确排除人工鼠标悬停。尚需在实际背包、创造目录、容器和 JEI 中检查普通/Shift、双语、长描述、屏幕边缘布局及其他模组提示共存，记录实际客户端组合和结果。自动事件检查证明内容与路径，不替代可读性和实际操作验收。

### G7 · 已解决当前入口 · 历史正文保留

本次开始审查时 `docs/DEVELOPMENT.md`、`docs/EFFECTS.md`、`docs/ITEM_TOOLTIPS.md` 已从工作区删除，相应资料位于 `docs/legacy`；[AGENTS.md](../AGENTS.md) 仍引用缺失的开发入口和 `docs/scaffold-source-manifest.json`。legacy 中也保留迁移前的相对路径、旧 SDK 命令及阶段状态。以上为当前工作区状态，未把文档搬迁归因于本轮审查。

2026-11-07 后续修复已恢复 [DEVELOPMENT.md](DEVELOPMENT.md)，新增 [当前领域定义](DOMAIN_DEFINITIONS.md)，README 链接当前入口，根开发约定的迁移清单改指向 `docs/legacy/migration/damage-source-manifest.json`。相关 legacy 入口标注历史状态并链接当前文档，正文及历史路径记录保留。G2 的旧套件命令仍不标记为当前通过。

### 已关闭项与不纳入项

狼铠回执、守卫者提交后反伤、托管 Effect 前后实例事件、生命周期通知送达、附魔 ID、药箭/装备/信标/潮涌核心来源已有实现和前述实际记录，不再重复列为未实现。前文“附魔 ID 未知”和“公共事件矩阵待补”等只代表对应阶段。

未启用 NeoForge 1.21.1、Fabric/Forge、旧存档/旧 API 兼容、任意第三方玩法全覆盖不属于当前支持承诺。无注册 ID 或明确绕过框架的原生伤害所有权也不是待修复的接管漏洞。

### 本次审查检查结果

`python scripts/verify-vanilla-combat-boundary.py` 实际通过，范围仍为 50 个适配器/探针。新增审查正文的 26 个本地源码/文档链接存在性检查通过，7 个缺口编号及正文尾随空白检查通过；README 与本记录的 `git diff --check` 通过（本记录为工作区未跟踪文件，另以正文检查覆盖）。这些是静态及文档检查，不新增游戏行为通过项。

## 战斗接管范围收窄与饥饿状态

2026-11-07，按用户最后确认的范围实施；本节取代前文原版 40 效果全托管的支持声明。

- 托管白名单为 26 项：speed、slowness、haste、mining_fatigue、strength、instant_health、instant_damage、jump_boost、regeneration、resistance、fire_resistance、invisibility、blindness、weakness、poison、wither、health_boost、absorption、glowing、levitation、slow_falling、darkness、wind_charged、weaving、oozing、infested。急迫/挖掘疲劳原版攻速每级 +10%/-10%，仅安装一次；原版采掘消费不改。
- 其余 14 项原版效果保留计时、属性及存储所有权。原版饥饿药水、饱和药水都不托管；它们与下述“食物耗尽状态”和“食物回血”是不同入口。非托管 `getEffect` 返回原实例，集合/map 查询保持原版，`removeAllEffects` 对非托管部分执行原方法；托管移除仍遵守取消事件。
- 删除全局 USE_ITEM 限制、普通右键拦截、流体/活塞/通用 push 拦截及导航 stop。保留战斗攻击、施法、移动控制与 knockback 限制；客户端 ATTACK 仅拦截实体攻击，不取消采掘左键。删除 USE_ITEM 改变了控制 mask 枚举顺序，需同版客户端/服务端，不声明旧网络兼容。
- 删除 `InstantFood`、`InstantExecution.feed`、对应 codec/描述器和文案。FoodData 的进食、食物值、饱和度、消耗度和回血周期不变；已有 `FoodDataHealingSourceMixin` 仅包装两处原版 `heal()`，正常饱和回血每 10 tick，非饱和回血每 80 tick，由原版决定是否触发，再交治疗管线处理。
- `combatnexus:starvation` 是食物值为 0 时持续存在的条件长效 Effect，恢复食物后解除，普通驱散不能解除。它接管饥饿伤害时钟：首次等待 80 tick，之后每 80 tick 尝试 1 点基础伤害，原版 starve 扣血入口被抑制，避免重复。简单/普通生命底线为 10/1，困难为 0；和平仍可显示食物耗尽状态但基础伤害为 0。取消或免疫消费本次周期，不补发；重新饥饿重新开始周期。
- 通用 `ScheduledEnvironmentDamage` 增加 `initialDelayTicks`，`EnvironmentEffectDefinition` 采用该参数，codec 保存 `first_delay_ticks`；原有六类接触伤害仍首次立即、每 10 tick。周期、首次延迟、基础伤害和生命底线由共同描述器/EffectTooltip 生成；持续条件说明来自环境展示定义，客户端不计算最终扣血。

本次 Java API、行为数据和网络控制枚举均按当前版本更新，不增加旧格式读取或旧接口兼容层。此前审查 G1–G7 中与本轮无关的投射物持久化、旧 SDK 套件接线等缺口仍保留。

验证脚本：`verify-combat-scope.js`、`verify-food-healing-boundary.js`、`verify-starvation-cadence.js` 已纳入 effect-foundation；同时更新原版所有权、原版行为、实际战斗入口、信标/潮涌核心、实际重启和客户端控制/显示断言。最终执行结果见下。


### 范围收窄实际验证

环境：Windows、Java 25.0.3、Minecraft 26.1.2、NeoForge 26.1.2.126、AutoJS 1.0.1、Rhino 2101.2.8-build.91；真实客户端包含既有 JEI。没有恢复 JUnit/GameTest 或降低 Mixin require。

- 活动 target 目录 `gradlew clean build` 与 common 独立 `clean build` 通过；common 边界 1169 类、发布包 886 个活动源码类、本轮 50 文件公开 API 边界、AutoJS/Python 语法和 `git diff --check` 通过。
- `python scripts/verify-capabilities-runtime.py --suite effect-foundation --start --restart --port 25595 --game-port 25570`：520 项通过，包含边界 43 项、原版食物回血 8 项、真实自然 tick 饥饿周期 15 项和实际重启 13 项。覆盖原版恢复事件正常/满血/取消、食物消耗度与饱和度记账，以及饥饿首次延迟/原生扣血抑制/周期取消/进食解除/重入/难度底线、行为 codec 和标准文本。
- `python scripts/verify-capabilities-runtime.py --suite vanilla-integration --start --port 25595 --game-port 25570`：179 项通过，包含保留原生的尸壳饥饿效果、战斗白名单来源及信标/潮涌核心所有权分离。
- `python scripts/verify-carried-effect-tooltips.py`：真实客户端 575 项通过，携带效果 195、中英文物品各 144、活动 Effect 77、面板 5、控制 10。包括双语饥饿周期/基础值/首次延迟/生命底线、进食后图标与缓存清除、普通右键和非实体左键保留、实体攻击限制及控制解除。没有人工鼠标悬停验收或新的神化组合验收。

报告归档至 `build/verification/combat-scope/{foundation-final,vanilla-final,client}`，汇总为同目录 `summary.json`。两个服务端套件共享自然 tick 检查，不把相加次数解释为独立场景。临时实例已由运行脚本停止。

准备过程中的失败为脚本将原生观察快照误当托管状态，以及 Rhino 对 Arrays.toString / EffectDefinition 重载选择不明确；已更正并重跑，失败不计通过。


## 定义顺序、引用校验与开发接线修复（2026-11-07）

Effect 行为引用改为有序 Identifier 列表，按 JSON 数组展开；空列表、重复引用及重复组件类型的既有规则保留。行为资源准备阶段对瞬时伤害、周期伤害、环境伤害及定时环境伤害检查本次 ResourceManager 的 damage_type 资源，失败阻止整批候选发布并保留旧代际，同一数据包新增类型可通过。运行期注册表解析和第三方组件 codec 合约保持原状。

旧 SDK 构建与运行参数接线已删除，边界检查改查实际临时 Effect 扩展；G2 中旧 SDK 场景恢复仍未完成。G7 当前文档入口已恢复，legacy 保留历史正文。构建、发布包、公共边界、语法检查通过；foundation 加真实重启和重载共 550 项通过，重载 16 项、重启 20 项。环境、初次脚本失败修正及逐项证据位置见 [当前开发验收](DEVELOPMENT.md#2026-11-07-定义顺序与接线修复验收)。
