# 发布 CombatNexus

当前只允许发布 `targets/neoforge-26.1.2` 的完整实现。先执行目标自己的 `clean build`、检查 jar 与运行验证记录；骨架的所有 publish 任务会拒绝执行。

每个产物对应一个 loader 和一个精确 Minecraft 版本，不使用 universal jar。公共源码编译进 target，common 不单独发布。许可证随 jar 分发。

## Maven

设置 `COMBATNEXUS_MAVEN_URL`、`COMBATNEXUS_MAVEN_USER`、`COMBATNEXUS_MAVEN_PASSWORD` 后在 target 内运行 `gradlew publish`。没有 URL 时不配置远程 Maven 仓库。不要提交凭据，也不要使用模板作者的发布服务器。

## CurseForge / Modrinth

在根 gradle.properties 中填写 `publish_curseforge_project_id` 与 `publish_modrinth_project_id`，或者使用 `CURSEFORGE_PROJECT_ID`、`MODRINTH_PROJECT_ID` 环境变量。token 仅使用 `CURSEFORGE_TOKEN` 与 `MODRINTH_TOKEN`；可用 `PUBLISH_CHANGELOG` 设置说明。在 target 内执行 `gradlew publishMods`。

基线版本是 SNAPSHOT；发布前必须设置正式版本号和正确发布类型，核实项目归属与 loader/MC 依赖范围。本次开发不执行上传或发布。
