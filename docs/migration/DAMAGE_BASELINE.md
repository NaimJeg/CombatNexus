# DamageNexus → CombatNexus 迁移说明

原始输入：`docs/DamageNexus-main`，Minecraft 26.1.2 / NeoForge 26.1.2.87 / Java 25 / ModDevGradle 2.0.141。原始副本保持不变，不参与构建。

| 原名称 | 新名称 |
| --- | --- |
| 独立模组 DamageNexus | CombatNexus 下的 damage 模块 |
| `damagenexus` mod ID/资源命名空间 | `combatnexus` |
| `io.github.naimjeg.damagenexus` | `io.github.naimjeg.combatnexus.damage` |
| `DamageNexus` 入口 | `DamageModule`，由 `CombatNexusNeoForge261` 初始化 |
| `DamageNexus*` Java 类型 | `DamageModule*`，如 `DamageModuleApi` |
| `damagenexus.mixins.json` | `combatnexus.mixins.json` |
| `damagenexus_*` 数据定义目录 | `combatnexus_*` |
| `damagenexus.gametest.runtime` | `combatnexus.gametest.runtime` |

迁入 482 个生产 Java 文件和完整 92 个测试/辅助文件；文件归属和原始 SHA-256 见 [damage-source-manifest.json](damage-source-manifest.json)。内容分类依据实际 NeoForge import 与 26.1.2 Mixin 绑定；未趁迁移重写伤害算法。

这是新模组身份，**不兼容原 Java 包名、旧网络协议、配置文件名或旧资源 ID**。旧数据包需更新 namespace、数据目录、属性 ID、翻译键和相关命令；旧存档中的实体/方块/物品/附件/组件引用不会自动重映射。迁移旧世界之前必须备份并另行实现、验证数据迁移。不要同时加载旧 DamageNexus 与 CombatNexus，二者都接入同一原版伤害过程。

## 依赖锁定（2026-11-03）

下表使用官方 metadata 中对应 Minecraft 分支最新版本；版本固定，构建不依赖动态 latest。

| target | Loader | 配套 API | 状态 |
| --- | --- | --- | --- |
| NeoForge 26.1.2 | 26.1.2.126 | 内置 | 主开发基线 |
| NeoForge 1.21.1 | 21.1.265 | 内置 | 保留骨架，未验收 |

查询来源：[NeoForge Maven](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml)。

NeoForge 26.1.2 与根/common 使用 Gradle 9.5.1 / JDK 25 / ModDevGradle 2.0.141。Gradle wrapper、插件版本与 loader 版本分开维护。NeoForge 1.21.1 仅为未验收骨架。

开发范围仅保留 NeoForge；Fabric 与 Forge target 已移除，不再提供构建、CI 或发布支持。
