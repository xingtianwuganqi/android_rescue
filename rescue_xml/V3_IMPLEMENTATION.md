# Android V3 我的找宠实现记录

2026-10-19，依据 ../front_demand_v3.md（2026-10-14 版）和本机后端 API_v3.md、find_pet_v3/services/presentation.py 的实际响应字段实现。范围仅 rescue_xml；未改用户需求文档、Compose 或后端。

## 实现

- 消息页增加“我的找宠”，按稳定 category 路由，不改变原点赞、收藏和评论类型。页面查询/写操作鉴权，取消登录返回；账号 revision 变化清理私人状态，旧账号成功/失败响应均被隔离。
- GET /api/v3/find-pet/me/recommendations/ 展示图片、公开描述、城市、当前送养状态、推荐时间和未读。分页按 recommendation_id 去重，失败保留历史和成功页码。no_request/open/closed 分别提供发布、管理与重新开启入口；关闭与城市变化不会清理服务端历史。
- 原 FindPetDetailActivity 之前为空壳，本次在原页面补齐查询、填写、保存、关闭及重新开启，继续使用 searchpet/info、create、effective 的原 Form/body token 协议。207 才是原“无本人信息”业务响应；其他错误不伪装为空表。发布说明固定显示，地区仍复用原选择器/原 address 格式，并可追加城市级 city_id。加载失败不能保存，表单不新增验证手机号或新审批字段。
- 推荐 GET 不自动已读。打开单条时 POST 空 JSON 到本人推荐 read 接口，验证返回 ID 与 read=true 后才更新该项并跳原 HomeDetailActivity。已读失败保留未读并允许重试；不可用历史可标记已读，但不打开私密或删除的帖子。没有推荐自动申请或新增联系授权。
- 我的找宠只显示未读子计数，不再累加进消息总角标。已读成功重新查询原系统未读及推荐未读；没有独立持久化已读表。
- 原系统通知保留原 POST 查询和服务端自动已读语义，普通通知字段兼容；识别 kind=find_pet_recommendation、recommendation_id/topic_id，转入鉴权的推荐已读/原帖跳转流程。原通知查询成功通知角标更新，账号过期和旧响应受到隔离。
- 删除/不可公开占位隐藏描述、城市和图片；结束但仍公开的送养帖可进入原详情，申请/联系继续由实时 V2 权限判断。

主要新模块：java/com/rescue/flutter_720yun/findpet/；管理页复用 home/activity/FindPetDetailActivity.kt；消息入口与原通知修改位于 message/。

## 验证

```sh
./gradlew -I qa/adoption/qa.init.gradle -I qa/promotion/qa.init.gradle \
  :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin \
  --offline --console=plain
```

133 项 JVM 测试通过，0 failures、0 errors。新增 FindPetFlowTest 22 项使用真实 Retrofit、MockWebServer 和 ViewModel，覆盖 Bearer/空 JSON、只查询不自动已读、写失败与成功导航、历史不可用项、分页去重、刷新失败续页、空/关闭状态、HTTP200 业务拒绝、401/旧账号401、非 JSON、错目标已读、旧207/Form/开启关闭码、资料加载锁、通知兼容和不重复计角标。

Debug APK 和现有 Android 设备测试源码编译通过，git diff --check 通过。当前未进行真机页面验收；测试没有调用生产写接口。后端契约注明新推荐接口未部署，真实推荐、原通知未读同步、城市匹配及多人联动需部署后联调，不能将本地模拟测试视为上线验收。

APK：app/build/outputs/apk/debug/app-debug.apk。

## 2026-10-19 首页找宠与新 UI 增量

按 front_demand_find_v3.md 及用户的列表/资料截图补齐首页第三个 Tab、公开互动、真实复制、收藏分类、资料猫咪提示/关闭态/单选/更多与防骗确认，新增设置找宠资料并调整消息顺序。此版本恢复一次“特别提醒”，以新文档覆盖旧版“不弹额外确认”的描述。

新增原通知目标跨页核验、写后本人信息重查、未知写结果先查询、公共列表失败续页和旧未读401的账号保护。155项JVM测试、Debug构建和设备测试源码编译通过；无连接设备，真机与部署联调未执行。详见 [本轮功能与验证报告](qa/find-pet-tab-2026-10-19/REPORT.md)。


## 2026-10-20 推荐消息与独立角标适配

本节按最新 front_demand_v3.md 覆盖上述历史记录中的“推荐属于系统未读”“从系统通知跳推荐”以及“发布额外确认”约定。

- 推荐模型使用 message_id，仅作 MessageList 关联；recommendation_id 用于 V3 已读，topic_id 用于原送养详情。卡片明确显示“系统推荐”。
- authunreadnum 继续原 POST/body token，读取 find_pet_unread，字段缺失或 null 按 0。消息页只从此统计接口更新找宠角标，不再额外查询推荐列表用于角标；App 合计包含找宠一次，并保留 V2 申请提醒原合计。
- 系统通知只显示原正文，删除推荐关联字段及点击跳转；读取系统通知不再触发找宠状态变更。没有新增 authmessage 的推荐查询。
- V3 列表仍不自动已读。单条 read 成功立即更新卡片和角标，再核验当前公开帖子并导航；后续目标刷新失败保留已读、提示重试。重复已读不重复扣减，不允许负数。未读刷新失败保留已有统计并显示重试。
- 保留账号切换清理和旧响应隔离、关闭需求历史、不可用占位及原 V2 申请/联系权限。有效需求空推荐文案明确“暂无同城推荐”。
- 按最新版要求取消发布的额外确认框，固定说明仍显示；原字段校验、可选规范城市 ID、写后真实资料重查和关闭/开启保持原协议。

验证结果见 [本轮报告](qa/find-pet-messages-2026-10-20/REPORT.md)。后端部署、迁移及真机联调不由本地模拟测试代替。
