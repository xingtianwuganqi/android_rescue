# Android V2 开发需求：与最新 iOS 申请及消息流程对齐

修订日期：2026-10-02。业务基线：`/Users/jingjun/Desktop/Githup/LaveCat/front_demand_v2.md`；Android工程：`/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml`。后端契约：`/Users/jingjun/Desktop/Githup/rest_framework/API_v2.md`，产品基线：同目录 `Demand_v2.md`。

已核对当前Android代码：V2资料、申请列表、申请详情及网络模块已经存在，本文件描述在这些模块上继续调整的要求。本次更新文档和配套后端，未实现Android界面调整，也不表示生产已部署。

## 1. 业务范围

复用原TopicDetail、发布、列表、搜索、图文详情、上传、收藏、点赞和评论，不迁移Compose、不创建第二套帖子。开发基线为rescue_xml的Kotlin、XML、ViewBinding、Activity/Fragment、ViewModel/LiveData、Retrofit/Gson及协程；rescue_compose不纳入本次。

新版详情原获取联系方式按钮走：登录→检查本人领养资料→缺失/缺项填写保存，完整则复用→确认申请→申请中→送养人同意→沟通中→申请列表topic.contact_info展示联系。申请不调用GetContact/checkPhone/短信验证码。

旧版GetContact和原帖子getedcontact/contact_info逻辑保留。送养人同意时同事务写原GetedContact（申请人user_id、原topic_id、topic_type=1）；新旧获取共享同一记录。申请状态独立，已有历史获取记录不会自动把applying变成communicating。无需独立V2联系方式接口。

## 2. 消息及个人页布局：本次主要变化

消息页面从上到下固定为：**系统消息、我的申请、收到的申请、点赞、收藏、评论**。原四Tab不变。

- “我的申请”进入申请人列表，独立显示本人提醒的未读角标。
- “收到的申请”进入送养人列表，独立显示收到申请相关提醒的未读角标。
- 不再显示独立“申请通知”入口或要求用户先进入通知页再找申请。
- 个人页只保留“领养资料”；移除“我的申请”“收到的申请”两个入口。
- 本人原帖“管理申请”仍可进入收到的申请列表，带topic_id过滤。
- 消息Tab角标合计旧系统/点赞/收藏/评论未读及两类申请未读，不重复计算申请总数和分类数。

iOS的App角标在Android对应系统通知/启动器支持的桌面角标；应用内消息Tab角标本期必须实现。不能承诺所有Android启动器均支持同一数字角标，也不为此改变申请或已读规则。

## 3. 按现有Android文件修改

下列路径相对 `rescue_xml/app/src/main`，java/...表示 `java/com/rescue/flutter_720yun/`。现有业务及广告改动继续保留，不重建页面。

| 文件 | 已有行为及本次调整 |
| --- | --- |
| java/.../message/viewmodels/MessageViewModel.kt | 当前存在category=adoption的独立通知类别；改为system/my_applications/received_applications/like/collection/comment，分别维护两类申请未读 |
| java/.../message/fragment/MessageFragment.kt | 当前adoption打开AdoptionNotificationsActivity；改为两个申请Activity，继续按category路由，旧like/collection/comment映射1/2/3不变 |
| java/.../user/fragment/UserFragment.kt、res/layout/fragment_user.xml | 当前有adoptionProfileEntry/adoptionMyApplicationsEntry/adoptionApplicationsEntry；只保留资料入口，移除另外两个绑定及布局 |
| java/.../adoption/activity/AdoptionMyApplicationsActivity.kt | 已继承收到申请页且isMyApplications=true；继续复用页面，增加本人未读提醒顶部区域 |
| java/.../adoption/activity/AdoptionApplicationsActivity.kt | 已有筛选、列表及底部刷新/加载更多按钮；补未读提醒、转圈、空页/失败点击重试，原帖进入带topic_id |
| java/.../adoption/viewmodels/AdoptionApplicationsViewModel.kt、AdoptionMyApplicationsViewModel.kt | 已有独立申请分页；增加按身份/帖子筛选的提醒分页、分类未读和单条已读状态；通知分页与申请分页分开 |
| java/.../adoption/adapter/AdoptionApplicationAdapter.kt、adoption/fragment/AdoptionApplicationSheet.kt | 保持资料快照、对应帖子、同意/结束/完成及联系展示；不把通知点击直接当作申请操作 |
| res/layout/adoption_list_content.xml | 当前仅筛选、状态文本、RecyclerView及底部按钮；增加顶部提醒列表及独立Loading/Empty/Error区域 |
| java/.../network/AdoptionService.kt、adoption/repository/AdoptionRepository.kt | 现有notifications只传page/size；新增可选role、topic_id、unread参数，未读查询支持分类字段 |
| java/.../adoption/models/AdoptionModels.kt | AdoptionNotification增加kind/role（保留created_at），UnreadCount增加两类计数；兼容旧后端时新增字段可为空，不默认0掩盖缺失 |
| java/.../MainActivity.kt | 增加或调整消息Tab badge，来源为旧未读合计加申请总未读；移除账号切换后的旧角标 |
| java/.../message/activity/AdoptionNotificationsActivity.kt及对应ViewModel | 不再作为独立入口；可复用通知加载逻辑，已有外部Intent可转发到对应申请列表，转发后仍鉴权 |

