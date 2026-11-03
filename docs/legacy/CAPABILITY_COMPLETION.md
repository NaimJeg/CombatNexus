# 能力补齐与独立环境验收

2026-11-06。仅 NeoForge 26.1.2.126、Minecraft 26.1.2、Java 25。用户已将其他 MOD 联合兼容移出本轮范围；1.21.1 骨架、Fabric、Forge 不在支持声明内。

交付范围更新：用户最终要求仅保证本轮源码修改完成，不再继续构建或测试。下方运行记录对应本轮验收时的代码快照；其后其他会话仍在修改诊断与日志代码，因此这些记录不代表包含后续并行改动的最终工作树已通过联合验收。本轮保留并行改动，不介入该重构。

## 实现与行为变化

| 能力 | 当前合约 |
| --- | --- |
| 闪避 | `dodge_chance` 默认 0，范围 0..1。仅 PRIMARY 近战、投射物或具有显式 action 的请求，在公共计算回调和资源应用前按目标判断一次；0/1 不抽随机数。规避时取消原生 Incoming，并封闭该操作的因果派生。其他目标及已完成兄弟操作不回滚；普通环境伤害、独立 DoT 不参与。 |
| 易伤 | `vulnerable_damage_additive` 默认由 0.20 改为 0；作为受击方加法比例进入公共 `vulnerable_damage` 倍率桶。不是固定的 debuff 推断，也不另写生命。 |
| 穿甲 | 新增 `armor_penetration_ratio`（0..1）和 `armor_penetration_flat`（0..1,000,000），默认均 0。移除量为 `max(armor * ratio, flat)`，有效护甲最低 0；随后应用既有模组护甲有效倍率与护甲公式。原生护甲归属路径在 `CombatRules` 调用处替换护甲输入，不穿透韧性或抗性。 |
| 固有减伤 | FULL_REPLACEMENT 及 CONFIGURABLE 对应开关启用时，在原生阶段等价计算女巫自身免疫及特定伤害类型的 85% 减伤；COOPERATIVE 保留原生计算。取消此前“抑制即禁用”的行为，不叠加两份减伤。 |
| 通道护盾 | `ShieldDefinition.channels` 与原生伤害类型/标签筛选共同匹配。实际 Pre 总量按已计算分量比例分配；最终覆盖仍保留比例，零分量基数归 `combatnexus:untyped`。来源池按优先级分配，各池仅吸收匹配通道；回执含 `absorbedChannels`。空通道集合不限通道。 |
| 动作控制 | Effect 的 `restrictions` 支持八类动作，多个来源取并集；解除一个来源不解除其他来源的限制。服务端决定，客户端仅接收 mask 并拦截本地输入。 |
| 持久化 | 默认 SESSION；显式 `CombatRuntime.sourcePersistence(target, source, PERSISTENT)` 选择跨退出/卸载恢复。装备成功安装账本自动保存；恢复时核对原生装备、绑定及当前定义。实体原生 `NeoForgeData` 中存储版本 1 的 `combatnexus:combat_state`，不是单独玩家文件。 |

### 控制入口

WALK 限制 travel 输入、停止生物导航，并拦截玩家移动包；JUMP 限制原生起跳及玩家上移输入；TURN 限制身体/头部旋转、鼠标输入和玩家旋转包；ATTACK 限制玩家/生物近战及新投射物入世界；USE_ITEM 限制开始、持续使用和服务端使用包。CAST 接入原版灾厄村民施法 goal；SKILL_MOVEMENT 接入末影实体、珍珠和物品消耗传送事件；EXTERNAL_MOVEMENT 接入击退、push、活塞/潜影盒位移及流体推力。重力、管理员位置写入、命令传送和维度管理不经技能位移门禁。

外部施法/冲刺等自定义入口应在写入前调用 `CombatActionGate.allows(entity, action)`；不能假定框架能辨别任意 `setPos/setDeltaMovement` 的业务意图。已飞行投射物不因发射者后来受控而撤销。世界生成时尚未入世界的实体构造不读取战斗 Runtime。

### 保存与恢复

保存剩余有效期、护盾余额和恢复延迟、恢复积累及合格 tick 数、周期治疗剩余间隔、保护消耗状态、SOURCE 触发计数及装备账本。离线暂停这些时钟，不补发离线治疗。其他触发范围的计数随原有动作/操作/tick 生命周期结束，不序列化事务权限。

恢复创建新的安装身份，将溢出盾来源重新绑定到新安装；撤销恢复后的 Effect 仍会移除对应溢出盾。一次性保护及 SOURCE 额度不会因重新登录或装备重新发现而补满。到期资源丢弃，死亡/重生清除。存储拒绝未知版本、跨目标内容、超容量和非法进度；恢复失败记录错误并清理该目标，避免残留部分安装。JSON 存储不包含 Runtime、实体引用、回调或 `CombatParent`。

此存储不是旧格式迁移器，也不提供进程崩溃后的事务 exactly-once。旧数据包的护盾 JSON 需要显式新增 `channels`；客户端与服务端应使用同一构建，不宣称旧存档或旧协议兼容。

