# CombatNexus 长期开发基线

## 当前范围

2026-11-03 基线将 DamageNexus 26.1.2 迁为 CombatNexus 的 `damage` 模块，保留伤害管线、规则、词条、结算、网络、调试假人、客户端伤害展示和既有测试。此阶段不改变数值平衡，不宣称实现完整 RPG 属性系统或玩家详细属性面板。

Java 根包 `io.github.naimjeg.combatnexus`，模组 ID `combatnexus`，开发版本 `0.1.0-SNAPSHOT`。MIT 许可证保留原作者与模板作者署名。

## 模块方向与依赖

| 模块 | 责任 | 基线状态 |
| --- | --- | --- |
| `damage` | 伤害请求、通道、规则、暴击决策、减伤、结算与来源追踪 | 原实现已迁入；NeoForge 26.1.2 适配 |
| `attribute` | 统一属性定义、单位、上下限、原版属性映射、修饰项与只读快照 | 下一阶段，不预注册空属性 |
| `formula` | 确定性公式、顺序、封顶、计算明细 | 下一阶段；先保持伤害模块既有公式 |
| `effect` | 将原版/扩展 Effect 转成有来源和生命周期的上下文贡献 | 下一阶段 |
| `presentation` | 玩家详细属性面板、来源解释、服务端快照显示 | 下一阶段；现有假人/伤害界面仍在 damage 内 |

模块间只依赖公开合约，不能从新增模块导入 `damage.core`、`damage.internal` 或 `damage.registry`。公开 API 不暴露内部实现类型。未来 damage 消费 attribute/formula 的快照；attribute 和 effect 不反向调用伤害内部流水线。现有核心与注册器耦合是迁移债务，逐条抽取 SPI，不能把本次包迁移描述成已经平台无关。

## 目录与平台边界

- `common/src/main/java/.../damage/`：403 个无已证实平台差异的原始业务文件。可引用 Minecraft 类型。
- `targets/neoforge-26.1.2/src/main/java/.../damage/`：79 个实际依赖 NeoForge 事件、注册、附件、配置、网络 API 的文件，以及绑定 26.1.2 方法的 Mixin。具体映射和原始 SHA-256 见迁移清单。
- `common/src/main/resources/`：共享 assets/data；加载器 metadata 和 Mixin 配置归 target。
- `targets/neoforge-26.1.2/src/test/`：原始测试套件，当前合约依赖 NeoForge 参考实现。
- `gradle/target-conventions/`：发布与测试布局约定。

`common` 的独立构建使用 NeoForge 26.1.2 类路径，引用同一份 target 适配源码用于 IDE/测试解析，不复制同名类，不生成公共发布 jar。各 target 用 `prepareCommonSources` 同步源码后直接编译。当前仅维护 NeoForge target；1.21.1 骨架排除整个 damage 模块及其资源，尚未完成移植。Fabric 和 Forge target 已移除，暂不纳入开发路线。

## 属性与公式约定

新属性先确定 ID、单位、默认值、范围、同步方式、消费阶段、来源标识和显示规则，再写注册代码。所有计算值必须有限；拒绝 NaN/Infinity。概率使用 `[0,1]`，显示时才转百分数；加成与最终倍率不能混用。修饰项必须有稳定来源 ID，支持替换、撤销、重算，不能每 tick 无界叠加。

| 能力 | 约定/迁移方向 |
| --- | --- |
| 攻击速度 | 复用 `minecraft:attack_speed`，单位次/秒；由游戏适配层使用，不塞入伤害倍率 |
| 移动速度 | 复用 `minecraft:movement_speed`，明确原版属性单位与面板换算，不重复注册同义属性 |
| 暴击率 | 现有 `combatnexus:crit_chance`；默认判定、强制/禁止暴击贡献及优先级维持原语义 |
| 暴击伤害 | 现有 `combatnexus:crit_damage_additive` 是加法增量，不是总倍率；面板区分原版和额外贡献 |
| 吸血 | 待实现；基于已确认的有效结算伤害，明确吸收/过量击杀、子请求、递归与治疗归属策略 |
| 护甲穿透 | 待实现；平穿与比例穿透独立定义，明确与护甲/韧性/抗性、真伤的先后关系与上下限 |
| 生命恢复 | 待实现；区分每秒恢复量和 tick 调度，实际治疗仍由服务端执行 |