现有 `AdoptionProfileActivity/AdoptionProfileViewModel`、`HomeDetailActivity/HomeDetailViewModel`、`AdoptionDetailFlow`、`AdoptionServiceCreator`、`AdoptionViewModel` 和 `AdoptionActivity` 的V2流程继续复用。新顶部提醒项可新增 `AdoptionReminderAdapter` 与item XML；不要求再创建一套个人资料、申请或通知Activity。

当前AdoptionServiceCreator已有独立JSON/Bearer调用，Repository已检查账号revision且取消底层请求，不重复写成待新建。继续使用这些实现，不将新REST请求改成旧FormUrlEncoded/body token，不把非2xx403统一当登录失效。

## 4. 两类申请列表和顶部提醒

### 4.1 申请条目

我的申请用GET adoption-applications/mine；收到的申请用GET adoptions/mine/applications，本人原帖进入附topic_id。按申请中/沟通中/已结束筛选并分页，刷新和追加沿用同一筛选；已存在“全部”选项可以保留。身份切换不复用前一账号数据。

卡片带原帖缩略图、摘要、topic_id及原详情入口，申请人称呼/头像、年龄、城市区县、住房、工作状态、申请时间和状态；经验及statement在面板展示。使用profile_snapshot，标注“申请时资料·用户填写”，不能请求他人最新profile。

我的申请在topic.getedcontact=true且contact_info非空时直接显示/复制送养人联系。原帖完成/关闭/删除后联系为空；单份申请结束不清已获取记录。收到的申请不展示申请人电话；不能代入申请人身份读取帖子联系。删除帖及清理后的空资料显示占位。

### 4.2 顶部未读提醒

两个列表顶部独立展示对应当前账号/身份的未读提醒，显示正文和created_at格式化时间。收到列表从原帖进入时提醒也限定topic_id；跨帖入口不加topic_id。申请状态筛选不应丢弃其他状态的未读提醒，提醒仅按身份/帖子及未读条件筛选。

点击提醒：调用POST adoption-notifications/{id}/read→成功后展示该提醒正文、移除该条未读提醒→刷新对应类别、消息Tab及桌面角标（支持时）。失败保留提醒和计数，允许重试；连点禁用提交，已读接口幂等。只标记点击的一条，不批量读掉全部，不要求因此打开原帖或自动同意申请。

进入列表、刷新、查看申请详情均不自动标记未点击通知已读。后端application.unread/read_at是旧申请查看字段，不是两类通知角标来源；只使用Notification.read及未读统计。

通知和申请条目各自分页，各自有has_more、加载中及失败重试。分页过程中不能因当前页无某类提醒就把该类别角标清零；角标来自服务端统计，头部按需加载更多。按通知id合并防重复，多页历史申请及终态申请仍参与旧后端归类。

### 4.3 加载、空页及错误

