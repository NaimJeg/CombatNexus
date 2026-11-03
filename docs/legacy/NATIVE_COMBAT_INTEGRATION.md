# 原版与通用战斗语义接入

状态：2026-11-05 源码接入完成。本轮按用户要求只执行源码、构建和脚本语法检查，**没有启动游戏，运行验收待完成**。基线为 Minecraft 26.1.2、NeoForge 26.1.2.126、Java 25；历史 127 项断言不证明本轮通过。

## 伤害分类与归属

完整清单见 [native-damage-semantics.json](native-damage-semantics.json)：50 个原版类型及 neoforge:poison，逐项列出纳管、默认通道和攻击类别。默认数据下 out_of_world、generic_kill 绕过管线；无注册身份和显式 bypass 标签也不纳管。清单中绕过类型的通道仅表示标签查询结果。

| 语义 | 当前规则 |
| --- | --- |
| 通道 | priority 降序，同优先级按通道 ID 升序；取首个命中，未知类型为 untyped |
| 类别 | 近战、投射物、爆炸、环境可以重叠，不等同于唯一元素通道 |
| 原版身份 | 攻击、暴击和特殊武器按注册类型及捕获证据识别，不再以同名 msgId 或 ID 子串冒充原版攻击 |
| 投射物 | 来源画像支持原生/CombatNexus 标签或实际 Projectile；通道仍由标签决定 |
| 原生归属 | directEntity 保留直接实体；原生 source entity 是生物时作为 logicalAttacker/equipmentOwner；effectOwner 保留有效的原生 source entity，包括非生物 |
| 无效角色 | 已移除、未加入世界或跨世界的角色分别清空；不猜测驯服者、召唤者或附近玩家装备 |
| 扩展归属 | 复用 registerAttributionResolver 的注册冻结、优先级/ID 排序、冲突诊断及身份校验 |
| 攻击重建 | 无匹配捕获时保留传入伤害，不推测力量/虚弱份额；矛重建另需同次附魔捕获绑定 source/target |
| 矛动作 | minecraft:spear 不再同时标记为刺击和冲刺；具体动作由捕获证据区分，未命中/异常退出时清理捕获 |

本轮接入 neoforge:is_physical、is_environment、is_wither，并补齐吐沫投射物、蜂刺近战。因此仙人掌、挤压、撞墙、甜浆果丛、蜂刺、音爆、墙内伤害及吐沫等开始消费物理通道抗性，属于数值变化。归入 physical 不撤销原生护甲 bypass。荆棘仍按优先级进入 magic；溺水、饥饿、干涸、世界边界明确保持 untyped。

开启现有 COMPATIBILITY 诊断后，零命中或多通道命中记录类型 ID、选中通道和命中数，使用有界限频器；关闭诊断时仍首命中返回，不保存活实体。

第三方优先使用原版/NeoForge 标签，或扩展 CombatNexus 的 damage_type 标签。自定义元素复用 combatnexus_channels 和规则。标签只改变分类，不授权代理读取其他实体装备，也不自动拆分模组已计算的伤害。

## 原版 heal 与来源 API

普通服务端 LivingEntity.heal(float) 进入独立治疗服务：

捕获输入 → 现有 RECEIVED/CONTRIBUTIONS 阶段 → 原生治疗事件一次 → 事件后采样 → setHealth 一次 → journal、结算和已安装的溢出转盾。

端口直接写 setHealth，不回调 heal；拒绝、取消或异常不重试、不再次执行原方法。直接 setHealth 不生成治疗事务。原版食物/效果计时器及触发条件保留，没有第二套恢复计时。正数满血治疗可产生溢出；普通入口的非有限、零或负数直接拒绝，避免固定增量把无效输入变成收益。死亡实体不复活，事件后死亡、移除、换世界时不再写血。

| 原生调用点 | reason / source ID / owner |
| --- | --- |
| FoodData.tick | NATURAL_REGENERATION / minecraft:food_regeneration / 玩家本人 |
| ServerPlayer.tickRegeneration | NATURAL_REGENERATION / minecraft:peaceful_regeneration / 玩家本人 |
| 再生效果 tick | EFFECT / minecraft:regeneration / 未知，不从当前效果猜施加者 |
| 瞬间治疗或伤害反转治疗 | EFFECT / minecraft:instant_health 或 instant_damage / 瞬时调用实际传入且有效的 owner，tick 路径未知 |
| 驯服动物、马、骆驼、羊驼喂食 | ITEM / 实际物品 ID / 喂食玩家 |
| 铁傀儡修复 | ITEM / minecraft:iron_ingot / 修复玩家 |
| 悦灵、快乐恶魂、马、凋零自愈 | NATURAL_REGENERATION / minecraft:allay_regeneration、happy_ghast_regeneration、horse_regeneration、wither_regeneration / 自身 |
| 凋零头颅击杀回血 | SKILL / minecraft:wither_skull_healing / 原生接受回血的发射者 |
| 无来源上下文的模组 heal | UNKNOWN_EXTERNAL / combatnexus:unknown_external_healing / 空 |

