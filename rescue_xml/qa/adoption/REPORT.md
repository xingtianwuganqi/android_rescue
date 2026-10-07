# V2 领养流程测试报告

**2026-10-05 修复状态：本报告的 5 个问题已完成本地代码修复。Android 63/63 项测试通过、Debug APK 构建成功；后端 57/57 项测试通过。详情见 [修复记录](FIXES.md)。地区问题需要配套发布后端修复，真机验收尚未执行。以下保留修复前的审计记录。**

测试日期：2026-10-05。对象：当前工作区 `rescue_xml`，包括已有未提交的 V2 修改。未修改产品代码。

结论：发现 4 个有自动化复现证据的问题，以及 1 个代码检查确认的失败恢复问题。当前不能判定完整端到端验收通过；没有 Android 测试设备、模拟器及双账号测试条件。

## 验证结果

| 检查 | 结果 | 边界 |
| --- | --- | --- |
| 原有单元测试 | 44/44 通过，其中领养相关 25 项 | 接口、解析及纯逻辑，不等于真机验收 |
| Debug 构建 | `:app:assembleDebug` 成功 | 存在广告 SDK D8 和已有 Kotlin 警告，未阻止构建 |
| 新增流程测试 | 14 项，10 通过、4 失败 | 真实 ViewModel + Retrofit + MockWebServer；地区测试读取实际资源 |
| 合计 | 58 项，54 通过、4 失败 | 失败为下述缺陷的预期行为断言，不是测试基础设施错误 |
| 测试环境只读探测 | 资料、申请状态、通知未读数 3 个 GET 路径均返回 JSON HTTP 401 | 无登录态；不能据此确认数据库迁移或业务流程可用 |
| Android 运行 | 未执行 | `adb devices -l` 没有设备；SDK 下不存在 emulator 可执行文件 |

修复前的测试统计与断言信息见 `results-before-fix.json`；修复后结果见 `results.json`。APK：`../../app/build/outputs/apk/debug/app-debug.apk`。

## 问题 1：部分城市无法填写领养资料（P1）

位置：`app/src/main/java/com/rescue/flutter_720yun/adoption/activity/AdoptionProfileActivity.kt:93`、`:61`。

复现：新建资料，选择广东省东莞市/中山市、海南省儋州市或甘肃省嘉峪关市。实际 `location.json` 中这四个城市没有 `children`，代码仍打开必选区县列表，只有选中区县才回写城市和区县；空列表无法选择。保存校验又要求城市、区县非空，用户无法完成资料，也就无法提交领养申请。已有地区保持旧值时也无法切换到这些城市。

证据：`everyCityMustOfferACompletableDistrictSelection` 失败，断言列出以上 4 个城市。数据和选择回调链已检查；尚未真机录屏。

建议：为无下级区县的城市提供受后端接受的地区选项，或允许用户补充地区；不能停在空选择器。

## 问题 2：旧申请幂等重放被误报为新申请成功（P1）

位置：`app/src/main/java/com/rescue/flutter_720yun/adoption/viewmodels/AdoptionDetailViewModel.kt:54`。

复现：首次申请写入成功，但客户端收到失败结果（如网关 503/响应丢失）；该申请随后被送养人结束；整帖仍开放。客户端重试相同资料和原幂等键。服务端按原键返回原申请的最新 `ended/not_completed` 状态。客户端忽略 POST 返回对象，无条件发送 `Applied`，显示“申请成功，等待送养人反馈。”，其实没有创建新的有效申请。

证据：`endedIdempotentReplayMustNotReportNewApplicationSuccess` 失败。MockWebServer 返回 ended 的原申请，真实 ViewModel 仍产生 Applied。已核对本地后端 `replay()` 返回关联申请当前状态，因此该响应形态符合契约；没有在远端操作真实申请。

建议：校验返回申请真实 ID、状态及结果；终态重放应展示已结束，并让用户重新确认新申请，再生成新幂等键。

## 问题 3：申请已成功，刷新失败后仍显示可申请状态（P2）

位置：`app/src/main/java/com/rescue/flutter_720yun/adoption/viewmodels/AdoptionDetailViewModel.kt:50`、`:58`。