Android沿用现有项目的空数据文字、字号、颜色及居中布局，效果对齐iOS，不引入DZNEmptyDataSet。初次请求显示ProgressBar转圈；成功空数据显示“暂无数据 / 请点击重试”，点击空区域重试；失败显示明确提示及点击重试。不能用当前底部“刷新/加载更多”按钮代替空白页。

顶部有未读提醒但申请条目为空时仍展示提醒，只在条目区域显示空状态。通知失败不覆盖已加载申请，申请失败不清已加载提醒；保留最后一次成功计数并显示重试。追加失败保留已有页，重复重试不重复条目。刷新及追加可沿用现有Refresh组件/滚动触发；加载更多按钮如保留只用于非空分页。

## 5. 通知接口与分类规则

以下是本次后端已补充的兼容字段/参数。部署前确认服务端版本，不能以本地源码存在推断线上已支持。

| 调用 | 参数或返回 |
| --- | --- |
| GET /api/v2/adoption-notifications | page/size、unread=true、可选role=mine/received及topic_id；items带id/kind/role/message/topic_id/application_id/target/read/created_at |
| GET /api/v2/adoption-notifications/unread-count | 无筛选时返回全账号unread_count、mine_unread_count、received_unread_count；可选role/topic_id用于对应列表/帖子统计 |
| POST /api/v2/adoption-notifications/{id}/read | 仅当前收件人，单条幂等已读；失败不减少前端角标 |

role=mine表示当前账号是该申请的申请人；received表示当前账号是该通知收件人但不是申请人。**不能按kind或target判断身份**，例如送养人的结束提醒target仍可能为topic_detail。分类完全由后端关联身份判断，不接受请求体user_id指定其他账号。

列表响应同时有unread_count、mine_unread_count、received_unread_count，统计涵盖当前role/topic_id范围全部未读记录，不依赖返回页数；两类之和等于该范围unread_count。无筛选接口保留原总数含义，新字段不影响已有iOS调用。查询不会写Notification.read_at。

建议Android消息页用无筛选unread-count直接取两类角标；本人列表顶部用role=mine&unread=true；收到列表用role=received&unread=true，可带topic_id。原帖筛选只影响该列表提醒，不把过滤后的局部计数覆盖消息页的全局角标。

旧部署兼容与最新iOS文档一致：若响应没有role/分类计数，前端分页查询unread=true全部通知，并分页查询本人的全部“我的申请”记录，按application_id是否属于本人拆分；其余归收到的申请，单帖再按topic_id过滤。不能只查第一页、只查applying或直接用当前可见申请页。老服务会忽略新query参数，缺role时不能误把未筛选响应当已筛选。归类完成之前不显示猜测的分类数。新接口上线后不必每次拉全部申请历史。

## 6. 继续沿用的申请与资料接口

均为/api/v2前缀，GET query、写请求JSON、现有token的Bearer头。错误在data.error_code/data.errors；正常HTTP200/code200。topic_id为原帖ID。

| 动作 | 方法及路径 |
| --- | --- |
| 本人资料查询/保存 | GET/PUT adoption-profile/me |
| 本人申请列表 | GET adoption-applications/mine |
| 收到的跨帖申请 | GET adoptions/mine/applications |
| 单帖申请列表 | GET adoptions/{topic_id}/applications |
| 原详情本人申请状态 | GET adoptions/{topic_id}/application-state |
| 新版申请按钮 | POST adoptions/{topic_id}/applications，profile_version、statement及Idempotency-Key |
| 申请详情 | GET adoption-applications/{application_id}，topic中包含getedcontact/contact_info |
| 同意/结束 | PATCH adoption-applications/{application_id}，action及申请version，end_note选填 |
| 完成/关闭 | POST adoptions/{topic_id}/status，confirmed及workflow version；完成带实际application_id |

资料四组必填：age整数1–120，城市/区县，住房，工作状态；经验选填最多200字，不收精确地址/公司/收入/新手机号。housing_type枚举owned/whole_rent/shared_rent/with_family/dormitory/other；employment_status为employed/self_employed/student/not_working/retired。

首次GET profile=null进入资料页，保存不自动申请；已有完整资料直接确认分享后申请。已有资料PUT带version，缺项PROFILE_REQUIRED补齐，变更PROFILE_CHANGED重新查询确认。申请保存快照，修改只用于新申请。资料地区选择与首页同城local_city隔离。

