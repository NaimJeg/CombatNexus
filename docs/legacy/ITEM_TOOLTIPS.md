> 历史记录：正文保留对应阶段的实现、命令和结果，不代表当前支持或通过范围。当前入口见 [开发约定](../DEVELOPMENT.md)、[领域定义](../DOMAIN_DEFINITIONS.md) 与 [现行审查](../VANILLA_COMBAT_REPAIR.md)。

# 统一战斗描述与展示

仅维护 NeoForge 26.1.2 / Java 25。描述迁移不修改战斗执行、词缀生成或堆叠规则。注册 ID 和资源命名空间不变；这是 Java API 和网络协议的破坏性迁移，不提供旧 API 门面，不承诺旧存档或跨版本网络兼容。

## 文档与职责

`presentation.api.DescriptionDocument` 是共同文档：标题、作者摘要、生成说明、补充详情、风味、子块、规则和独立调试内容。`DescriptionRule` 保存 AND/OR/NOT 条件树以及有序操作，每个文本具有 compact/detail 两种形式；线上不存在条件执行对象、行为对象或 registry holder。`TooltipBlock` 只用于简单文本叶子，由 `DescriptionDocument.leaf` 转换。

`DescriptionRenderer` 负责共同展示策略。作者摘要优先；没有摘要时显示生成说明。Shift 保留摘要并增加详情。`show_rule_breakdown=false` 同时隐藏子块及规则，调试内容独立于 Shift。affix 保持连续分组，entry 和规则保持执行排序。原版效果栏、物品事件和面板保留各自插入位置、折行及布局；其他模组的提示行不被重建。

`DamageAffixDisplay`、`DamageEntryDisplay` 的 `name`、`authored_summary`、`flavor_text`、`show_rule_breakdown` 语义不变。`EffectDisplay` 仍只定义显示身份和表现标志。

## 共享文本和参数

通用短语 schema、slot、参数、嵌套短语、factory/provider 位于 `presentation.api.phrase`。伤害条件、操作及 Channel/Role/RequestKind 等适配位于 `combat.damage.api.display.phrase`。语义规划器位于 `combat.damage.api.display.document` 和 `narrative`，不依赖 Minecraft 客户端。`TooltipNumbers`、`AttributeDescription` 提供共同数字和属性单位格式；比例固定增量使用百分点，倍率使用百分数。非零极小值不显示为零。时长参数以 tick 传输，格式化为秒。

`DisplayText` 接受字面文本或翻译键，支持原版大小写键名。字符串参数仍按字面解释；嵌套可翻译名称使用 `nested`，数值使用 `NUMBER`、`PERCENT`（存比例）、`DURATION`（存 tick）：

```json
{
  "translate": "entry.example.summary",
  "fallback": "%s: %s / %s / %s seconds",
  "args": [
    {"nested": {"translate": "effect.minecraft.speed"}},
    {"kind": "NUMBER", "value": 3.25},
    {"kind": "PERCENT", "value": 0.25},
    {"kind": "DURATION", "value": 40}
  ]
}
```

文本参数限 16 个、字符串限 256 Unicode 码点、嵌套文本限 8 层；拒绝非有限数值、负时长及布局控制字符。缺失翻译使用作者 fallback，否则显示明确的未本地化提示。

作者摘要可使用 `{"bind":"amount"}`。绑定只读取描述器显式提供的具名 slot，不访问对象字段、不执行表达式。伤害操作的唯一 slot 名可直接使用；多规则存在不同值时必须使用 `规则ID/操作序号/slot名`，例如 `example:hit/0/amount`。序号从零开始。绑定使用当前 ItemStack 已保存实例中的短语参数；缺失或歧义绑定拒绝本次投影。静态模板文档仅用于物品上的模板引用，不能替代物品上已经保存的实例数值。

## 启动注册与 Effect 描述器

通过现有 `CombatRegisterEvent` 的 `CombatRegistrar` 注册：

- `ruleDescriptions()` 注册规则短语 schema、条件与操作 provider；结束时冻结。
- `effectDescriptor(behaviorId, implementationClass, descriptor)` 将描述器绑定到已注册行为 ID 和具体 Java 类型；类型错误、重复注册和冻结后注册均拒绝。

`EffectBehaviorDescriptor<T>.describe(T, int level)` 返回简单说明叶子。未注册描述器的行为显示未知行为。内置行为说明位于 `BuiltinEffectBehaviorDescriptors`；生命周期、等级、来源、叠加和解除策略由 `EffectTooltip` 组合。定义说明、活动说明使用同一行为描述器，实例显式传入等级和剩余时长，不再按固定列表下标替换文本。