复现：最新状态为开放且可申请，POST 成功返回申请 501，随后的状态 GET 返回 503。页面状态仍是提交前的 `can_apply=true`，没有转成申请中，也没有清为待重试状态。用户会同时收到成功/网络结果未知提示，按钮仍可能显示“获取联系方式”。

证据：`failedPostSuccessRefreshMustNotLeaveCanApplyState` 失败，真实 ViewModel 在写入成功后仍保留 `can_apply=true`。

建议：写成功时立即更新可信的申请状态，或清除旧可申请状态；将写入成功与后续刷新失败分开提示。

## 问题 4：筛选加载失败时，显示上一筛选的申请（P2）

位置：`app/src/main/java/com/rescue/flutter_720yun/adoption/viewmodels/AdoptionApplicationsViewModel.kt:38`。

复现：收到的申请先加载“申请中”，再切换“已结束”，本次请求失败。筛选按钮已经切换，但 items/page/hasMore 仍来自“申请中”，旧卡片及操作仍可见。如果旧页有更多数据，继续加载还可能把旧分页位置用于新筛选。

证据：`failedFilterChangeMustNotShowPreviousFilterApplications` 失败，新筛选为 ended、请求失败后仍显示 applying 记录。

建议：按身份、帖子、筛选维护数据集；切换数据集时重置分页并隐藏旧项，同一数据集刷新失败才保留旧内容。

## 问题 5：申请资料面板加载失败后空白，缺少直接重试（P2，代码检查）

位置：`app/src/main/java/com/rescue/flutter_720yun/adoption/fragment/AdoptionApplicationSheet.kt:17`、`:42`、`:44`。

复现条件：打开申请时资料，申请详情或帖子状态请求断网/5xx。reload 先清空 application，观察者清空全部内容；错误只弹 Toast。除 409 的自动重查外，面板没有错误占位或重试入口。用户需要关闭再打开，或离开前台后返回。

证据：源码确认清空和错误处理路径；尚未做真机点击验证，未计入 4 个自动化失败。

建议：保留明确的失败状态和“重新加载”按钮。

## 已覆盖的流程与异常

- 无资料只进入补资料事件；完整资料只展示确认事件，确认前不写申请。
- 保存资料只有 PUT，不自动提交申请。
- 关闭、完成、未知流程状态停止在状态检查，不读取资料、不写申请。
- 正常提交使用 JSON、Bearer、空 statement、UUID 幂等键，成功重新查询真实申请。
- 提交失败重试保留原 key/body；已有 applying/communicating 状态阻止重复 POST。
- HTTP 200 内的 PROFILE_REQUIRED、PROFILE_CHANGED、IDEMPOTENCY_CONFLICT、VERSION_CONFLICT、TOPIC_CLOSED、BLACKLISTED 业务拒绝保留错误，不能变成成功。
- 既有测试覆盖 401/403、账号变化、取消底层请求、分页、历史联系获取、帖子完成/删除时清除联系、申请版本与流程版本分离、原生和旧后端通知分类、单条已读、消息角标去重。
- 新增测试验证通知已读失败保留提醒和计数，提醒追加失败保留已有页和计数。

## 尚未完成的验收

- Android 上的登录返回、资料填写、取消/查看领养说明、连续点击、旋转、前后台、断网交互、面板和卡片视觉效果。
- 双账号真实联调：创建申请→同意沟通→申请人显示/复制联系→结束/重新申请，以及拒绝、放弃。
- 多人同时沟通→选择一人完成→其余申请结束→全体联系清除；关闭/删除/注销的事务和通知联动。
- 真后端的黑名单、并发冲突、敏感词、资料变更、幂等重放、账号切换和通知角标。
- 系统/厂商推送和桌面角标。站内逻辑已有测试，实际通知渠道仍需设备及服务配置验收。

以上需要可运行 Android 的设备/模拟器、申请人和送养人测试账号及允许写入的测试环境。目前没有创建或结束任何远端申请。

## 重现命令

在 `rescue_xml` 目录执行：

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug --offline --console=plain
./gradlew -I qa/adoption/qa.init.gradle :app:testDebugUnitTest --offline --console=plain
```

第二条启用独立 QA 测试目录和仅供 JVM 使用的协程主调度器；修复前有 4 个缺陷断言失败，修复后已全部通过。它不加入默认测试集，不改变 Android APK。测试使用反射注入仓库和虚拟账号，未引入新依赖。后续仍需做上述实机验收。