一帖多人申请/沟通；结束单份不影响其他申请，完成选实际接宠申请，其余有效申请联动结束。同意即沟通中且获取记录同事务生成，旧获取记录仅代表获取、不改变申请状态。原获取/帖子接口保持兼容；有申请历史的完成/重开及删除继续服务端联动。

## 7. 导航、推送及账号切换

现有AdoptionActivity/Activity Result登录返回、UserManager.sessionRevision账号保护继续复用。登录失败/取消不自动提交；返回详情、消息/申请列表出现及App前台刷新状态、提醒和未读；状态更新成功触发刷新，不增加聊天/长连接。

推送维持资源导航：新申请→发布人管理，同意→申请人原详情刷新，结束→查看对应申请结果；正文/载荷不带联系或profile。使用topic_id/application_id，不凭target判断身份，重新鉴权；非法ID停止，不能默认跳帖子1。Android现有无完整业务推送接收路由时需另接渠道，站内通知不依赖通知许可。

退出/切号清资料、获取联系、列表、提醒、角标、待跳转和分页任务；旧账号请求不能写入新账号UI。提醒及申请异步任务按身份/topic_id检查结果，筛选改变丢弃旧请求。新返回数据不放Intent、SharedPreferences、SavedStateHandle或调试日志中的私人缓存。

## 8. 联调验收

1. 消息顺序为系统消息、我的申请、收到的申请、点赞、收藏、评论；个人页仅保留资料，无独立申请通知入口。
2. 两类提醒各自位于对应列表顶部，正文/时间齐全；单帖提醒按topic_id隔离，同一账号兼任申请人/送养人不混类。
3. 送养人结束提醒即使target=topic_detail仍归收到的申请；覆盖多页通知、多页和终态申请，分类数不只计首页。
4. 点击一条成功只减少对应类别，重试幂等；失败保留提醒，进入/刷新/看详情不自动读通知。
5. 两类未读合计与服务端总数一致；消息Tab旧未读加申请总未读一次，单帖局部统计不覆盖全局；无许可仍站内可处理。
6. 首次转圈、空数据“暂无数据 / 请点击重试”、失败点击重试；顶部有提醒时不被空页挡住，追加失败保留列表。
7. 资料首次填写/复用、无短信申请、多人沟通、完成联动及topic.contact_info保持原V2规则，原发布/列表/广告回归。
8. 越权通知/帖子筛选拒绝，退出切号清数据与角标；后台/旋转/返回不重复提交，日志不含私人数据。

建议先调整AdoptionService/DTO/Repository，再调整MessageViewModel及两个列表的提醒状态，移动入口、补空页，最后检查角标和推送/登录返回。此次文档更新不代替Android编码、构建或真机验收。

申请权限无总开关：详情使用application-state的can_apply，帖open且未删除/完成、当前用户非发布者且没有有效申请即可申请。缺资料先填写；can_apply=false不能直接当作帖子结束，整帖状态看workflow_status。

## 9. HTTP 状态、业务 code 与错误处理（两端统一）

### 9.1 解析顺序与网络层边界

新 V2 REST 使用 `Authorization: Bearer <token>`，响应包为 `code/message/data`。正常结果是 **HTTP 200 且 code=200**；只有二者同时成功才能弹“申请成功”、更新操作成功状态或扣减通知未读数。重复申请返回已有申请也属于成功，直接使用服务端 application_id/status/result，不新增本地记录。

网络层必须在非 2xx 时继续解析 JSON 响应体，保留 HTTP 状态、code、message、data.error_code 和 data.errors，交给具体页面处理。不能只把非 2xx 转成“网络失败”，不能用 HTTP 200 代替业务成功判断，也不能把所有失败统一当登录失效。原 V1 的 HTTP 200/业务 code 规则保持在原网络封装中，不能全局改成 V2 的规则。

