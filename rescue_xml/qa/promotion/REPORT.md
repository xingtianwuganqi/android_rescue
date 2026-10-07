# 当前修复状态（2026-10-05）

已修复下方原始测试报告中的 3 个已复现缺陷，并将广告全屏占用改为按广告实例持有和释放。修复后 Android 共 84 项测试全部通过，Debug APK 构建成功。详情见 [FIXES.md](FIXES.md)，当前统计见 [results.json](results.json)，原始失败统计见 [results-before-fix.json](results-before-fix.json)。真实广告 SDK、页面旋转及晚到回调仍需设备验收。

---

以下为修复前的原始测试记录；其中失败结论、代码行号和测试数量描述的是修复前版本。

# 观看视频增加曝光：测试报告

测试日期：2026-10-05。对象：当前 `rescue_xml` 的曝光协调器、奖励恢复、推广接口、入口与快照列表，以及相邻本地后端 `topic_promotion`。

**结论：新增测试复现 3 个 Android 异常处理 bug，另发现 1 个需要真机确认的全屏互斥风险。后端业务、迁移和 MySQL 并发测试全部通过。真实广告 SDK 及界面验收尚未完成。**

本次只新增 QA 测试和报告，未修复或修改产品代码，也没有提交真实广告奖励、部署或操作生产数据库。

## 测试结果

| 范围 | 结果 | 说明 |
| --- | --- | --- |
| 默认 Android 测试 | 44/44 通过 | 含推广接口 7 项、奖励策略 2 项、列表及其他既有测试 |
| 加入已有领养 QA 与新增推广 QA | 78 项，75 通过、3 失败 | 原 44 项 + 已有领养 QA 19 项 + 本次推广 QA 15 项；3 个失败均是缺陷断言 |
| 本次新增推广 QA | 15 项，12 通过、3 失败 | 真实协调器、Repository、Retrofit；模拟 SDK 事件和 SharedPreferences 内存/磁盘 |
| Debug APK | 构建成功 | 已有广告 SDK D8/Kotlin 警告不阻止构建 |
| 本地后端业务及部署迁移 | 29 项通过 | SQLite 内存数据库；最初跳过 4 项 MySQL 行锁测试 |
| 本地后端 MySQL 并发 | 4/4 通过 | 新建隔离 MySQL 8.0.42 临时库，Unix socket 访问，结束后关闭服务 |
| 后端合计 | 33 项全部通过 | 包括最初跳过后补跑的 4 项，没有仍被跳过的推广测试 |
| 测试环境路由探测 | promotion-state GET 返回 JSON 401；promotion OPTIONS 返回 200、允许 POST | 未登录探测，不能证明线上迁移、资格或实际奖励提交可用 |
| 设备及 SDK | 未实测 | adb 无设备，未安装 Android emulator；广告 App/奖励位配置非空，但不能证明有效填充或回调可用 |

修复前详细统计和失败断言见 [results-before-fix.json](results-before-fix.json)。测试文件见 [PromotionCoordinatorAuditTest.kt](kotlin/com/rescue/flutter_720yun/promotion/qa/PromotionCoordinatorAuditTest.kt)。

## Bug 1：首次奖励保存失败后，恢复提交没有重新确保落盘（P2）

位置：[奖励观察处理](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/promotion/RewardedTopicPromotionCoordinator.kt:82)、[恢复逻辑](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/promotion/RewardedTopicPromotionCoordinator.kt:144)。

复现：收到真实 Reward，SharedPreferences.commit 返回 false，界面提示“奖励保存失败，请保持应用打开后重试”。Android 此时仍可能在内存中保留该记录。存储恢复后回前台，restore 从内存读取记录，直接发 POST，没有重试失败的持久化操作。奖励在提交过程中仍可能没有磁盘副本，若进程此时退出而请求未成功，重启后原 UUID 和奖励记录不可恢复。

同时 RewardObservation 已标记 observed，重复奖励回调不再重试初次保存。因此该提示不能保证“保持打开后重试”会先完成可靠落盘。

证据：`rewardMustBeDurablySavedBeforeRestoringAfterCommitFailure` 失败；恢复路径已发起提交，但模拟磁盘仍没有原记录。测试模型区分内存和磁盘：commit 失败仍更新内存，与 Android 行为一致。

建议：保留待落盘奖励，明确重试持久化；只有确认已落盘的记录才进入可恢复提交，失败时保持原 UUID。

## Bug 2：重试时间保存失败后重启，绕过 Retry-After（P2）

位置：[提交异常处理](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/promotion/RewardedTopicPromotionCoordinator.kt:123)。

复现：奖励初次保存成功；POST 返回 429、Retry-After=60 秒；保存新的 next_retry_at 时 commit 失败。代码忽略 put 的返回值，只建立内存定时任务。进程重启后定时任务消失，磁盘仍是 next_retry_at=0 的旧记录，restore 会立刻读取状态并恢复提交，没有等待原 60 秒。

证据：`failedBackoffPersistenceMustNotAllowImmediateRateLimitReplay` 失败；模拟重启恢复后，等待期内已出现额外请求。正常 commit 成功的 429 等待和原 UUID 重试测试通过。

建议：重试状态写入失败必须被处理；保证原等待时间可靠保存，或进入明确的暂停恢复状态，避免重启后立即连发。

## Bug 3：业务拒绝后不刷新推广资格（P2）

