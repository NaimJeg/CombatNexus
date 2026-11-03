# CI target 发现

`scripts/discover-targets.ps1` 读取 `targets/*/ci.properties`，输出启用 target 的 JSON 矩阵。每个 target 都必须拥有 descriptor 与自己的 wrapper。

```properties
ci.enabled=true
ci.java=25
ci.attempts=3
```

仅保留 NeoForge target：`neoforge-26.1.2` 启用，`neoforge-1.21.1` 骨架禁用。根 `-PallTargets=true` 遵循同一 enabled 状态。显式 `-Ptarget=<name>` 可以编译骨架，但不会启用发布。

`.github/workflows/verify-common.yml` 监听 common、所有 target、根构建与 conventions 变化。common-tests 使用 JDK 25 和 NeoForge 26.1.2 参考类路径，target-builds 使用各自 wrapper。编译包括迁移后的 JUnit 测试与源码/资源契约；NeoForge job 还运行 `runGameTestServer` 并保留 JUnit/GameTest XML。运行验证记录见 migration/VALIDATION.md，不将普通 build 等同于 GameTest 或客户端验收。

新增 NeoForge 版本完成开发文档规定的功能与运行验收后，再同时启用 descriptor、解除发布门禁并更新 README 支持矩阵。不要只改 `ci.enabled` 就发布空模组。
