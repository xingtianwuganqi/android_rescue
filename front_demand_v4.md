# Android V4：客户端埋点接入需求

修订日期：2026-10-22。适用仓库：android_rescue；当前主接入工程为 rescue_xml（Kotlin / Activity / Fragment / ViewModel / Retrofit）。rescue_compose 是独立工程，如需接入应复用协议并单独验证，不能宣称修改 XML 工程就覆盖了 Compose 版本。

**状态：rescue_xml 首批埋点代码已接入，离线单元测试及 Debug 构建通过；设备和后端联调尚未验证。** 后端已完成隔离验证，实际环境是否部署需联调确认。全部事件由客户端上报，后端登录、注册、发布流程不自动生成事件。

完整字段、15种事件、属性白名单、响应与限制见同目录 [API_analytics_v4.md](API_analytics_v4.md)。本需求与 front_demand_v2.md、front_demand_v3.md 的既有业务并行，不替换其错误处理或界面规范。

## 1. 事件及采集时机

| 事件 | Android 触发时机 | 关键字段/限制 |
| --- | --- | --- |
| app_open | 主进程冷启动进入前台，或后台达到30分钟后恢复 | 同一轮进入前台一次 |
| app_quit | 主进程整个 App 进入后台时尽力发送 | 不等同于 Activity 切换或退出登录 |
| page_view | Activity/Fragment 对应业务页面实际可见 | page 必填；不要在每次 LiveData 更新时发送 |
| login_click | 用户点击登录入口 | 可选 login_method、referrer_page |
| login_success | 登录/注册自动登录成功，保存新 Token 后 | 必须携带新 Token；恢复已保存账号不算登录 |
| login_failed | 本次登录尝试明确失败 | reason_code 必填，验证码校验成功不等于登录成功 |
| publish_click | 用户点击发布入口，登录拦截之前 | 未选类型可不传 publish_type |
| publish_page_view | 发布表单实际可见 | publish_type 必填，page 与类型一致 |
| publish_success | 新建业务成功且取得可靠对象 ID | publish_type、object_id 必填；编辑不记录 |
| logout | 用户最终确认主动退出，清除 Token 之前 | 发送后立即继续退出，无需等响应 |
| logOff | 用户最终确认发起注销，凭证失效之前 | 大小写固定；后端删除账号会清除关联埋点 |
| export_image | 详情页点击生成/导出分享图 | publish_type、object_id 必填，不代表保存成功 |
| export_video | 详情页点击生成/导出分享视频 | publish_type、object_id 必填；功能未上线时不发送 |
| click_video_ad | 点击我的发布或发布成功页的观看视频操作按钮 | ad_placement、object_id 必填，需要有效 Token |
| video_ad_success | 原观看请求的推广 POST 确认成功后 | ad_placement、object_id、reward_attempt_id 必填，需要有效 Token |

页面白名单：home、login、publish_selector、publish_adoption、publish_find_pet、publish_show_pet、topic_detail、messages、profile、adoption_profile、message_list。类型为 adoption / find_pet / show_pet。不要上传 Activity 类名、中文页面标题、任意 URL 或未约定属性。

生成海报点击对应 export_image；下载、渲染、预览和保存回调不再次计点击。当前已核对 TopicShareFlow 有海报入口，未确认已有视频导出入口，后者按实际功能上线接入，不为埋点新增产品功能。

## 2. 标识、会话与账号

建议新增统一 AnalyticsManager / AnalyticsSessionStore，使用独立的持久化命名空间，不能在各 Activity 中独立创建标识。

| 状态 | 要求 |
| --- | --- |
| anonymous_id | 启动初始化采集时首次生成随机 UUID，持久保存；登录/退出不改，不使用 Android ID、广告 ID、手机号或硬件指纹 |
| session_id | 随机 UUID，一次使用过程的标识，与登录 Token、UserManager.sessionRevision 不同 |
| sequence | 同会话从1递增，换会话重置；状态修改和事件创建保持串行，避免并发重复分配 |
| last_interaction_at / background_at | 分别记录真实前台交互与 App 进入后台的时间；自动请求/埋点回调不能延长会话 |

持久化可用独立 SharedPreferences 或工程已有 DataStore；状态恢复完成后再生成首个事件。不要用阻塞式磁盘写入拖慢点击或页面进入。匿名标识和会话状态不应随账号 logout 清除；应核对 Android 备份策略，若要求“重装后新安装实例”，排除这份状态被备份恢复，不能只依赖卸载假设。

