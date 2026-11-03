# 核心、原版桥接与 compat

活动 target：NeoForge 26.1.2.126 / Java 25。只维护当前公开 API 和数据结构；没有存储版本、旧格式迁移或跨版本探测。definition generation 是运行时发布身份。

## 职责与制品

| 层 | 实现职责 |
| --- | --- |
| common | 冻结事实、有效属性策略、来源描述、通用公式与随机采样、领域命令、共同事务、状态与调度、结果证据 |
| 核心 NeoForge target | 原版事件与精确调用点、资源写入、实体保存/恢复与生命周期、服务端展示快照及客户端呈现 |
| 独立 combatnexus_compat | 锁定第三方 API 的事实翻译、算法组合、能力声明及执行抑制；只导入核心公开 API |

主 Jar 使用 [ModDevGradle 子项目 JarJar](https://github.com/neoforged/ModDevGradle/blob/main/README.md#jar-in-jar) 内嵌独立 compat Jar。compat 编译依赖核心 classes，避免反向依赖最终嵌套制品。核心根目录不混入 compat 类；第三方本体不进入任一制品。保持 common source-set sharing，common 独立编译不生成发布 Jar。

固定版本为 Apotheosis 9.1.0、Apothic Attributes 3.1.0、Placebo 10.0.2，发布文件和官方来源见 [开发约定](DEVELOPMENT.md#神化与灾变测试依赖2026-11-05)。compat 使用 compileOnly，开发运行分别通过 `verificationCompatibility` 或 `verificationAttributesOnly` 显式选择。入口不直接引用可选类型；Mixin plugin 在转换前判断模组存在，注入 require 保持 1。

## 注册、事实和执行归属

注册按收集、校验、冻结、世界运行排序。`BridgeRegistrar` 声明能力 ID、触发点、执行位置、执行者、模式和允许操作；同一能力与位置的多个替换者在启动失败。覆盖视图只描述已声明的能力，不能推断未列出的第三方玩法已接管。

`CombatWorld.bridges()` 为每次接管保留 invocation、action、target、贡献状态和领域证据。贡献区分待执行、已执行、已抑制、无法拆分。近战使用真实攻击 scope；投射物使用发射动作身份。bridge scope 只限制权限，不签发 ticket；共同 kernel 继续负责父子身份、权限交集、预算和完成队列。关闭调用保留有界历史；历史不保存实体、物品或执行句柄。

`FactKey<T>` 以 codec 冻结外部事实，采集失败显式记录缺失 ID。属性同时保存 raw、effective 和理由，读取原生有效 getter，避免绕过神化属性开关。来源描述只消费捕获的输入；宝石包含孔位和纯度、词缀包含实例等级。服务端面板投影传送解释与属性值，客户端不重算。

动作输入包含装备与武器、蓄力、攻击强度；投射物首次发射保存射手输入和武器副本，后续换装、射手移除及多目标命中使用原快照。`CombatMoment` 提供攻击、发射、碰撞、物品格挡、命中、攻击后、确认死亡及效果生命周期观察，碰撞保留直接实体与部件所属主体。物品格挡不等同于框架护盾抵扣。

伤害公式接收冻结上下文和受控采样器，记录每次样本与倍率序列；`shared` 可以按有效动作或动作/目标共享决策。采样器在回调结束后失效，同一 invocation 不可重掷。治疗保留独立 `HealingCalculation`，不复用伤害上下文。

## 写入前计划与领域命令

`BeforeApplicationPlan` 是有序的类型化领域意图，不接受 Runnable。伤害、治疗计算及发射应用使用同一执行器：完成纯计算与父准入后提交子操作，随后重新验证目标并使用冻结结果应用父操作。每步显式选择普通拒绝后继续或终止父操作。取消、满血和拒绝与不确定写入分开；异常/部分写入停止剩余步骤，成功子操作不回滚，不重算或重放父请求。

计划结果保留子治疗、每步 journal、操作身份和拒绝原因；治疗结算同样保留计划结果与自身身份。所有完成观察在共享完成队列派发，观察不能写入，反应只获得声明权限。

新增的原版应用出口包括：

- 原版效果施加、刷新与移除，保留原版实例、计时、modifier 所有权及事件拒绝。
- 原版吸收的来源账本、上限、授予、消费和撤销，与框架护盾池分离；保存恢复保留归属。
- 非致死生命校正与生命成本；真实治疗仍走 healing；处决走统一致死协调而非大额伤害。受管保护保持写入前时机，原生保护保持原生边界，图腾证据记录实际物品消耗、保护后的生命和效果。
- 发射阶段的投射物倍率修改、点燃、位置/速度命令及实际写入证据。投射物修改校验世界、来源和发射时机。
- 冷却及类型化运行状态，按实体、来源安装、动作分域。`StateKey` 明确持久化和死亡、重生、卸装、换维度、动作结束的清理政策，不统一清空所有状态。
- 一次性/周期调度采用序列化 `ScheduledIntent`，原生实体 tick 驱动，共用准入与预算。到期步骤写入前消费；不确定写入不自动重试。持久任务使用实体原生保存恢复。
- `restoredSource` 为已注册 owner 重交安装句柄，普通查询只读；`definition` 描述当前采用的 ID 与命名参数，`install` 按已采用 ID 安装，`observeDefinitions` 通知重载。在途请求继续使用捕获代际。

原版补充包含玩家攻击、投射物发射/碰撞、物品格挡、效果生命周期、死亡保护和确认死亡。末影龙水晶的实际恢复调用进入 healing，来源为水晶 UUID；初始化、读档、最大生命校正及死亡写血不冒充治疗。

## 六类神化实现

| 场景 | 接管与证据 |
| --- | --- |
| 属性开关与来源 | 采集真实 effective 值与开关原因，冻结词缀/宝石实例并提供展示说明；原生属性计算仍归第三方 |
| 多重暴击 | compat 组合原版 float 倍率和 0.85 衰减循环，保留抽样顺序、倍率序列及粒子；抑制原暴击路径 |
| MageSlayer | 魔法命中冻结比例，写入前子治疗，然后应用已计算减伤；子治疗取消/满血仍继续，父随后取消不回滚子治疗 |
| Overheal | 在原 post-hit 的 Overheal 属性读取处精确接管，保留原生 lifesteal；按实际伤害与原生上限授予来源吸收 |
| BloodyArrow | 发射快照、原生伤害型生命成本、箭倍率与冷却组成同一计划；卸装不清除冷却，继承预算允许成本引起的子治疗 |
| Executing / 图腾 | 冻结阈值和攻击强度，抑制原处决，使用统一致死协调；保护可拒绝死亡、消耗一次图腾并应用原生副作用 |

没有实现神化全量翻译。未接管的词缀、宝石及其他玩法继续原生执行。只支持上述锁定版本，不能据此声称任意第三方组合或旧存档兼容。发射快照当前为实体运行期快照；此轮保存重启验收针对来源、冷却、状态、吸收和调度。

## 验证

```powershell
# 活动 target
.\gradlew.bat clean build --console plain
# 仓库根目录
.\gradlew.bat -p common clean build --console plain
python scripts/verify-common-boundary.py
python scripts/verify-baseline.py
python scripts/verify-compat-boundary.py
python scripts/verify-capabilities-runtime.py --syntax-only
python scripts/verify-capabilities-runtime.py --suite all --start --packaged --restart --reload --port 25594 --game-port 25569
python scripts/verify-capabilities-runtime.py --suite bridges --start --packaged --attributes-only --port 25596 --game-port 25571
python scripts/verify-capabilities-runtime.py --suite bridges --start --packaged --compatibility --port 25595 --game-port 25570
```

`--packaged` 去掉核心源码模组映射及独立 compat 依赖，从最终 Jar 启动；SDK 示例单独加载。`bridges` 包含 bridge-plans、native-commands、compat-scenes，未安装模组的场景明确跳过。脚本核对实际类加载、执行次数、数值、时机与来源，不将启动成功视为语义通过。没有恢复 JUnit/GameTest。

客户端使用 `scripts/autojs/client.init.gradle`、`verificationPackaged=true`、`verificationCompatibility=true` 启动，并运行 `verify-architecture-client.py --compatibility`，检查真实 C 键请求/响应及宝石来源、raw/effective 原因。生成报告、日志与截图保留在 target 的 build 下，不入库。

最终结果见下方验收表。

### 最终制品实际验收（2026-11-06）

环境：Windows、JDK 25.0.3、Minecraft 26.1.2 / NeoForge 26.1.2.126、AutoJS 1.0.1、Rhino 2101.2.8-build.91；组合版本如上。以下均从最终嵌套 Jar 运行，SDK 示例单独加载。

| 验证 | 实际结果 |
| --- | --- |
| target / common clean build | 均通过；common 不生成发布制品 |
| common 公共签名及源码/字节码边界 | 1,057 个编译类通过 |
| 活动 target 基线制品 | 797 个活动源类、元数据、Mixin、资源和许可证通过 |
| compat 制品与依赖边界 | 唯一嵌套 compat、核心根目录隔离、无第三方本体通过 |
| 无神化最终 Jar | 1,462 项通过：29 份脚本、自然 tick、定义重载、真实保存重启 |
| 仅 Apothic Attributes | 89 项通过：三份桥接脚本和自然 tick；神化专属场景跳过 |
| 完整神化组合 | 131 项通过：三份桥接脚本和自然 tick |
| 组合客户端 | 5 项面板/来源/有效属性检查，另 5 项展示生命周期回归通过 |
| AutoJS/Python 语法、git diff --check | 通过 |

原始服务端结果在 `targets/neoforge-26.1.2/build/verification/{isolated,attributes-only,compatibility}-packaged/`，客户端报告为 `build/verification/client.json`。截图在 `build/settings-client-run/screenshots/bridge-gem-source.png` 与 `bridge-effective-value.png`。生成结果不入库；CI 已加入三种最终制品环境，当前记录来自本地实际运行，不宣称远端 CI 已执行。

六类组合检查包含原版 float 算法对照、实际原生事件调用、MageSlayer 满血/取消/父取消、Overheal 实际来源授予、BloodyArrow 成本/倍率/卸装冷却、发射后换装及射手移除后的双目标调用、Executing 被图腾保护和保护消费。通用计划脚本覆盖计算回滚、部分提交与未知写入后不重试，以及治疗父计划的子身份。既有回归保留预算与观察权限拒绝检查。真实重启恢复来源、保护消费、冷却、类型化状态、吸收账本和待执行任务，并使用重交句柄撤销准确来源。

客户端脚本等待装备的原生 tick 采用和最新面板响应后检查，避免将首次显示的缓存快照误判为当前服务端结果。未改动客户端权威计算，也未用延时重试业务操作来掩盖写入失败。

本轮没有开展任意神化玩法全覆盖、跨版本或投射物卸载重载快照恢复验收；这些不由六类场景通过推导。

最终核心 Jar SHA-256：`b80a3c40dab12d8802a130a65872abb0976eb97016a4f7c1b38b8a3749fcde21`。三份服务端 summary 各自记录工作区摘要；其间仅更新客户端验收脚本、CI 和文档，生产 Jar 相同。验收进程均已正常关闭。


## 语义通用化后的桥接调用（2026-11-06）

应用前计划的命令迁至 `DomainCommand`；MageSlayer/BloodyArrow 调用同步迁移。`beforeApplication().steps()` 提供命令 ID、完整领域结果、上下文与日志，删除治疗专用列表。原生抑制位置及收益规则保持不变。

注册组合意图见 [当前 SDK 合约](PUBLIC_SDK.md#内部语义通用化2026-11-06)。意图使用独立 INTENT 准入，子操作仍受桥接、父操作和根预算限制。兼容运行结果随 SDK 通用化验收记录维护。
