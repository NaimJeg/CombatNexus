# 战斗行为 ownership 审查与修复（2026-11-05）

范围：当前 S1–S10 受控实现；不扩展玩法范围。最终运行验收仍未执行。

| 编号 | 问题 | 修复要求 | 状态 |
| --- | --- | --- | --- |
| B1 | 公开反应服务允许清空去重、触发计数及恢复补偿 | 公共接口只读；派发走可信 SPI；内部状态写入检查线程、会话和执行阶段 | 源码已修复，运行待验收 |
| B2 | 来源账本公开安装、时间推进和清理，绕过 Effect 账本 | 公共接口只读；安装与补偿由 Effect 贡献端口拥有；生命周期由组合根推进 | 源码已修复，运行待验收 |
| B3 | damage 强转来源实现，target 直接使用动作内部作用域 | 应用决策通过窄 SPI 返回消费回执；动作接线通过可信 SPI | 源码已修复，运行待验收 |
| B4 | 来源账本同时拥有保护、周期治疗和派生护盾政策 | 分离执行器；来源账本只协调安装身份、撤销和补偿；护盾池身份规则单一所有者 | 源码已修复，运行待验收 |
| B5 | 治疗逐来源截断令加法结果取决于来源排序 | 固定顺序汇总增量后统一截断，再应用 received；明确数值变化 | 源码已修复，运行待验收 |
| B6 | 治疗贡献事实未随计划与结果保留 | 保存有界不可变事实，成功、取消及异常结算均保留已完成计算解释 | 源码已修复，运行待验收 |

验证要求：AutoJS 覆盖只读接口、过期派发、来源协调、保护消费、正负贡献组合及结算事实；执行 target/common clean build 与边界/产物检查。脚本语法通过不等于运行通过。

## 最终所有权与接口

- `CombatRuntime` 仍为组合根；`behaviors()` 返回独立只读视图（capture/view/valid），`reactions()` 返回独立诊断视图（facts）。调用方拿不到实际安装或反应执行器，不能通过业务接口修改额度、去重或另建安装端口。
- `CombatBehaviorAccess` 是可信领域接线 SPI。其短期 Permit 只允许访问组合根接线，不签发事务权限：派发仍校验当前真实回调；保护仍要求 DAMAGE 的 APPLICATION 作用域并绑定实际世界/目标票据。
- `CombatActionAccess` 是原生攻击动作 SPI；动作身份只作因果证据。damage/target 不再依赖 `WorldBehaviorSources` 或 `CombatActionScope` 的具体实现。
- `WorldBehaviorSources` 负责安装身份、active/blocked、Effect 回执和补偿。保护额度由 `ProtectionExecutor` 拥有；周期治疗计划和调度由 `PeriodicHealingExecutor` 拥有；溢出池 ID、授予和撤销统一由 `OverflowShieldExecutor` 拥有。它们为内部实现，不提供业务写入口。
- 补偿保留保护消费与反应计数；移除来源仍撤销其派生护盾与周期任务。源码检查不替代部分失败、生命周期和原生重入实测。
- `HealingContribution` 保存来源 ID、运算类别和计算前后值，最多 66 条（64 个来源、一次限幅、一次 received）。计划和结算复制不可变列表，取消及应用异常也保留已经完成的计算事实。它们是计算解释，不是生命写入证据；未完成计算仍回滚工作区。

## 数值变化及破坏性 API 变化

治疗固定增量改为按固定来源顺序求和，再统一 `max(0, sum)`，最后应用 received。基础 1、贡献 −5/+5、received 1 时，原来负贡献先执行会得到 5，现在得到 1；这属于明确的数值修复。全正贡献和伤害暴击随机消费位置不变，不承诺不同浮点归约顺序位级一致。

移除 `BehaviorSources` 的 compose/tick/clear/close 和 `CombatReactionService` 的派发、计数、补偿及清理方法；不提供旧签名门面。`HealingApplicationPlan` 与完整 `HealingSettlement` 构造参数新增贡献列表；无计算结果的简便结算构造仍生成空事实。`BehaviorPlan` 继续限定本轮样例范围，不宣称已经支持通用行为 DSL。

## 本次检查与运行边界

2026-11-05，在基线提交 `0771c158570d6a41671d81d0b1dca4a1e9229042` 上的本次工作树执行：

| 检查 | 实际结果 |
| --- | --- |
| 活动 target 目录 `gradlew clean build` | 通过；空 test 任务不计行为覆盖 |
| 根 wrapper `-p common clean build` | 通过 |
| `python scripts/verify-common-boundary.py` | 通过；793 个独立 common 类，新增只读 API 与领域/SPI 依赖检查 |
| `python scripts/verify-baseline.py` | 通过；619 个活动源码类及 metadata、Mixin、资源、许可证 |
| 六个行为 AutoJS 脚本 `node --check` | 通过，仅语法 |
| Python runner/边界脚本语法检查、`git diff --check` | 通过 |

环境：Windows 11、Gradle 9.8.2、Gradle launcher JDK 21.0.11、Java 编译目标 25、NeoForge 26.1.2.126、CombatNexus 0.1.0-SNAPSHOT。构建仍有既有 deprecated API/Gradle 警告。

扩展了 `verify-domain-pipelines.js`（混合正负贡献、统一限幅、不可变事实、来源撤销后历史、容量及失败回滚）、`verify-combat-reactions.js`（只读权限、SOURCE 额度、派生池撤销、取消/UNKNOWN 事实、过期 SPI）、`verify-behavior-sources.js`（禁止独立修改来源账本）、`verify-preapplication-decisions.js`（无应用权限拒绝）。保留调度和动作脚本。