公开扩展示例：`scripts/autojs/effect-extension/java/probe/EffectExtension.java` 注册 Pulse 行为和对应说明；Effect 定义文档由服务端生成；物品中的伤害实例在客户端使用启动注册的规则短语描述器生成说明，扩展需在两端注册相同规则描述器。`RegisterRulePhrasesEvent`、旧客户端短语注册、旧 `DamageTooltipRenderer` / `RulePhraseRenderer` 已删除。`DisplayTextResolver` 移至 `presentation.client`，属性说明移至 `presentation.api.AttributeDescription`。

## 服务端投影与客户端缓存

网络注册版本为 `combatnexus-description-3`，客户端与服务端必须一致。`TooltipDefinitionSync` 登录/重载时发送模板和 Effect 的共同文档，不再发送 affix/entry 执行定义。

物品提示直接读取当前 `ItemStack` 的组件：已保存的词条/词缀实例、附魔等级、装备绑定、药水、食物和炖菜。客户端复用共同描述器与展示策略，不执行战斗行为。仅含 ID 的模板/装备绑定引用使用既有 `ClientTooltipDefinitions` 定义缓存，保护换算参数使用既有 `ClientFormulaSettings`。缺失定义、空投影或渲染失败时，不追加本模组说明或“不可用”提示，保留原版和其他模组的 tooltip。渲染先在副本完成，成功后才更新提示列表，失败不遗留部分新增行或折叠结果。

背包、箱子、创造目录、JEI 和脱离槽位副本走同一路径，不查找服务端槽位、不发物品请求、不等待回包，也没有两秒实例缓存。`CreativeDescriptionRequest` 和服务端 `ItemDescriptionProjection` 已移除，共用读取逻辑位于 `presentation.client.ItemDescriptionProjection`。`CombatPresentationRequest` 仅接受面板订阅；活动 Effect 和面板仍使用服务端快照。

`EffectTooltipPayload` 同样携带会话和 generation。活动文档保留实际来源及隐藏层来源，原版客户端实例只用于现有名称、等级和倒计时。断线、维度/玩家变更及重载清理缓存；错误或空文档撤销旧说明。

编码继续受 4096 项/24000 字符展示预算和载荷预算约束。物品说明超预算或投影失败时跳过新增 tooltip，不截断条件或展示旧实例结果。普通投影异常通过公共诊断记录。

药水、食物说明和概率在 Shift 中展示；迷之炖菜仍仅在高级提示显示。隐藏组件不被重新公开。原版属性段在折叠时按完整原版组件序列移除，展开保留原版属性行，避免重复收益或重复说明。附魔书保留原版 `STORED_ENCHANTMENTS` 标题，不生成装备作用说明。

## 验证

```text
cd targets/neoforge-26.1.2
gradlew clean build
cd ../..
gradlew -p common clean build
python scripts/verify-common-boundary.py
python scripts/verify-baseline.py
python scripts/verify-capabilities-runtime.py --syntax-only
python scripts/verify-capabilities-runtime.py --suite effect-foundation --start --restart --port 25595 --game-port 25570
python scripts/verify-carried-effect-tooltips.py
```

最后一个命令启动一次性服务端/客户端（AutoJS 25587/25588，游戏 25589，`build/carried-tooltip-run`），运行携带效果双语脚本，接着运行 `verify-item-tooltips.py` 双语、`verify-effect-display.py` 和 `verify-presentation-runtime.py`。真实槽位物品、临时数据包、重载和断线均由脚本控制，结束后停止本次实例。独立调用物品脚本需传对应端口及 `--run-directory`。

报告写入 `build/verification` 和 target 的 `build/verification/isolated`，不入库。不使用 JUnit/GameTest；实际结果另记于开发入口。运行通过范围不包含鼠标截图视觉验收或可选神化组合。

