# 架构边界重构（2026-11-05）

本次修复 Runtime 查询副作用、服务生命周期权限外泄、展示全局扫描、客户端重载失效不完整及完整定义归属 damage 五项问题。仅维护 NeoForge 26.1.2.126 / Minecraft 26.1.2，编译工具链 Java 25。按用户要求只执行源码、构建和脚本语法检查，没有启动游戏或服务器。

## Ownership 与调用链

世界 Load 创建 CombatRuntime 并初始化当前 tick；LevelTickEvent.Pre 经 CombatWorldLifecycleAccess 的短期 Permit 推进过期和补偿，然后调度恢复及行为。runtime(level) 只查找已加载会话。同 tick 推进立即返回，倒退拒绝；属性和战斗输入捕获要求已初始化且 tick 与世界一致，缺失推进直接报错。查询不会替调用方推进世界。

Effect、护盾、治疗、恢复的业务接口不再提供 create、tick、clear、reset、close 或 drainChanges。组合根创建实现并持有各领域 internal 的 Control 接口，返回独立业务门面；生命周期仍经现有可信 SPI 调度。真实服务端线程、世界会话、事务阶段检查保留。内部接口及 SPI 不构成恶意 Java 扩展安全沙箱。隔离故障脚本显式构造内部实现，不把测试构造方式作为业务 API。

Effect pending 在新 tick 重试一次；同 tick 重复推进、snapshot、restrictions 和 pendingSources 不补偿。显式 apply/remove/clear 保留针对来源的协调。失败贡献不会提交 active 状态。

Effect active、护盾池及行为来源以 target 分组，组内保留来源身份索引。展示只访问本人分组；替换沿用来源位置，撤销、过期及实体清理删除空分组。Effect/护盾另保留只含 Key 的插入顺序索引，使世界级过期处理维持原跨实体顺序。反应历史仍全局最多 256 条，同时维护按 target 的有界索引，展示最后 16 条。行预算在添加结果前扣除，字符串和编码预算保留。属性数值与 provenance 仍同次捕获，没有新增跨请求属性缓存。

## 定义与客户端失效

combat.api.DefinitionGeneration 是完整不可变代际，包含 settings、damage 分片及通用 behaviors。combat.internal.definition 拥有整批准备、编译和发布；damage.core.definition.DamageDefinitions 拥有 channels/rules/templates。所有领域访问显式使用 generation.damage()，不再将接口强转为 damage Snapshot。

只有成功采用资源才发布完整代际，内核同时采用该代的预算设置；DamageSettingsStore 是从代际派生的只读视图，其 revision 现在等于 generation，包括设置值相同的成功重载。缓存随采用失效。失败重载保留旧定义，旧代不能覆盖新代，登录只同步当前代际，在途请求继续持有已捕获完整定义。

服务端登录及成功资源采用后发送 CombatGenerationPayload。客户端集中保存连接级 generation，通知新代时清理物品缓存、取消 panel/item pending，并使已打开面板回到等待状态，随后按原有请求节奏更新。换世界或玩家替换只清世界状态，断线清全部状态。收到展示回复不会改变 generation。

CombatPresentationRequest 新增 generation 字段。服务端先做限频、代际及容器/槽位检查，再取 Runtime 并投影。回复须匹配当前 generation、pending generation、nonce、世界、session 和容器身份；物品另核对槽位及 ItemStack。generation 仅为运行身份，没有新增协议版本、握手或旧签名兼容门面。

## API 迁移

- 业务方从 CombatRuntime 取得服务；删除业务接口静态工厂及生命周期方法的调用。平台生命周期使用 CombatWorldLifecycleAccess。
- 原 damage.core.definition.CombatDefinitions / CombatDefinitionReloadListener 移至 combat.internal.definition；原 CombatDefinitions.Snapshot 改为 combat.api.DefinitionGeneration，构造时传入 DamageDefinitions 分片。
- channels/rules/templates 从 definitions.damage() 读取。DamageSettingsStore 不再独立 publish/clear。
- 展示请求构造及 codec 必须携带已通知的 generation；旧 payload 不兼容，不提供旧存档或旧 Java API 兼容声明。

## 验证与运行缺口

源码边界检查新增：业务接口不得暴露生命周期/工厂、Runtime 查询与捕获不得推进、定义发布不得位于 damage、展示必须按 target 分组且先扣预算、服务端检查必须先于取 Runtime、客户端回复不得自行采用 generation。

运行验收脚本已更新，尚未运行：

| 脚本 | 场景 |
| --- | --- |
| autojs/verify-runtime-query-boundary.js | 同 tick 查询/推进、业务门面无生命周期方法、关闭后的服务引用 |
| autojs/verify-effect-receipts.js | 补偿失败/每 tick 重试、查询无补偿、不同实体隔离、预算拒绝、清理和过期、时钟倒退 |
| autojs/verify-kernel-and-resources.js | 护盾 target 隔离、来源替换、预算边界和移除 |
| autojs/verify-generation-invalidation.js | 客户端受控注入：重载前在途回复、旧通知、换世界、断线及重新接受较低代际 |
| autojs/verify-settings-snapshot.js、verify-definition-retention.js | 捕获代际保留、旧代拒绝、替换代际释放 |
| verify-settings-runtime.py | 真实成功/失败资源重载，额外断言客户端展示 generation 与采用代际一致 |
| verify-presentation-runtime.py、verify-architecture-client.py | 真实面板传输、预算拒绝、换世界/死亡/重生；需另行启动隔离客户端与服务器 |

客户端受控注入通过反射访问私有状态，仅为测试路径；语法通过不证明反射调用或网络事件运行正确，也不替代真实重载/断线重连验证。尚需实际验证 Level Load/Pre tick 时序、失败补偿、多人状态、网络队列顺序、真实重连和所有修改后的脚本。没有进行性能测量，不宣称实测性能提升；源码证明的是去除重复推进及按目标状态限定展示遍历范围。

实际检查结果记录于本文末尾，不引用历史运行结果作为本次证据。

2026-11-05 最终源码检查结果：

| 检查 | 实际结果 |
| --- | --- |
| 活动 target 目录 `gradlew clean build` | 通过，7 个任务执行；未运行游戏 |
| 根 wrapper `gradlew -p common clean build` | 通过，5 个任务执行；common jar 跳过 |
| `python scripts/verify-common-boundary.py` | 通过，802 个 common class；生命周期、定义归属和 target 隔离检查通过 |
| `python scripts/verify-baseline.py` | 通过，625 个活动源码类及 metadata、Mixin、资源、许可证检查 |
| JavaScript `node --check` / Python AST 解析 | 29 个 AutoJS 文件、10 个 Python 文件通过语法检查；Node 24.14.0 |
| 产物字节码工具链 | CombatRuntime class major 69，确认 Java 25；终端默认 `java` 为 21，不作为编译工具链证据 |
| `git diff --check` | 通过 |
| AutoJS、客户端网络、Mixin/事件运行、性能测量 | 未执行，待验收 |

构建仍提示现有展示 payload 使用 deprecated API 及 Gradle 10 不兼容弃用项；不影响本次构建通过。`test NO-SOURCE` 不计入行为验证。
