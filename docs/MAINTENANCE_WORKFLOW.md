# CombatNexus 维护工作流

长期架构、模块边界、变更范围和验证要求统一见 [DEVELOPMENT.md](DEVELOPMENT.md)。

当前唯一启用的功能 target 为 NeoForge 26.1.2。仅保留 NeoForge 1.21.1 骨架；它完成版本适配、相同语义测试及运行验收后，才可更新 `ci.enabled` 与支持矩阵。不得把继承模板的版本列表当作已经支持的功能列表。

公共合约变更需验证全部已支持 target；平台专属变更验证对应 target；构建/CI 变更检查 wrapper、矩阵发现、产物 metadata 和发布边界。评审记录说明改了什么、为什么、测试结果与未验证范围。