新增 NativeHealingSource、NativeHealingSources.open(target, source)。作用域只提供来源，不授予事务权限；绑定当前线程与目标，只消费一次。最内层目标不匹配时返回 UNKNOWN，不穿透外层；close 恢复外层，必须在异常路径同样清理。

```java
try (var ignored = NativeHealingSources.open(target,
        new NativeHealingSource(source, HealingRequest.Reason.ITEM))) {
    target.heal(amount);
}
```

第三方也可直接使用既有 HealingRequest；没有改变该请求或 CombatSource 字段结构，没有新增网络协议。common 的 NativeHealingAccess 提供原生路由与提交，嵌套身份由事务内核继承。target 的独立治疗包与 combatnexus.healing.mixins.json 负责原生接线，Mixin require 至少为 1，双调用点为 2。

## 已确定的例外

- APPLICATION 原生回调及有写权限的结算通知中的嵌套 heal 使用独立受管事务，继承父链与共享预算，完整应用倍率和回执；外层在事件后采样，不把事件内嵌套回血计入自身恢复。执行与失败语义见[同步嵌套事务](SYNCHRONOUS_NESTED_TRANSACTIONS.md)。
- COMPUTE 和其他禁止写入的可变阶段抛错，不通过原版回退绕过守卫；不引入子事务或延迟队列。
- 海底神殿生成守卫者有一次初始化 heal。未加入世界且未移除的实体没有可查询 UUID，因此保留原版初始化，不创建 Runtime、不推进 tick；已移除实体不使用此例外。
- 客户端沿用原方法；服务端异步调用拒绝。已加入世界但缺失有效 Runtime 不静默回退。第三方重写 heal 且完全不调用父方法不在接管范围。
- invalid_amount、vanilla_initialization、rejected_phase 路由提供 DEBUG 限频诊断，不保存活实体；已移除 vanilla_nested 绕过路径。

## 验证与复现

静态命令：

```text
python scripts/verify-native-semantics.py --minecraft-jar <sources+classes.jar> --neoforge-jar <universal.jar>
```

输入使用 Gradle 解析的本版本依赖。工具校验版本、全部分类、标签引用和优先级，并用 JDK javap 对照 15 个含 heal 调用的方法及调用数量；枚举原版源码全部调用者，唯一未包装来源的调用者是上述世界生成初始化。这不证明 Mixin 已成功运行。

已准备、未运行的脚本：

- verify-native-damage-semantics.js：真实 catalog/tag 分类、同名 msgId 和暴击捕获、未知倍率、投射物身份、非生物来源、已移除及跨世界角色。
- verify-native-healing.js：倍率、来源、事件次数、取消/改量、伤害及治疗事件内嵌套、结算通知嵌套、死亡/移除、非法数值、来源隔离和异常清理、实际 Effect 调用、溢出转盾、COMPUTE 拒绝。它前后重建 overworld CombatRuntime，以便在首个结算前注册观察者，只能用于隔离世界。
- verify-native-player-healing.js：连接 SettingsVerifier 后，使用独立 FoodData 检查 10/80 tick 触发与一次倍率，保留玩家实际饥饿计时器。
- 继续使用 verify-native-healing-failure.js 检查真实写入后异常的部分证据；已有 Effect、生命周期、代际回归仍待重跑。

以后允许运行时，在已启动的隔离 AutoJS 服务器执行：

```text
python scripts/verify-native-integration-runtime.py --port 25589 --reload
```

可选 --player 要求 SettingsVerifier 在线。runner 不启动服务器，会等待 UUID 查询区块准备好、注入分类矩阵、执行脚本，并可重载后重复检查。原生事件重入、失败、重载、客户端、生产负载和第三方整合仍待实际验收。

### 本轮实际结果

环境：Windows、JDK 25.0.3、NeoForge 26.1.2.126、Node 24.14.0。

| 检查 | 实际结果 |
| --- | --- |
| 活动 target 目录 gradlew clean build | 通过，7 个任务执行；未启动游戏 |
| 根 wrapper 的 -p common clean build | 通过，5 个任务执行，common jar 跳过 |
| verify-common-boundary.py | 通过，808 个 common class，无 target/loader 反向依赖 |
| verify-baseline.py | 通过，645 个活动源码类、两个 Mixin 配置、metadata、资源与许可证 |
| verify-native-semantics.py | 通过，51 个伤害类型、15 个原生治疗调用方法及实际调用数量 |
| node --check | 3 个新增 AutoJS 脚本及 2 个既有 native 故障脚本通过语法检查 |
| Python AST | 两个新增工具和修改后的产物检查工具通过语法检查 |
| git diff --check、文档 UTF-8 检查 | 通过 |
| AutoJS 实际执行 | 0；本轮未启动服务器或客户端，未作性能测量 |

构建仍报告 deprecated API 和 Gradle 10 相关弃用提示。test 为 NO-SOURCE；上述构建与静态结果不计行为覆盖，所有新增运行断言仍待执行。
