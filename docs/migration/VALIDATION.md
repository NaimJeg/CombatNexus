# 开发基线验证记录

日期：2026-11-03。环境：Windows / JDK 25.0.3，Minecraft 26.1.2。

| 验证 | 结果 |
| --- | --- |
| 根目录 `gradlew build` | 通过；默认调用 NeoForge 26.1.2 自己的 wrapper |
| NeoForge `clean build`，场地修正后 `build` | 通过；398 项 JUnit 测试，0 失败/错误/跳过 |
| `gradlew -p common clean build` / `test` | 通过；398 项 JUnit 测试，0 失败/错误/跳过；无独立发布 jar |
| NeoForge `runGameTestServer` | 43/43 通过，其中 42 项 CombatNexus、1 项 Minecraft 基础测试 |
| NeoForge `runClient` | 启动成功；日志记录 Dev 进入单人世界、资源加载与正常保存退出，Gradle 成功退出 |
| `python scripts/verify-baseline.py` | 通过；482 个迁移类、精确 MC metadata、Mixin、语言、许可证与旧命名空间扫描 |
| `scripts/discover-targets.ps1` | 仅发现启用的 NeoForge 26.1.2 / JDK 25 |
| 原源码校验 | 迁移清单中 482 项 SHA-256 全部与原副本一致；92 个测试 Java 文件完整迁入 |
| `git diff --check` | 通过 |

## 迁移时修正的构建/测试问题

- 原模板测试类路径不包含 Minecraft/NeoForge，按原工程恢复 test compile/runtime classpath。
- `common` 独立测试补齐同一 target 的 Mixin 和展开后的 metadata 资源，避免只在 target 下通过。
- mod ID 从入口常量改为公共身份常量，相关测试改为验证实际常量值，仍要求 `combatnexus`。
- 源码契约测试从实际编译输入构造工作目录，不依赖旧项目布局或原始副本。
- 原 GameTest 使用 1×1×1 的 `minecraft:empty`，却在范围外生成实体和方块。首次运行出现展示怪物 `/kill` 清除、假人恢复生命的不同失败；检查 26.1.2 源码确认结构包围盒决定强制加载/tick 范围。改为 16×8×16 空测试场地后全部通过，未更改伤害和生命恢复实现或放宽断言。
- target 发现脚本默认路径在 Windows PowerShell 的参数初始化阶段无法可靠读取 `$PSScriptRoot`，改为在脚本主体解析。

## 验证边界

客户端依据启动/进入世界/退出日志验收，未逐项人工检查伤害界面、提示词和所有交互。Fabric 和 Forge target 已移除；NeoForge 旧版本功能移植、新 RPG 属性、Effect 统一上下文和玩家属性详情面板尚未实现。

运行产物和日志位于各 target 的 `build/`、`run/` 及本地 `*-baseline.log`，均不提交。CI 对主 target 执行构建、JUnit 与 GameTest，并保留报告；验证过程未执行模组发布。