优先按 HTTP 状态和业务 code 判断是否成功；失败时用 data.error_code 分流，message 用于提示，不通过匹配中文 message 判断 BLACKLISTED/PROFILE_REQUIRED 等有明确标识的错误。不是每个错误都带 error_code；缺少时按 HTTP 状态与 code 做通用处理。HTML、空响应或 JSON 解析失败显示“服务暂不可用，请稍后重试”，不展示原始 HTML/堆栈，不伪造成功、资料完整或帖子结束。

Android：在 AdoptionRepository/AdoptionServiceCreator 中解析 Retrofit Response.errorBody()，或 HttpException 携带的 response 错误体，保留状态与错误包。按现有 sessionRevision 校验当前账号后交给 ViewModel 更新提示、字段错误和按钮，不把所有 HttpException 都转成登出。

### 9.2 新 V2 状态码处理表

| HTTP / code | 场景 | 客户端必须执行的处理 |
| --- | --- | --- |
| 200 / 200 | 查询或写入成功，包括返回已有有效申请 | 使用服务端数据刷新；申请成功才显示成功弹窗。资料 profile=null 或 is_complete=false 也是查询成功，提示填写资料，不跳登录。列表 items=[] 显示空状态，不视为接口失败。 |
| 400 / 400 | 字段、枚举、分页、确认或幂等键格式无效 | 停止本次提交、恢复按钮；展示 data.errors 对应字段错误或 message，保留表单供修改。缺少 Idempotency-Key 是请求实现错误，不自动重试、不跳登录或验证码。 |
| 401 / 401 | 未登录、token 无效或失效 | 清理失效登录态及私人缓存，按现有登录流程处理，避免并行失败弹出多次登录。保留经校验的原 topic_id 导航目标；登录成功后重新查询状态/资料，用户再次确认才提交，取消登录不提交。 |
| 403 / 403，error_code=BLACKLISTED | 本人手机号或邮箱命中 black_status=1/3 的黑名单 | 展示服务端“拒绝访问”，停止申请及本次重试，不弹成功、不清 token 后循环登录、不跳绑定手机号/短信验证、不调用旧 GetContact 绕过。已有申请或幂等重试也可能被当前黑名单拦截，不能继续使用旧授权或缓存执行操作。 |
| 403 / 403，其他或无 error_code | 账号不可用、越权、申请自己帖子、非发布人操作、查看他人申请/提醒 | 提示 message，停止当前操作，隐藏本次无权查看的私人内容；按上下文重新查询可用状态/列表或返回可访问页面。403 本身不代表登录失效，不统一登出。账号失效时不要继续显示缓存联系。 |
| 404 / 404，有效业务 JSON | 帖子、申请或通知不存在/不可用 | 详情显示资源不可用并禁用写操作；列表移除或刷新失效项，清除该资源缓存联系。通知已读失败不能当成功提前扣数，应重新拉取服务端统计。 |
| HTTP 404，非业务 JSON或无法确认路由已部署 | 路径错误、已移除接口、部署/代理未匹配 | 显示服务不可用并记录脱敏诊断，核对路由；不能据此把某帖子标记删除。独立 V2 contact 路由已经移除，联系只从本人申请列表/详情 topic.contact_info 读取。 |
| 409 / 409 | 资料、操作版本、幂等内容或帖子状态冲突 | 按下表分流；通用处理为展示 message 并重新读取相关资料/申请/帖子状态，不强制登录、不继续用旧版本提交。 |
| 405 / 405、415 / 415 | 方法或 Content-Type 不匹配 | 提示请求暂不可用，检查 GET/PUT/PATCH/POST 及 JSON Content-Type。属于请求协议问题，不重试成旧 body token/Form 请求、不跳登录。 |
| 5xx 或无法解析的响应 | 服务内部或网关异常 | 结束加载、保留可安全展示的已加载列表与表单，提示稍后重试；写入结果视为未知，先查询最新状态，不能假定成功或失败后立刻重复创建。 |

HTTP 与业务 code 不一致时不能当成功：保留两者用于脱敏诊断，按拒绝/失败处理并提示；旧接口 HTTP200/code202 等由旧业务规则处理。未知 code/error_code 展示可用的 message 或通用失败提示，停止写入，不能默认跳登录。

### 9.3 409 的细分规则

