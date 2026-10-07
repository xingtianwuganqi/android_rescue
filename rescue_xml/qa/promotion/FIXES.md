# 观看视频增加曝光：修复记录

日期：2026-10-05。修改当前 Android XML 工程，不涉及后端改动或部署。

## 已修复

1. **奖励首次保存失败**：协调器保留待落盘记录，按原 UUID 自动重试保存；恢复提交前再次检查持久化。持续存储失败时暂停 POST，并提示保持应用打开，无需重新观看。
2. **重试时间保存失败**：提交前先持久化 120 秒恢复保护时间；429/网络异常后检查重试状态写入结果，失败时继续重试落盘，成功才安排提交。正常保存的 Retry-After 仍按后端返回值等待。冷启动丢失内存重试任务时，原磁盘保护记录避免对当前后端的 60 秒限流窗口立即重放。恢复资格查询先完成，然后紧邻 POST 写入保护记录。
3. **业务拒绝后资格未刷新**：403/404/409 后重新查询资格并通知入口和列表刷新；记录停止重试，保留真实失败提示，不显示推广成功。刷新失败时仍通知页面再次加载。
4. **旧广告清除新广告占用风险**：全屏互斥使用引用身份 token。插屏、激励视频各自持有 token；旧关闭回调、晚到播放失败、销毁和 60 秒清理只能释放自己的占用。显示抛异常时释放本次占用。

同时为提交任务使用延迟启动和任务身份校验，避免旧任务结束时移除新任务的提交占用。原 UUID、账号、帖子和场景绑定继续保留。

## 修改位置

- [RewardedTopicPromotionCoordinator.kt](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/promotion/RewardedTopicPromotionCoordinator.kt)：待保存记录、持久化重试、提交保护和资格刷新。
- [PromotionModels.kt](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/promotion/models/PromotionModels.kt)：恢复保护间隔。
- [FullscreenOwnership.kt](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/ads/FullscreenOwnership.kt)、[TakuAds.kt](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/ads/TakuAds.kt)、[FullscreenAdController.kt](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/ads/FullscreenAdController.kt)：占用身份和释放。

## 验证

| 范围 | 结果 |
| --- | --- |
| 原有 Android 测试 | 44/44 通过 |
| 既有领养 QA | 19/19 通过 |
| 推广协调器 QA | 18/18 通过，包含原 3 项失败断言 |
| 新增全屏占用测试 | 3/3 通过 |
| 合计 | 84/84 通过，0 失败、0 跳过 |
| Debug APK | assembleDebug 成功 |

新增回归覆盖首次保存失败的自动恢复、持续保存失败不提交、两次重试状态写入失败后的冷启动保护、旧关闭和旧清理不能释放新占用，以及身份相同值不同引用的隔离。

最终运行命令，在 `rescue_xml` 目录：

```sh
./gradlew -I qa/adoption/qa.init.gradle -I qa/promotion/qa.init.gradle :app:testDebugUnitTest :app:assembleDebug --offline --console=plain
```

统计见 [results.json](results.json)，原始失败结果保存在 [results-before-fix.json](results-before-fix.json)。后端未修改；前一轮 33 项后端测试已通过，本轮未重复运行。

## 验证边界

- 没有手机或模拟器连接，尚未实测真实 SDK 视频、旋转、页面销毁、晚到 Reward/Close 和 60 秒宽限。
- 存储错误通过区分内存与磁盘的 SharedPreferences 模型模拟；如果磁盘始终不可写且进程在首次成功保存前被杀死，内存奖励仍无法跨进程恢复。
- 120 秒保护针对当前后端固定 60 秒限流窗口；后端窗口或网络耗时策略改变时需要同步调整和回归。
- 广告 SDK 原有 D8 警告未阻止构建。未调用远端推广 POST、未部署、未提交 Git。