30分钟规则：无会话创建；冷启动会话仍有效可沿用，但本次冷启动仍记 app_open。后台不足30分钟恢复不记新 app_open；后台达到30分钟恢复先换会话再记打开。一直在前台闲置达到30分钟时，下次操作前换会话，只记该操作。持续交互不会每隔30分钟固定换会话。

匿名→登录成功沿用同一会话；主动退出或 A→B 切号后换会话。UserManager.sessionRevision 也会因 Token 改变递增，它负责原业务状态隔离，不能直接当埋点 session_id 或在每次 revision 变化时无条件重建会话。

事件创建时固定 event_id、anonymous_id、session_id、sequence、occurred_at 和当时账号/Token，随后立即异步发送。不要在协程或拦截器真正发送时才读取“当前 Token”，否则退出/切号后可能把旧事件算给新账号。原匿名请求始终不带认证头；若调度前原身份已经无法安全使用，丢弃，不能借用新账号重发。

## 3. 按当前 rescue_xml 工程接入

下列路径相对 rescue_xml/app/src/main/java/com/rescue/flutter_720yun/，为已核对的实现入口，名称不是新增代码已完成的承诺。

| 当前文件/模块 | 接入工作 |
| --- | --- |
| BaseApplication.kt | 当前已有主进程判断；仅主进程初始化采集，WebView/广告辅助进程不创建第二套匿名/会话或重复报打开 |
| MainActivity.kt、home/fragment/HomeFragment.kt、message/fragment/MessageFragment.kt 等 | 主导航的实际页面进入与发布入口点击；隐藏 Fragment、ViewPager 预加载和重复数据刷新不算曝光 |
| home/activity/LoginActivity.kt、home/viewmodels/LoginViewModel.kt | 登录入口采集 login_click；loginNetworking/注册自动登录确认业务 code 后保存用户，再报一次 login_success；不要同时在 Activity observer 和 ViewModel 各报一次 |
| util/UserManager.kt | 使用 token/userId 判断当前认证状态及账号变化，读取已保存账号不补报登录成功；只在埋点请求中传 Bearer 头，不改变旧接口 body token |
| home/activity/ReleaseTopicActivity.kt、home/viewmodels/ReleaseTopicViewModel.kt | 表单可见报 publish_page_view；新建成功用 releasedTopicId / data.topic_id 报 publish_success，利用现有 published/publishing 防止状态重放重复采集 |
| findpet/MyFindPetActivity.kt、FindPetRepository.kt、FindPetViewModels.kt | 找宠表单曝光及首次新建结果采集；save 可能用于修改，必须区分首次创建、修改和开启；补齐下文对象 ID 可靠来源后再报成功 |
| show/activity/ShowReleaseActivity.kt、show/viewmodels/ShowReleaseViewModel.kt | 秀宠表单可见及新建结果采集；当前仅消费状态，先处理对象 ID 来源 |
| home/activity/HomeDetailActivity.kt、home/share/TopicShareFlow.kt | 点击正常生成海报入口采集 export_image，使用有效 topicId；调试无二维码入口不混入正式统计，可不采集 |
| user/activity/UserSettingActivity.kt | logoutDialog 最终确认后、UserManager.logout 前采集 logout；401 导致的被动清理不伪报主动退出 |
| 实际账号注销入口 / network/UserService.kt | 最终确认注销后、凭证失效前尽力采集 logOff；如果客户端尚无入口，应明确未接入，不在网络 Service 定义处自动埋点 |

