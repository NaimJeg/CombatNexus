# CombatNexus 开发约定

本文件是仓库开发入口。遵循 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)。

- 仅维护 NeoForge target。当前以 `targets/neoforge-26.1.2` 为基线，Java 25；NeoForge 1.21.1 为未启用骨架。Fabric 和 Forge 暂不在开发范围内。
- 模组 ID 为 `combatnexus`，根包为 `io.github.naimjeg.combatnexus`。伤害属于 `combat.damage` 子域，不得将全部新业务继续塞入伤害管线。
- `common` 放没有已证实平台差异的业务代码与资源；有实际 API 差异的实现放 target。平台依赖仍需逐步由明确合约隔离，不通过反射或运行时版本分支伪装兼容。
- 共用源码以 source-set sharing 编译；target 不依赖 `project(':common')`。`common` 独立编译仅采用 NeoForge 26.1.2 的 API 类路径，不纳入 target 源码或资源，不生成发布 jar。
- `docs/DamageNexus-main` 是用户提供的只读迁移参考，不能作为编译输入或修复目标。迁移映射见 `docs/legacy/migration/damage-source-manifest.json`。
- 修改命名空间必须同时检查注册 ID、数据目录、Mixin、网络、配置、语言文件、Java API 和测试。禁止悄悄宣称旧存档兼容。
- 属性、Effect 和面板共享服务端计算快照及来源明细；客户端只展示，不另算一套权威公式。复用原版已有属性，禁止同一 Effect 同时走原版和新流程造成双重收益。
- 按用户决定移除现有 JUnit 单元测试、GameTest 及其专用夹具和构建接线；后续行为验证使用 AutoJS 配合脚本，不默认恢复 JUnit/GameTest。新行为仍需覆盖正常、边界和失败语义，记录脚本、运行环境与实际结果；AutoJS 依赖已安装不代表替代脚本已实现或验证通过。不得降低 Mixin require 规避失败。
- 提交前在目标目录执行 `gradlew clean build`；Mixin、事件、网络和资源改动还需对应运行验证。记录实际通过范围，编译不等于运行正确。
- 依赖固定版本并记录官方来源。禁止动态版本、个人绝对路径、凭据、缓存或 `build/` 输出入库。
- 未移植 target 禁止发布；更新 CI、README 与实际支持矩阵，不把空入口的成功构建视为功能覆盖。
