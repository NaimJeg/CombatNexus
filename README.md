# CombatNexus

Minecraft RPG 属性、公式与 Effect 上下文框架。DamageNexus 现作为 `damage` 模块迁入；后续扩展攻击速度、暴击、移动速度、吸血、穿透、原版 Effect 和玩家详细属性面板。

当前功能基线是 **NeoForge / Minecraft 26.1.2 / Java 25**，包含原 DamageNexus 的伤害管线、规则、结算、网络、调试假人和客户端伤害展示。统一 RPG 上下文与玩家属性面板属于后续开发，不是本次已完成功能。

## 构建

使用 JDK 25；`JAVA_HOME` 指向本机安装目录，无需提交机器路径。

```powershell
# 仓库根：默认构建完整 NeoForge 26.1.2 实现
.\gradlew.bat build
# 独立编译和测试公共源码的参考实现
.\gradlew.bat -p common test
# 完整清理构建
cd targets/neoforge-26.1.2
.\gradlew.bat clean build
# 真实服务端 GameTest 与人工客户端检查
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
```

产物：`targets/neoforge-26.1.2/build/libs/CombatNexus-neoforge-26.1.2-0.1.0-SNAPSHOT.jar`。Linux/macOS 使用 `./gradlew`。

## 支持边界

| Target | 状态 |
| --- | --- |
| `neoforge-26.1.2` | 完整伤害模块迁移与开发基线，CI 启用 |
| `neoforge-1.21.1` | 保留骨架；未验收、CI 禁用、禁止发布 |

仅维护 NeoForge target。Fabric 和 Forge 暂不在开发范围内，其 target 已移除。根 `-PallTargets=true build` 只构建启用的 NeoForge target。

## 结构

- `common/`：共享业务与 assets/data，伤害位于 `io.github.naimjeg.combatnexus.damage`。
- `targets/<loader>-<version>/`：独立 wrapper、平台适配器、入口、Mixin、metadata。
- `docs/DamageNexus-main/`：原始只读参考，不参与构建。
- `gradle/target-conventions/`：发布和迁移测试约定。

各 target 直接编译共享源码；`common` 独立构建使用 NeoForge 26.1.2 真实适配器用于 IDE/测试解析，不发布独立 jar。

## 开发文档

- [开发入口](AGENTS.md) · [模块边界、属性/Effect/面板约定](docs/DEVELOPMENT.md)
- [迁移映射、兼容性与依赖版本](docs/migration/DAMAGE_BASELINE.md)
- [验证记录](docs/migration/VALIDATION.md) · [CI 约定](docs/CI_TARGET_DISCOVERY.md)
- [发布](docs/PUBLISHING.md)

mod ID、Java API 和资源命名空间已改为 `combatnexus`，旧 DamageNexus 存档和数据包不会自动迁移。MIT 许可，保留 DamageNexus 与 [SighsTemple 模板](https://github.com/Tower-of-Sighs/SighsTemple)署名。