本次实际通过 781 项；各套件数量、环境及报告见 [开发入口的实际结果](DEVELOPMENT.md#统一描述四阶段实际结果2026-11-07)。

### 历史：单机实际物品不可用修复（2026-11-07）

创造模式背包使用客户端 `ItemPickerMenu`，其编号不是服务器 `InventoryMenu` 的编号。此前使用当前客户端菜单直接查找并发送槽位，导致实际持有的物品仍请求错误槽位、回包匹配失败。客户端现对创造模式选择菜单显式使用 `player.inventoryMenu`；生存背包和箱子继续使用服务器同步的当前菜单。请求、缓存匹配和回包核对采用同一个菜单选择，不放宽物品或代际匹配检查。

没有可展示战斗/效果组件的普通物品不请求投影，也不追加不可用提示；此判断只查看组件是否存在，不生成数值或执行说明。

新增 `python scripts/verify-item-tooltip-integrated.py`，使用此前携带效果测试世界的副本启动真正的单机集成服务器，不操作个人世界。脚本在服务器线程安装测试物品，验证实际 tooltip 事件：生存/创造背包、缓存到期重取、快捷栏、箱子内容、箱子中的玩家背包及无效果普通物品。运行前需由 `verify-carried-effect-tooltips.py` 生成一次性世界，或保留该脚本此前复制的 `build/verification/integrated-tooltip-seed`。

此次 target/common `clean build`、边界、发布包和脚本语法检查通过；单机回归 9 项通过，报告为 `build/verification/item-tooltip-integrated/report.json`。此前 781 项通过记录只覆盖当时的独立服务器等场景，没有覆盖创造模式客户端菜单映射，不能作为该入口正确的证据。

### 历史：创造目录预览补齐（2026-11-07）

连接用户提供的 AutoJS 25585，确认空背包的创造目录“夜视药水”提示仍为“部分效果说明暂不可用”。该入口没有服务端槽位；此前 9 项只测试了实际背包物品，未覆盖目录项。

增加独立 `CreativeDescriptionRequest`：仅创造玩家可请求，客户端仅为当前创造目录中的物品发出请求。服务端使用请求的副本生成只读说明，不安装物品、不生成词缀、不执行行为；模板引用仅解析服务端模板定义，预览明确标注“创造物品栏目录预览”。实际物品继续走服务端槽位投影。目录回包使用独立槽位标记，仍核对 nonce、session、维度、generation、物品及当前目录；离开目录后不得复用预览。请求限频与实例请求共用，输入/输出编码限制均为 65,536 字节，文档沿用既有预算。协议更新至 `combatnexus-description-3`。

`python scripts/verify-item-tooltip-integrated.py` 在独立临时客户端与集成服务器上通过 16 项：原有 9 项，加空背包目录食物、夜视药水、中英文说明、超预算请求、预览到期、离开目录与取入背包切换。该脚本从真实食品目录填充创造菜单并调用实际 tooltip 事件，不代表人工鼠标视觉验收。报告：`build/verification/item-tooltip-integrated/report.json`。环境仍为 Windows / Java 25 / NeoForge 26.1.2.126 / AutoJS 1.0.1。

默认 target `clean build` 因用户正在运行的客户端锁定 Minecraft jar 而未能删除 build 目录；随后 target `build`、common `clean build`、边界与发布包检查通过，并以临时 Gradle init 将 target/compat 输出重定向到 `build/verification/creative-clean` 完成 target `clean build`。未关闭用户的 25585 客户端；该进程须重启加载新代码与协议。


### 物品本地读取与 JEI（2026-11-07）

按用户要求删除上述历史槽位查询和创造预览方案，改为读取传入的 ItemStack。历史服务端投影验收不能代表当前本地路径；当前回归使用 `python scripts/verify-item-tooltip-integrated.py`，覆盖真实 tooltip 事件与 JEI `ItemStackRenderer.getTooltip`，报告位于 `build/verification/item-tooltip-integrated/report.json`。

活动 target/common `clean build`、边界和发布包检查通过。Windows / Java 25 / Minecraft 26.1.2 / NeoForge 26.1.2.126 / AutoJS 1.0.1，运行加载 JEI 29.53.0.115。真实客户端 18 项通过：生存/创造背包、快捷栏、空背包创造目录、中英文夜视药水、脱离槽位副本、箱子及玩家背包、JEI 渲染器的食物/药水、JEI 缺失模板回退（随后按用户要求改为不追加新 tooltip），以及普通物品。逐项结果见本次报告；未执行人工鼠标悬停视觉验收或完整 Effect 回归。


物品提示回退增量：缺失模板、装备/携带效果描述、空上下文或说明渲染失败，均保留原有提示，不注册额外提示行。`verify-item-tooltip-integrated.py` 增加空投影、错误投影、缺失段、损坏文档和空物品验证；本轮 target/common `clean build`、边界和发布包检查通过；上述 Windows / Java 25 / NeoForge 26.1.2.126 / AutoJS 1.0.1 环境实际客户端 25 项通过，包含 JEI 缺失模板仅保留原提示及七项空值/失败回退。报告仍为 `build/verification/item-tooltip-integrated/report.json`。
