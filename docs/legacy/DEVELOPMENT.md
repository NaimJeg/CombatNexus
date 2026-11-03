> 历史记录：正文保留对应阶段的实现、命令和结果，不代表当前支持或通过范围。当前入口见 [开发约定](../DEVELOPMENT.md)、[领域定义](../DOMAIN_DEFINITIONS.md) 与 [现行审查](../VANILLA_COMBAT_REPAIR.md)。

# CombatNexus 开发入口

遵循根目录 [AGENTS.md](../AGENTS.md)。领域边界、事务、固定依赖、诊断和构建约定见 [既有开发基线](legacy/DEVELOPMENT.md)；历史验收记录只说明其对应版本和场景。

## 支持范围

| Target | 状态 |
|---|---|
| NeoForge 26.1.2 / Java 25 | 唯一活动实现与验证目标 |
| NeoForge 1.21.1 | 未启用骨架，不发布 |
| Fabric、Forge | 不在维护范围 |

统一 Effect 仅支持新世界，旧存档不兼容。不导入旧原版效果和燃烧字段；托管状态使用自己的保存、恢复与周期相位协议。详细模型、公共 API、40 种原版映射、15 类环境状态及展示接管见 [统一 Effect](EFFECTS.md)。

`common` 保存业务模型、事务、叠加、条件绑定、行为调度、结算限制和显示定义；target 保存当前版本输入采集、原生能力调用、Mixin 与网络接线。定义引用注册 ID；不能在核心服务中增加具体 Effect ID 分支。`common` 独立构建不包含 target 源码或资源，不生成发布 Jar。

## 构建与运行检查

活动 target 目录执行 `gradlew clean build`。根目录再执行：

```text
gradlew -p common clean build
python scripts/verify-common-boundary.py
python scripts/verify-baseline.py
python scripts/verify-capabilities-runtime.py --syntax-only
python scripts/verify-capabilities-runtime.py --suite effect-foundation --start --restart --port 25595 --game-port 25570
```

验证环境为 Windows、Java 25.0.3、Minecraft 26.1.2、NeoForge 26.1.2.126、AutoJS 1.0.1、Rhino 2101.2.8-build.91。不新增 JUnit/GameTest。生命周期套件在独立临时 source set 加载公共扩展示例；它不使用已移除的 `examples/sdk-extension`，也不进入发布 Jar。

客户端使用 `scripts/autojs/client.init.gradle` 启动；服务端使用 `server.init.gradle`，为一次性运行目录配置 EULA、仅绑定回环的离线模式和独立端口。运行 `scripts/verify-effect-display.py` 验证真实网络及原版 tooltip 事件。具体命令和报告位置见 [运行验证](EFFECTS.md#运行验证)。

服务端逐项证据包含生命周期、剩余时长取模、刷新与隐藏层恢复、来源独立性、原版属性安装、环境进入/退出/免疫、伤害生命底线、随机世界事件次数、扩展、失败补偿和真实重启。客户端逐项证据记录原版宽窄提示、双语、来源、永久/时限、重连、换维度、死亡重生及移除缓存。构建成功不能替代这些运行结果，也不能将隔离运行结果外推为未运行的兼容组合。

2026-11-07：活动 target/common clean build、边界、发布包与脚本语法检查通过；隔离服务端 417 项、真实客户端 59 项通过，明细见 [实际结果](EFFECTS.md#2026-11-07-实际结果)。

同日环境周期增量：六类持续环境改由通用 Effect 每 10 tick 调度，独立于共享受伤冷却，仍保留伤害免疫与事件取消。重新完成构建及边界检查，服务端 428 项（本次不含重启）、客户端 63 项通过，详见 [环境周期接管增量](EFFECTS.md#2026-11-07-环境周期接管增量)。

统一描述与展示 API、网络迁移和实例投影约定见 [统一战斗描述](ITEM_TOOLTIPS.md)。通用展示模型归 presentation；伤害条件与操作描述适配归 combat.damage。

### 统一描述四阶段实际结果（2026-11-07）

共享短语/类型化文本、启动期 Effect 描述器、共同文档/展示策略、服务端定义与物品实例投影已接入。旧客户端描述器注册事件、重复规则渲染器和客户端物品数值生成路径删除；公开 API 迁移及网络版本要求见 [统一战斗描述](ITEM_TOOLTIPS.md)。

Windows / Java 25.0.3 / Minecraft 26.1.2 / NeoForge 26.1.2.126 / AutoJS 1.0.1 / Rhino 2101.2.8-build.91。活动 target `gradlew clean build`、common 独立 `clean build`、`verify-common-boundary.py`、`verify-baseline.py`、AutoJS/Python 语法及 `git diff --check` 通过。未新增 JUnit/GameTest，未修改 Mixin require。

实际运行共 781 项：Effect foundation 与真实重启 442，物品提示 en_us/zh_cn 各 36，携带效果双语 199，活动 Effect 63，presentation 5。包括采用词缀值 7/19 及具名摘要绑定、嵌套文本与百分号、AND/OR/NOT、有序操作、来源/隐藏层恢复、不同属性单位、未知描述器、类型不匹配、注册冻结、缺失翻译、无效参数、真实预算拒绝、缓存到期、重载、脱离槽位副本、重连、维度转移、死亡重生和其他模组提示行保留。

运行命令为 `verify-capabilities-runtime.py --suite effect-foundation --start --restart --port 25595 --game-port 25570` 与 `verify-carried-effect-tooltips.py`；后者依次调用双语物品、活动 Effect 及 presentation 脚本。汇总为 `build/verification/unified-description-summary.json`，逐项证据链接在汇总内。所有临时实例已由运行脚本停止。未进行人工鼠标悬停视觉验收，也未执行可选神化组合；通过范围不外推至这些场景。

实机反馈后的单机槽位映射修复、9 项集成服务器回归及此前验收缺口见 [单机实际物品不可用修复](ITEM_TOOLTIPS.md#单机实际物品不可用修复2026-11-07)。

创造目录项（无服务端槽位）的后续修复及 16 项实机回归见 [创造目录预览补齐](ITEM_TOOLTIPS.md#创造目录预览补齐2026-11-07)。


物品 tooltip 已按用户要求改回直接读取当前 ItemStack，删除服务端槽位投影、创造目录预览请求及实例缓存。背包、创造目录、箱子、JEI 共用本地读取路径；模板/装备绑定 ID 仍读取既有定义缓存，活动 Effect 与面板仍用服务端快照。当前实现和验证范围见 [物品本地读取与 JEI](ITEM_TOOLTIPS.md#物品本地读取与-jei2026-11-07)，此前服务端实例投影记录为历史结果。

本轮狼铠、守卫者、Effect 事件快照及原版战斗物品说明增量见 [原版战斗修复](VANILLA_COMBAT_REPAIR.md)。该记录区分各阶段结果，并列出当前公开 API、来源、mob 与展示矩阵及验收边界。