伤害现有阶段、随机暴击与结算逻辑作为回归基准。统一公式改造须先写 golden cases，覆盖零值、负值、封顶、多来源、伤害转换、子请求及真伤，不能在重命名提交中偷偷改变结算顺序。

## Effect 上下文与面板

统一快照应表达：实体、计算时点/修订号、有效属性值、修饰项来源、Effect ID/等级/持续时间、条件与计算明细。持久属性与一次攻击上下文分开；面板常驻值不得混入一次性武器捕获结果。

Effect 适配先核对 26.1.2 源码，然后选择唯一执行路径：迅捷接入原版移动速度修饰项；挖掘疲劳接入挖掘速率上下文；生命恢复接入周期治疗上下文。必须处理添加、升级、刷新、移除、过期、死亡/重生、换维度和存档加载。替换原版逻辑时明确停止旧贡献，不能两边同时生效；未替换前应准确展示原版贡献。

属性面板目标类似 Diablo 4 详细属性或 LOL 按住 C：展示基础值、最终值、来源贡献、单位、条件与说明。按键可配置，客户端只显示服务端认可的快照；不得允许客户端提交属性值、暴击结果或治疗数值。同步需限制目标、频率、数据量并清除过期状态。此面板尚未实现。

## 下一阶段验收顺序

1. 建立 attribute 定义与快照合约，统一原版攻击/移动速度和现有暴击属性；测试单位、范围与修饰项撤销。
2. 抽取纯 formula 计算与来源明细，保持原伤害测试全部通过。
3. 接入迅捷、挖掘疲劳、生命恢复，验证原版贡献仅生效一次及完整生命周期。
4. 接入吸血与穿透，验证结算顺序、服务器权威和递归边界。
5. 提供只读同步与玩家属性面板，验证专用服务端不加载客户端类。
6. 后续若开展 NeoForge 旧版本适配，按真实 API 差异实现，完成相同语义测试和运行验证后才能启用 CI 发布。其他加载器需另行确定开发范围。

## 开发与发布

采用 `common/<topic>`、`target/<loader-version>/<topic>`、`build/<topic>` 分支；提交说明写清行为、平台范围与验证。修改公开合约时说明兼容性；行为改动与机械迁移分开。

在仓库根使用 JDK 25 执行 `./gradlew build`，默认构建 NeoForge 26.1.2；`-PallTargets=true` 只构建 `ci.enabled=true` 的 target。在目标目录执行 `./gradlew clean build`，不要用其他 target 的 wrapper 替代。`./gradlew -p common test` 验证参考类路径和迁移测试。`runGameTestServer` 验证服务端真实 Mixin、事件和伤害行为；`runClient` 用于人工界面验收。

原测试中的源码/资源契约由 `prepareDamageContracts` 在 build 下汇集真实 source-set 输入和编译字节码，再运行断言。任务使用 Sync 清除过期文件并拒绝重复路径；不读取原项目，不放宽原断言。

GameTest 使用 `combatnexus:test_arena`（16×8×16 空结构），其可复现生成器为 `scripts/generate-test-arena.py`。原版 `minecraft:empty` 仅 1×1×1，不能覆盖原测试在边界外创建的实体、方块及其 tick；场地修正保证测试隔离，未改变伤害算法。`scripts/verify-baseline.py`（Python 3.11+）检查发布 jar 内全部迁移类、metadata、Mixin、语言和许可证，并禁止残留旧命名空间。

发布前检查 jar 中的 metadata、命名空间、Mixin 与许可证。尚未移植 target 的 publish 任务会明确失败。Maven 发布目标通过 `COMBATNEXUS_MAVEN_URL` 设置，凭据只取环境变量；不沿用模板作者的服务器。不得在未验证功能的骨架上发布 CombatNexus 正式版。