AutoJS 实际执行次数为 0：遵循先前仅源码与构建检查的限制，没有启动服务器。上述新增行为断言、原生取消/重入/异常、客户端、网络、重载与性能仍未验收。此次 clean 可能删除历史 build/verification 输出；历史通过记录不能作为本次运行证据。

## 四项边界修复与运行验证（2026-11-05 后续）

用户要求直接修复本轮审查发现的四项问题，并将 AutoJS 服务端端口改为 25590。本节记录本轮实际结果，不覆盖前文其他功能的验收缺口。

1. 装备账本分别记录尝试安装与成功安装。只有 Effect 返回 true 才确认成功；异常和 false 返回进入重试集合。重试先协调旧尝试，再读取当前装备，卸装不会安装过期事件中的装备。成功安装后被驱散或自然过期仍不补充。实体清理和世界关闭同时清除重试状态。
2. `CombatPanelService` 改为仅含 request 的业务接口；`presentation.internal.DefaultCombatPanelService` 持有限频状态和 clear/reset/close，Runtime 只返回独立门面。保留的门面在所有者关闭后拒绝请求。
3. `RegenerationScheduler.recordDamage` 从业务 API 移除，仅内部控制接口保留。恢复延迟通过 `CombatBehaviorAccess` 接入，Runtime 核对当前真实 damage 回调、服务器和世界，并检查确认损失，使用世界当前时间写入。业务方不能提交任意受伤 UUID/时间。
4. `DamageServerSettings`、`DamageSafetySettings` 分别迁至 `combat.config.CombatServerSettings`、`CombatSafetySettings`；原版减伤专用配置继续归 damage。删除独立预算 configure 入口，以及无调用方的旧 settings-only reload listener；预算只随完整代际采用。现有 formulas.json/damage.json 数据格式保持不变，limits 的解析和执行归 combat。

以上为破坏性 Java API 调整，不保留旧类名或生命周期门面。调用方应从 Runtime 获取面板服务，公共配置使用新包；恢复延迟由结算协调负责。无命名空间或协议格式变更。

### 实测环境与范围

Windows 11、游戏 JVM JDK 25.0.3、Gradle 9.8.2、Minecraft 26.1.2 / NeoForge 26.1.2.126、CombatNexus 0.1.0-SNAPSHOT、AutoJS 1.0.1、Rhino 2101.2.8-build.91。隔离目录为 `targets/neoforge-26.1.2/build/boundary-run`；AutoJS 25590，游戏 25592。旧测试服正常停止后执行 clean，再启动本轮服务器。

实际同时加载 Apotheosis 9.1.0、Apothic Attributes 3.1.0、Placebo 10.0.2、JEI 29.53.0.115、Just Enough Characters 4.6.9、MezzConfig 0.6.10、spark 1.10.187。使用仓库内置数据，设置脚本临时采用新代际并在 finally 恢复；没有额外测试 datapack。这不是这些模组的完整兼容性验收。

| 检查 | 实测结果 |
| --- | --- |
| 活动 target `gradlew clean build` | 通过，7 个任务；首次因旧测试服占用文件失败，正常停服后重跑通过 |
| 根 wrapper `-p common clean build` | 通过，5 个任务；common 不发布 jar |
| `verify-common-boundary.py` | 通过，849 个独立 common 类；新增面板生命周期、恢复写入口和配置归属断言 |
| `verify-baseline.py` | 通过，667 个活动源码类及产物检查 |
| `verify-ownership-boundaries.js` | 19 项通过：真实属性端口的部分失败/补偿、未换装备重试、待补偿阻止安装、卸装、免疫 false 返回、面板限频和关闭、恢复写入拒绝 |
| `verify-runtime-query-boundary.js` | 41 项通过：查询无推进副作用、五类服务不暴露生命周期/受伤写入口、关闭后拒绝 |
| `verify-settings-snapshot.js` | 6 项通过：公共配置、预算随代际更新、无独立 configure、旧代拒绝、捕获句柄保持 |
| `verify-recovery-and-lifecycle.js` | 13 项通过：20 tick 累积、取消/跳 tick、100 tick 战斗窗口、独立延迟、到期、会话替换 |
| `verify-domain-definitions.js` | 33 项通过：领域定义、装备多绑定、属性安装和撤销、驱散后不补充 |
| `verify-combat-reactions.js` | 26 项通过：真实伤害经结算写恢复延迟、反伤/溢出护盾、失败结算和过期授权 |

最终 6 份脚本共 138 项断言通过。准备脚本先使主世界和下界的测试区块可查询实体；新故障脚本通过现有构造器建立隔离下界 Runtime，finally 关闭并恢复平台会话，不修改生产故障注入接口。实际运行中修正了 Rhino 不支持的反射路径及块内函数声明；失败的脚本尝试不计通过。

运行命令为 `python scripts/run-autojs.py scripts/autojs/<脚本>.js --port 25590 --timeout-ms 30000`。结果保存在未入库的 `targets/neoforge-26.1.2/build/verification/ownership/*.json`，服务器日志在 `build/boundary-run/logs/latest.log`。共享启动和 Python 验证脚本的服务端默认端口统一为 25590，仍支持显式覆盖。客户端网络、真实资源重载及性能本轮未测；未新增 JUnit/GameTest。