位置：[失败处理](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/promotion/RewardedTopicPromotionCoordinator.kt:119)、[详情/成功页观察更新](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/promotion/PromotionEntry.kt:29)。

复现：观看前资格允许，但提交时后端返回 DAILY_LIMIT_REACHED 等终止业务错误。协调器将记录标记 stopped 并提示失败，没有重新 GET promotion-state，也不触发 changes；PromotionEntry 的 updates 只 render 原有 state。若提交结果在页面回前台并已做过一次刷新后到达，旧的 can_promote=true 可继续显示为观看入口。

下一次 start 会再次查询资格，后端仍能拒绝，所以不是绕过额度；问题是界面状态滞后、反复确认和失败提示。本次自动化复现使用 DAILY_LIMIT_REACHED，其他拒绝的页面刷新行为来自同一错误处理路径。

证据：`businessRefusalMustRefreshEligibilityWithoutClaimingSuccess` 失败，只有 POST，没有应有的后续资格 GET。

建议：终止业务拒绝后刷新该帖资格及对应卡片，保留真实失败原因，不显示推广成功。

## 风险 4：旧广告销毁/晚到关闭可能清除新广告的全屏标志（代码检查，待真机）

位置：[60 秒清理](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/ads/FullscreenAdController.kt:146)、[LateRewardListener](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/ads/FullscreenAdController.kt:163)。

代码使用全局布尔值 TakuAds.fullscreenPresented。旧页面销毁后，LateRewardListener 的关闭回调直接写 false，但不会更新旧控制器 rewardShowing。旧控制器 60 秒后的清理仍可能因 rewardShowing=true 再次写 false；它没有检查当前全屏广告是否仍属于自己。

可能时序：A 视频期间页面销毁→A 晚到关闭→B 视频开始→A 的 60 秒清理将 B 的全屏标志清为 false。该标志供开屏、插屏及激励视频共用，失去互斥可能导致广告重叠。实际 SDK 关闭、销毁和旋转时序未实测，因此不计入 3 个已自动化复现 bug。

建议：全屏占用绑定广告实例/attempt，仅持有者可释放；旧回调及旧定时清理不能解除新占用。需真机验证 60 秒宽限及关闭后再次观看。

## 已通过的主要场景

- 只有 Reward 创建奖励；PlayEnded、Closed、Failed、Destroyed 不凭空发奖。
- 奖励正常落盘后 POST，成功后才删除记录；JSON/Bearer、UUID 及场景绑定正确，不保存 token。
- 同一 Reward 重复回调只提交一次；关闭后的晚到 Reward 仍使用原目标和 UUID。
- 切换账号后原奖励不使用新账号提交或提示；原账号返回后恢复原 UUID；旧账号 Loaded 不显示广告。
- 429 正常等待后查询状态并以原 UUID/body 重试；503 恢复保留记录，模拟内存任务重建后仍用原 UUID。
- 重复 restore 不重复提交；24 小时过期只查询状态、不 POST。
- 历史成功快照不会被当成新的一小时；成功后状态过期显示已结束，查询失败提示已处理、需刷新确认。
- 后端首次创建、续期永久复用推广行、不累加剩余时长、边界过期、相同 UUID 不二次续期/扣额、目标/场景冲突、上海自然日额度、黑名单与帖子终态、严格请求字段、事务回滚恢复。
- 后端首页/同城推广排序、普通帖子页内随机、快照稳定、删除/过期过滤、空页 has_more、批量摘要、发布真实 ID、清理及迁移兼容。
- MySQL：相同 UUID 并发只成功计数一次；不同 UUID 续期共用推广行；跨帖争抢最后额度只有一次成功；相同 UUID 换目标冲突不重复扣额。

## 未完成的端到端验收与阻碍

- 当前没有连接手机/模拟器，无法真实播放 Taku/AnyThink 广告。配置非空不等于广告位、填充、隐私同意及奖励时序正确。
- 三个入口的点击、取消、重复点击、播放失败、视频关闭、旋转、页面销毁、60 秒宽限和跨广告互斥仍需设备验证。
- 没有授权的远端测试账号及实际奖励回调，未对远端调用推广 POST；未核验线上推广迁移与可用额度。
- 未做真实进程强杀、磁盘写失败、离线恢复与真实 SDK 晚到奖励测试；自动化只模拟这些前置状态和事件。
- 未实机验证推广成功后首页/同城刷新的可见位置和曝光标签。后端排序、分页及客户端纯逻辑已测试。

MySQL 并发阻碍已解决：本次新建临时隔离库补测，未使用已有数据，测试完成后关闭服务。

## 重跑命令

在 `rescue_xml`：

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug --offline --console=plain
./gradlew -I qa/adoption/qa.init.gradle -I qa/promotion/qa.init.gradle :app:testDebugUnitTest --offline --console=plain
```

第二条启用 JVM 主调度器和独立 QA 目录，修复前有 3 个缺陷断言失败，修复后全部通过。测试通过反射注入真实单例的接口仓库及模拟存储，未改变产品实现；不接入真实广告网络。

本地后端使用隔离 `apps.topic_promotion.test_settings` 运行 `tests`、`test_deployment_migrations`；`test_concurrency` 另在新建的临时 MySQL 上执行。首次 SQLite 扫描显示 4 skipped，后续 MySQL 已补跑全部通过。