| data.error_code | 处理要求 |
| --- | --- |
| PROFILE_REQUIRED | 使用 missing_fields 提示缺项，再由用户选择进入本人领养资料页。保存成功后只刷新，不能自动提交申请；用户再次点击申请并确认。 |
| PROFILE_CHANGED | 重新 GET 本人资料，获取最新 version，保留本地尚未保存的输入供用户核对。更新资料时不静默覆盖草稿；申请时重新展示最新资料并让用户确认，再用最新 profile_version 提交。 |
| VERSION_CHANGED | 重新 GET 申请详情或 application-state，区分申请 version 和流程 workflow_version。更新按钮与操作对象后重新确认，不能只替换 version 自动重发“同意/结束/完成”。 |
| IDEMPOTENCY_CONFLICT | 同一 key 已用于不同请求内容；先查询当前申请/状态，停止自动重试。确认是新的用户操作后才生成新 key；不能每次收到冲突就自动换 key 连续提交。 |
| UPGRADE_REQUIRED | 原完成/重开入口不能直接处理已有申请历史。提示后进入收到的申请/管理申请，由送养人选择实际申请人完成；不能重试旧完成接口绕过联动。 |
| 无或未知 error_code | 展示 message，重新查询相关状态；可能是帖子已结束、申请已结束、所选申请不在沟通中或帖子联系未填写。根据刷新结果恢复合法操作，不仅凭 409 推断整帖已结束。 |

新版被拒绝示例：

```json
{"code":403,"message":"拒绝访问","data":{"error_code":"BLACKLISTED"}}
```

新版缺资料示例：

```json
{"code":409,"message":"请先补齐领养资料","data":{"error_code":"PROFILE_REQUIRED","missing_fields":["age"],"version":1}}
```

这些示例的 HTTP 状态分别为 403/409，不是 HTTP200。禁止硬编码把 202 当作新版黑名单响应，禁止假定所有403都有 BLACKLISTED。

### 9.4 原 V1 业务 code 的兼容处理

仅适用于保留的原接口，通常 HTTP200；新版申请按钮不调用原 GetContact，因此不能把以下旧规则接入申请失败后的回退路径。

| 原业务 code | 处理 |
| --- | --- |
| 200 | 按原接口成功结构消费，不套用新版 items/profile 结构。 |
| 201 / 204 | 参数错误/缺少参数；提示并检查原字段及表单编码，不跳登录。 |
| 202 | 拒绝访问，旧黑名单拦截；提示 message 并停止，不进入手机号验证或反复重试。 |
| 203 | 原接口方法错误；检查请求方法，不跳登录。 |
| 401 | 原 token 认证失败；走现有登录流程，登录后刷新，不自动提交申请。 |
| 404 | 原查询/接口不存在；展示 message 并核对资源或路由，不能泛化为账号失效。 |
| 209 / 210 | 原未绑定/未验证手机号；只在原接口所属的旧业务中保留现有处理。V2 申请不产生或消费这两种验证码门槛，不允许收到未知响应就跳旧验证页。 |
| 300 / 500、其他未识别 code | 保留原业务已有处理；没有明确处理时展示 message 或通用失败并停止，不按成功或登录失效处理。 |

### 9.5 重试、缓存与验收

- 网络超时/断网属于传输失败，没有可依赖的业务 code。恢复提交按钮，先重新查询服务端状态；同一写入操作的重试保持同一 Idempotency-Key 和请求内容，新操作由用户确认后生成新 key。
- 请求完成后先核对当前账号及页面/筛选上下文；旧账号返回的成功或失败都不能更新新账号页面，不能因旧账号401登出新账号。
- 被拒绝或状态未知时不弹申请成功，不增加本地申请/通知数；通知已读只在 HTTP200/code200 后扣减并刷新统计。进入/刷新列表仍不自动将通知已读。
- 验收分别覆盖 HTTP200/code200、profile=null、400字段错误、401失效token、403 BLACKLISTED、403越权、404资源与路由错误、五类409、非JSON 5xx/超时及未知code；黑名单手机号/邮箱、状态1/3、已有申请重复提交均应停止，状态0/2且资料完整可正常申请，无需验证手机号。