进程前后台可接入 ProcessLifecycleOwner；其 ON_PAUSE/ON_STOP 有延迟，适合避免配置变化和 Activity 切换被误认作后台，不能承诺毫秒精确的退出时刻。记录实际观察到的时间，并验证30分钟边界。参考 [Android 官方说明](https://developer.android.google.cn/reference/androidx/lifecycle/ProcessLifecycleOwner)。如使用现有 ActivityLifecycleCallbacks，应同样正确处理配置变化、多个 Activity 交接和后台辅助进程。

不在每个 Activity.onResume 无条件记 app_open，也不在 Application.onCreate 直接把后台进程启动算作前台打开。页面曝光按当前实际可见页面去重，旋转重建、权限弹窗、LiveData 重放不增加同一次曝光；返回已离开的页面才可算再次访问。可参考 [Activity 生命周期](https://developer.android.google.cn/guide/components/activities/activity-lifecycle)。

## 4. 独立异步请求通道

新增独立 AnalyticsService / AnalyticsTransport（建议命名）；复用 BuildConfig.API_BASE_URL 的环境选择，路径为 `/api/v4/analytics/events/`，不要复制生产域名常量或把测试事件发到生产。platform 固定 android，app_version 使用当前应用 versionName，不额外添加 build_number。

```json
{
  "event_id": "14b921cb-458b-457e-a655-04aec062d8f0",
  "event_name": "publish_page_view",
  "anonymous_id": "21a2cfc1-0304-4ecb-a52c-67b471095d9e",
  "session_id": "75477b3d-4fa8-43bc-8094-a86cba8a6f6d",
  "sequence": 4,
  "occurred_at": "2026-10-21T10:01:05+08:00",
  "platform": "android",
  "app_version": "1.0.0",
  "page": "publish_adoption",
  "properties": {"publish_type": "adoption"}
}
```

示例时间仅示意，实际生成含时区的 ISO 8601 当前时间；采用 java.time 时按工程 minSdk/既有反糖配置选择实现，不引入未支持的 API。object_id 用 JSON 整数（Kotlin Long 等明确类型），不使用 Gson 的任意 Double 数字容器或字符串代替。

每次点击/进入页面立即调用一次，JSON 对象直接作为请求 body，不用 @FormUrlEncoded、paramDic 或 events 数组；Token 只在显式 Authorization: Bearer 头。请求最多8 KiB、properties 最多2 KiB，发生时间只接受最近10分钟到未来5分钟。

建议复用一个专用于埋点的 OkHttpClient，设置 callTimeout 约5秒、retryOnConnectionFailure(false)，不安装 Token 刷新 authenticator、业务 Toast/退出登录拦截器或 retry/retryWhen；不要把业务客户端所有拦截器原样继承。采用 enqueue 或应用级非阻塞协程发起请求，不在主线程 execute/runBlocking。相关能力见 [OkHttp 官方源码](https://github.com/square/okhttp/blob/master/okhttp/src/commonJvmAndroid/kotlin/okhttp3/OkHttpClient.kt)。

当前 network/ServiceCreator.kt 的 awaitResp 在401/403时会 Toast、UserManager.logout 并处理 Activity 栈，**埋点不得走这个扩展函数**。也不直接套用 FindPetRepository 中自动失效账号的请求逻辑。成功/失败回调只结束采集任务，不改变业务页面和登录态。

不将请求绑定到立即退出的页面 lifecycleScope；可用应用级 SupervisorJob 持有在途任务，确保单次失败不取消其他请求，并在采集关闭时取消。这里仅保存正在发送的任务，不建设离线队列、WorkManager、持久重试、定时发送或批量上传。进程终止及后台限制允许漏报，不额外拉起前台服务保活。

## 5. 发布成功对象 ID 的现状

送养模型当前已取得 data.topic_id / releasedTopicId；验证业务 code==200 且 ID>0 后上报一次。HTTP 200 本身不能证明旧业务成功。

当前找宠 save 不返回业务对象，秀宠 ShowReleaseViewModel 只读取 response.code，后端秀宠发布 data 也为空。因此找宠/秀宠的 publish_success 不能假造 object_id、用用户 ID/草稿 ID 代替，或查询列表第一条来猜测刚发布对象。

先核对各接口是否已有可解析的可靠对象 ID：有则补客户端模型；没有则另行同步最小响应扩展或可靠关联方案。在此之前相应页面曝光可以先接，无法满足必填字段的成功事件列为待接入，不发送非法数据，不改变成功事件的后端校验。本次仅写文档，不改业务接口。

## 6. 响应、默认采集与验收

HTTP 200 且 data.status 为 accepted / duplicate 均结束发送。400/401/403/413/415/429/503、网络失败、取消和超时均静默结束，不自动重试，不刷新 Token、不弹 Toast、不跳登录，不影响原发布或页面导航。调试日志仅记录事件名称、UUID、HTTP状态/规范错误码，不记录凭证、联系方式或完整正文。

**2026-10-22 产品决定：启动 App 即默认开启本次使用情况采集，不需要用户先同意，不新增埋点授权弹窗或设置入口。** 主进程初始化匿名标识和会话，在实际进入前台时按原规则上报 app_open；不等待登录、隐私弹窗确认、推送、定位或 ATT 授权。保留内部采集启停控制；内部关闭后停止生成和发送，并取消尚可取消的在途请求，不改变业务登录态，不补报关闭期间的事件。

以下均为待客户端实现后的验收，不代表已测试：

1. 主进程冷启动前台只报一次 app_open，辅助进程不报；旋转、Activity 交接和 Fragment 预加载不重复计打开或页面曝光。
2. 后台不足30分钟恢复沿用会话且不新增 app_open；达到30分钟换会话再记打开。前台闲置超时仅换会话并记实际操作，持续操作不会定时硬切会话。
3. 匿名打开→点击登录→登录成功→打开发布的匿名/会话标识保持一致，成功携带新 Token；账号恢复和 sessionRevision 普通变化不虚构登录成功。
4. LiveData 重放、重新订阅、旋转、成功页面重新打开不会重复计 publish_success；编辑不计新发布；对象 ID 缺失明确列为未完成接入。
5. 退出/切号换会话，原事件不使用新账号凭证；401/403埋点失败不触发 awaitResp 的登出/Toast/清 Activity 逻辑。
6. 海报生成点击记一次，权限、渲染和保存回调不重复；没有视频导出入口则不生成 export_video。
7. 断网、慢网、取消和所有HTTP错误不阻塞交互，不进入 WorkManager/离线缓存/自动重试；关闭采集不发送。
8. JSON字段、数字类型、时区和大小满足白名单，后台按 android/版本可筛选；请求未携带 user_id、source、IP、密码或 body token。

完成实现后，在本文件补充具体接入工程、已覆盖事件、对象 ID 缺口、设备/系统版本及验证结果；XML 与 Compose 验证分别记录。本轮只交付文档。


## 2026-10-22 新增：领养资料与消息详细列表

新增页面编码：`adoption_profile`（领养资料页）、`message_list`（消息详细列表页）。消息首页仍用 `messages`，不是 `message_list`。

领养资料页在内容实际可见时上报；编辑输入、校验、保存、加载完成和普通重绘不重复计页面曝光。消息列表在实际打开或切换到另一种列表时上报；刷新、分页、已读状态变化不重复计曝光。

以下仅展示事件相关字段，实际请求仍需补齐 event_id、anonymous_id、session_id、sequence、occurred_at、platform 等必填字段：

```json
{
  "event_name": "page_view",
  "page": "adoption_profile",
  "properties": {"referrer_page": "profile"}
}
```

```json
{
  "event_name": "page_view",
  "page": "message_list",
  "properties": {
    "message_list_type": "like",
    "referrer_page": "messages"
  }
}
```

`page_view` 且 `page=message_list` 时，`properties.message_list_type` 必填，取值如下：

| message_list_type | 消息列表 |
| --- | --- |
| like | 点赞 |
| collect | 收藏 |
| comment | 评论 |
| system | 系统消息 |
| find_pet | 我的找宠（推荐列表） |
| my_applications | 我的申请 |
| received_applications | 收到的申请 |

该字段只允许用于 `page_view` 的 `message_list` 页面；不能放在顶层，不能使用后端 msg_type 数字、列表标题或自由文本代替。`adoption_profile` 不携带该字段。新增页面也可作为 properties.referrer_page；message_list_type 描述当前页面的列表类型，不描述来源页面。

后端 Admin 的概览、事件列表和路径页支持按页面及消息列表类型筛选；这次扩展使用现有 page 和 properties，不增加数据表或迁移。

接入位置：领养资料位于 `rescue_xml/app/src/main/java/com/rescue/flutter_720yun/adoption/activity/AdoptionProfileActivity.kt`；消息入口位于 `message/fragment/MessageFragment.kt`，列表使用现有消息、找宠推荐和领养申请页面。单 Activity 内切换列表类型也按实际进入的新类型记录，分页/刷新不算切换。


## 2026-10-22 同步：推广激励视频采集规范

统一使用 `click_video_ad` 和 `video_ad_success`，不使用 click_my_video_ad / my_video_ad_success，也不为两个入口分别定义同名重复事件。通过 `properties.ad_placement` 区分 `my_posts` 和 `publish_success`；后端 Admin 可按“推广广告入口”筛选。

两种事件都需要有效 Bearer Token，object_id 是送养帖子 ID。page 可不传；不要把推广 scene=publish_success 直接填入 page，它不是当前页面编码白名单中的值。属性不传广告素材标题、SDK 原始回调或自由文本。

- 点击：用户点击“观看视频/增加曝光”操作按钮时记录，不是广告素材的落地页点击。一次实际按钮点击生成一个 event_id，广告加载、SDK 播放与关闭回调不重复生成点击。
- 成功：收到推广 POST `/api/v2/topics/{id}/promotion` 的 HTTP200、业务 code=200 后记录一次。仅 SDK 播放结束、广告关闭或奖励回调都不能说明已经取得推广权益。
- 使用原 attempt 的 scene、帖子 ID、reward_attempt_id 和账号上下文；原业务奖励恢复提交/重试逻辑继续保留，不能因为埋点失败再提交推广业务。
- 同一 reward_attempt_id 只记一次成功。建议成功 event_id 直接沿用该 UUID（点击事件单独生成 UUID），使重复成功回调/幂等回放仍由事件表唯一约束去重。不能在每次回放成功时创建新的成功 event_id。
- 成功 occurred_at 使用首次得到推广业务成功确认的当前时间；不把很久以前的观看时间当成新上报时间。来源始终取原 attempt，不能取恢复提交时所在的新页面，也不能使用已切换账号的 Token。
- 原业务可以恢复奖励提交；埋点本身仍是单次异步发送，不新增离线补报、队列或自动重试。不保证埋点与权益写入原子一致，统计是客户端自报数据。

以下为部分字段示例，实际请求需携带完整必填字段和 Bearer 头：

```json
{
  "event_name": "click_video_ad",
  "properties": {"ad_placement": "my_posts", "object_id": 123}
}
```

```json
{
  "event_id": "67f5c6a9-2ba1-4910-a305-8d0ac8d1a68b",
  "event_name": "video_ad_success",
  "properties": {
    "ad_placement": "publish_success",
    "object_id": 123,
    "reward_attempt_id": "67f5c6a9-2ba1-4910-a305-8d0ac8d1a68b"
  }
}
```

具体 Android 接入位置（rescue_xml，待实现）：

- `user/fragment/UserTopicFragment.kt` 的本人发布列表观看视频确认操作对应 my_posts；`home/activity/PublishSuccessActivity.kt` 通过 `PromotionEntry` 对应 publish_success。路径相对 `rescue_xml/app/src/main/java/com/rescue/flutter_720yun/`。
- 点击在操作入口选择唯一采集点，不能同时在 Activity、PromotionEntry 与 coordinator 记录同一次点击；SDK 广告素材点击不作为 click_video_ad。
- `promotion/RewardedTopicPromotionCoordinator.kt` 的 `repository.submit(record)` 成功返回后采集 video_ad_success；`RewardedAdEvent.Reward` / PlayEnded / Closed 都不能直接记权益成功，`publish(a,"succeeded",...)` 引发的观察通知也不再次记录。
- 来源使用原 `PendingReward.scene/topic_id/account_id/reward_attempt_id`，不能读取当前显示页面推测。保留原奖励业务恢复提交，重复业务结果沿用同一成功 event_id；协程重启、LiveData 重放和账号恢复不重新生成成功事件。
- 验收覆盖两个入口、取消/失败、SDK 奖励但推广失败、进程恢复、幂等回放与切号；采集请求仍不用会自动登出的 awaitResp，也不改原推广重试行为。Compose 如后续接入需单独验证。


## 2026-10-22 同步：启动默认采集

本节覆盖原“用户同意后才开始采集”的约定：启动 App 即初始化本次使用情况采集，未登录先匿名上报。首个前台 app_open 按既有会话规则只记录一次；不等待用户点击同意、不新增采集授权弹窗，不依赖登录、推送、定位或 ATT 授权。内部开关关闭仍停止后续事件，恢复开启不补造关闭期间事件。

验收增加：首次安装且未登录、尚未确认隐私弹窗或其他权限时，进入前台即可发出一次 app_open；后续确认弹窗或完成登录不会补发同一轮 app_open。Android 只统计主进程真实前台启动，iOS 沿用现有 Scene 去重。原有30分钟会话、单事件异步上报和失败不重试规则保持一致。

本次仅同步产品要求及文档；实际代码实现状态以各端实现记录和测试结果为准，不据本节宣称已通过上述新验收。


## 2026-10-22 Android 实现记录

本轮已在 rescue_xml 接入统一安装/会话与单事件异步通道、App 生命周期、页面/消息类型曝光、登录结果、三类发布入口与表单、送养新建成功、主动退出/注销、正式海报导出点击和推广视频点击/接口成功。采用独立 OkHttp，不使用会触发业务退出的 awaitResp；启动默认开启，内部可关闭，未新增采集设置或授权弹窗。analytics_v4.xml 从云备份及设备迁移排除。

找宠和秀宠缺少可靠新建对象 ID，仍不发送对应 publish_success；没有视频导出或独立发布选择页面时不虚构事件。rescue_compose 未接入。

本地 201 项 JVM 测试通过（其中新增埋点39项），Debug APK 和现有设备测试源码编译通过。当前 adb 无连接设备；未执行真机/模拟器、后端 Admin 和完整业务联调。本轮没有生产上报测试。详细覆盖范围及限制见 [验证报告](rescue_xml/qa/analytics-v4-2026-10-22/REPORT.md)。
