# Android V2 实现记录

依据 `../front_demand_v2.md` 第 10、11 节及本机后端 `API_v2.md`、`API_promotion.md` 实现。范围仅 `rescue_xml`，未修改 Compose 工程或用户的需求文档。

## 本次实现

- 领养状态兼容流程 `version` 别名及管理未读数；申请 DTO 增加 `end_reason`，统一严格判定“被拒绝”。支持数组/逗号字符串预览图、原帖图片补取去重和设备时区时间。
- 申请前检查最新开放状态和资料，只显示一次领养须知，不额外输入说明。登录/资料保存返回仅刷新，不自动申请。卡片提供同意、拒绝、结束、完成和放弃；先获取最新申请/权限，确认后提交，申请版本与流程版本分开。完成、关闭及申请操作保留各自幂等键。资料面板只展示快照。
- 复制联系重新 GET 本人申请详情并校验账号、获取事实和帖子可用性，提示“已复制联系方式”。未读提醒为空时收起整个提醒区域。已有双入口、分类提醒、全局角标及旧后端分类兼容继续使用。
- 独立 PromotionService/Repository，严格 HTTP 200/code 200，保留业务错误、字段错误及 Retry-After；旧账号响应不能更新新账号或使其登出。V2 通用解析器保留 HTTP 200 内的业务拒绝及其错误字段，非 JSON 响应显示服务暂不可用。
- 应用级 RewardedTopicPromotionCoordinator 绑定账号/帖子/场景/UUID。只有 SDK onReward 创建奖励记录；播放结束/关闭/失败不创建奖励。先同步保存最小奖励记录，再提交。账号隔离、原 UUID 恢复、429 等待、5xx/断网重试和不超过 24 小时的恢复窗口；成功后重新查询当前状态，不把历史幂等响应当成新的一小时。
- 类型化 SDK 事件和全屏互斥；页面销毁后由不持有 Activity 的奖励接收器处理晚到回调。销毁后的 SDK 清理宽限为 60 秒，需要真机确认平台回调时序；宽限结束后销毁原广告对象，未收到奖励的观看不补奖。
- 我的发布使用 authpublishlist 批量推广摘要，其他用户继续原接口；结束帖查领养流程区分“完成领养”/“结束领养”。本人详情、我的发布、发布成功页共用主动观看协调器。成功发布使用服务端真实 topic_id；缺 ID 不猜目标，仍可退出。成功事件、草稿清理和发布回调防重。
- 首页/同城保留 Form 编码，增加 promotion_feed/snapshot_id，完整保留 meta。按服务端顺序、业务 topic_id 去重，广告插入在业务顺序之后；按 has_more 继续空页，快照 409 清批次并重载首屏，城市/账号变化隔离旧请求。搜索、收藏、个人主页不使用快照排序。

主要新代码位于 `promotion/`、`home/repository/FeedRepository.kt`、`home/models/FeedModels.kt`、`network/V2EnvelopeAdapter.kt`、`adoption/ui/AdoptionActionFlow.kt`。

## 验证

2026-10-04：44 项单元测试通过；`:app:assembleDebug` 成功。新增测试覆盖原 UUID/JSON/Bearer 重试、200 中的业务拒绝、Retry-After、旧账号 401、错误目标响应、非 JSON 响应、晚到/重复奖励的纯状态逻辑、24 小时及账号限制、空页/快照去重、预览图兼容和拒绝判定。测试不等于真实广告 SDK 验收。

Lint 使用工作区外的空临时基线完整扫描，报告 `app/build/reports/lint-results-debug.html`：6 errors、257 warnings。6 项错误为已有 Leanback launcher、touchscreen optional、Leanback feature 声明（主/debug Manifest 各一项），新增 Kotlin 模块没有报告问题。没有提交自动生成的忽略基线。

APK：`app/build/outputs/apk/debug/app-debug.apk`。

## 尚需环境验收

- 当前无连接 Android 设备，未安装模拟器；须真机验证视频加载/播放/关闭、销毁后奖励、旋转、账号切换、进程重启恢复、SDK 60 秒清理宽限及页面视觉效果。
- 未调用生产写接口或部署后端。须确认新版接口及迁移部署，再联调多人沟通/完成联动、额度/续期、推广快照和历史幂等重放。
- 工程尚无完整厂商/FCM 业务推送接收渠道。本次保留已鉴权的外部申请 Intent 转发，未配置远程推送服务；站内提醒及应用内消息角标不依赖通知许可。桌面数字角标仍取决于通知渠道和启动器支持。