## 原生时钟时序修复

真实客户端换维度/死亡回归暴露了原有 LevelTick.Pre 早于 gameTime 递增的问题：自然实体治疗可能因 Runtime 仍处于旧 tick 而失败。现在在 ServerLevel.tickTime 更新时间后推进快照过期，再在原生 block/entity tick 前执行恢复、装备协调和展示同步；不在查询中偷偷推进时间。新增自然 tick 观察器连续 20 tick 调用原生治疗，测试本身不调用 beginTick，避免脚本掩盖时序问题。

## 运行验收

环境为 Windows、JDK 25.0.3、NeoForge 26.1.2.126、AutoJS 1.0.1、Rhino 2101.2.8-build.91；独立运行配置排除神化及其前置。沿用固定依赖来源，见 [开发约定](DEVELOPMENT.md)。源码基线为 `5b4bf56509541554b4ba5d792d17565102629b95`，本次修改未提交 Git。

| 检查 | 实际通过范围 |
| --- | --- |
| 活动 target `gradlew clean build` | 成功；684 个活动源码类的打包基线检查通过 |
| common 独立 `clean build` | 成功；不生成发布 jar，864 个 class 的平台依赖边界检查通过 |
| `verify-capabilities-runtime.py --start --restart --reload` | 1,335 项断言；23 份服务端脚本、20 次自然世界 tick、10 项完整代际重载检查、4 项真实重启检查 |
| 新增 `verify-capabilities.js` | 60 项：公式边界、真实伤害、三种归属模式、女巫减伤、通道盾、控制并集、到期存储、保护消耗、溢出盾重绑及 SOURCE 额度 |
| `verify-support-policy.js` | 三种模式 × 16 组开关，795 项断言，包含原生护甲/保护/抗性真实生命损失 |
| `verify-native-integration-runtime.py --player --reload` | 原生伤害、嵌套治疗、失败回执、真实玩家治疗，资源重载前后执行 |
| `verify-architecture-client.py` | 真实连接、C 键打开面板、服务端快照显示，共 3 项 |
| `verify-control-client.py` | 八类 mask 到达、输入/转向拦截、服务端移动包限制及解除，共 7 项 |
| `verify-presentation-runtime.py` | 预算拒绝、恢复显示、旧回复过滤、真实换维度、死亡重生，共 5 项 |
| 语法与静态检查 | 全部 AutoJS `node --check`、Python AST、`verify-common-boundary.py`、`verify-baseline.py`、`git diff --check` |

重载负例包括未知字段、失效行为引用、失效护盾标签/通道、重复动作限制及非法周期；全部保留旧代际。真实重启同时验证显式来源和原生装备账本，验证已消耗保护不会补发。

证据写入 `targets/neoforge-26.1.2/build/verification/isolated/`，包括每份脚本结果、完整服务器日志和工作树 SHA-256；构建输出不入库。CI 运行相同独立服务端命令并上传证据。客户端验收需要桌面客户端，未将服务端 CI 当作客户端覆盖。

## 性能基线

`measure-capability-runtime.py` 在基线 checkout 与本次工作树使用相同 `measure-capability-load.js` 和独立启动配置。每档 20/100/500 个存活目标，每个目标一次主攻击，三轮预热后取五轮测量；每轮总伤害严格等于目标数 × 2。实体创建不计入请求耗时，Rhino、请求构建和同步派发计入；JFR 分配样本排除第一条跨录制权重，样本不足记 null。

这些是单个服务器任务内的突发微基准，不是 500 实体持续 AI 战斗的 TPS 保证。小样本延迟受 JIT 预热影响，不能把单次表格差异解释为确定的性能优化。原始分位数和采样数据见 `performance-baseline.json`、`performance-current.json`。

| 目标数 | 基线每请求 p95 中位数 | 本次每请求 p95 中位数 | 基线 / 本次每请求 JFR 采样分配 |
| --- | --- | --- | --- |
| 20 | 0.877 ms | 1.141 ms | 131,375 / 91,177 bytes |
| 100 | 0.327 ms | 0.381 ms | 133,846 / 113,396 bytes |
| 500 | 0.078 ms | 0.131 ms | 130,181 / 126,586 bytes |

500 目标档 p95 相比基线约增加 68.4%；本轮未设性能通过阈值，保留该开销记录，不声称性能无回退。分配量属于采样估计，尤其 20 目标档不能用于精确比较。

## 复现

```powershell
python scripts/verify-capabilities-runtime.py --syntax-only
python scripts/verify-capabilities-runtime.py --start --restart --reload
# 在隔离客户端连接 25566 后：
python scripts/verify-control-client.py
python scripts/verify-presentation-runtime.py
```

客户端使用 `scripts/autojs/client.init.gradle`，服务端使用 `server.init.gradle`；AutoJS 默认端口分别为 25591/25590。仅在一次性验证世界执行这些脚本。其他 MOD 联合兼容暂缓，未移植 target 仍禁止发布。
