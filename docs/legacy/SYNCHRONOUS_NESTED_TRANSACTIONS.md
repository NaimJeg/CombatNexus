# 同步嵌套事务

2026-11-05：源码重构已实施；仅完成 Java 25、NeoForge 26.1.2 的构建和静态检查，未启动游戏、服务器或执行 AutoJS。本文取代原版接入记录中“嵌套治疗沿用原版”的临时策略。

## 执行合约

- 每个请求独立准入，持有自己的 ticket、定义代际、计算输入、应用计划和回执。APPLICATION 中可以同步执行子请求；子请求返回前完成自身应用。
- 子请求继承当前栈顶的父 ID、根 ID、权限和触发抑制，增加深度；不能借外层祖先权限、换服务器或指定另一个显式父级。根链派生数、服务器 tick 数、深度和通知容量共同限制递归。治疗不再另设深度 16。
- 只要执行栈中存在 COMPUTE，就禁止新的战斗请求和受管世界写入。原生 heal、hurt、setHealth 和 setAbsorptionAmount 也检查计算阶段；原版与公共伤害归属解析均在 COMPUTE 中执行。这不是对任意 Java 扩展或所有世界 API 的沙箱。
- 原生伤害在 hurtServer 入口准入并绑定上下文，Incoming 复用该准入；公共伤害的原生延续不重复扣费。即使原版在 Incoming 前拒绝，本次入口已消耗预算，且没有伪造结算。
- 应用阶段嵌套 heal 完整经过治疗倍率、原生事件、回执和通知。事件完成后才采样外层生命，避免将事件内子治疗的恢复算入外层。
- 完成通知进入服务器 FIFO，在可变执行栈退出后排空。应用内先完成的子请求先入队；通知回调内产生的子请求通知追加队尾，不能递归投递。观察回调仍不能通过隐式调用获得写入权限。

## 隔离与失败

伤害附魔、暴击、重锤、长矛和投射物的临时捕获改用调用栈帧。子调用消费自己的待用捕获，退出恢复父帧；公共提交与 Incoming 关联同时核对执行身份，避免同目标、同 DamageSource 的重入偷取外层提交。

父请求取消或异常不会回滚已完成子请求。finally 关闭作用域、释放剩余通知槽和准入票据。物理生命写入期间若出现同目标的受管嵌套写入，外层无法从净差确定自身贡献：保留 UNKNOWN 证据，不提供虚假的精确恢复/生命损失，不据此发放溢出或吸血收益。普通事件内嵌套发生在外层写入采样之前，仍可分别得到精确回执。

## Java API 变化

DamageRequest 保持不可变，其预建 lineage 是请求描述；实际嵌套执行身份通过 DamageResult.lineage() 和 settlement 获取，包括已选择执行身份后的准入拒绝或无结算失败。不要用 request.lineage() 推断实际父链。

CombatRuntime 和 DefaultHealingService 构造器移除独立 healingDepth 参数；内部 nativeApplication 必须提供已准入 lineage，不再支持延迟授权的无身份应用作用域。HealingApplicationPort 增加 UNKNOWN_WRITE 状态，端口实现应按证据报告。

## 验证

构建检查使用 JDK 25.0.3、NeoForge 26.1.2.126。目标端及 common 的 clean build、verify-common-boundary.py、verify-baseline.py 通过；修改的 AutoJS 脚本通过 node --check。这些检查不验证 Mixin 运行注入或事件顺序。

verify-native-healing.js 已更新嵌套倍率、独立回执、父子链、通知子调用、父取消保留子提交、深度耗尽、兄弟共享根预算、COMPUTE 原生写入拒绝等断言。verify-native-healing-failure.js 覆盖原生提交后异常及物理写入内嵌套的 UNKNOWN 回执。已有 runner verify-native-integration-runtime.py 调用这两份脚本。脚本需要已启动的隔离 AutoJS 环境，治疗脚本会重建 overworld 战斗会话；本轮未执行，运行结果待补。

跨领域伤害→治疗已有脚本断言；完整治疗→伤害→治疗、同源嵌套伤害捕获、实际反伤/荆棘链及压力容量耗尽仍需运行验证，不能从构建成功推定通过。
