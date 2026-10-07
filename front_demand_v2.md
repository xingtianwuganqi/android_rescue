# Android V2 开发需求：领养流程、消息与激励视频曝光

修订日期：2026-10-04。业务基线：`/Users/jingjun/Desktop/Githup/LaveCat/front_demand_v2.md`；Android工程：`/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml`。后端契约：`/Users/jingjun/Desktop/Githup/rest_framework/API_v2.md`，产品基线：同目录 `Demand_v2.md`。推广接口基线：`/Users/jingjun/Desktop/Githup/rest_framework/API_promotion.md`；客户端参考：`/Users/jingjun/Desktop/Githup/LaveCat/demand_ad.md`。

已核对当前Android代码：V2资料、申请列表、申请详情及网络模块已经存在，本文件描述在这些模块上继续调整的要求。本次仅更新 Android 开发需求文档，不表示已完成 Android 新流程/推广编码、构建、真机验收或生产部署。第 3 节为原有改造清单，2026-10-04 的接口与实现核对补充见第 10、11 节；发生差异时以后两节及最新后端契约为准。

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
| 同意/拒绝/结束/放弃 | PATCH adoption-applications/{application_id}，action及申请version；拒绝使用action=end、end_note="拒绝沟通申请"，没有独立reject动作 |
| 完成/关闭 | POST adoptions/{topic_id}/status，action、confirmed及workflow version；完成带实际application_id和独立Idempotency-Key |

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


## 10. 领养流程变更与接口调用细节（2026-10-04）

### 10.1 详情申请流程

登录后进入原 HomeDetailActivity 立即 GET `/api/v2/adoptions/{topic_id}/application-state`；登录返回、从资料/申请页返回、回前台、写操作成功后重新查询。点击申请也先读取最新状态：只有 `workflow_status=open && can_apply=true` 才继续 GET `/api/v2/adoption-profile/me`。`open/adopted/closed` 分别为仍在送养/完成领养/结束领养；`can_apply=false` 或单份申请 `ended` 不能代替整帖结束判断。缺失或未知流程状态显示状态暂不可用并允许重试，不发送写请求。未登录沿用原 `is_complete` 展示，登录后切换到 V2 状态。

- 资料不存在/不完整：使用项目现有自定义确认弹窗，正文“没有领养资料，需要填写后再申请。”，按钮“取消”“去申请”；后者打开 AdoptionProfileActivity。保存只返回并刷新，用户再次点击申请，不自动弹须知或提交。
- 资料完整：复用原领养须知内容及《领养说明》链接，按钮“取消”“继续申请”。取消或查看说明不申请；继续申请先关闭弹窗，再提交。Android 使用现有 XML/Dialog 组件实现，不引入 iOS QMUIDialogViewController。
- POST `/api/v2/adoptions/{topic_id}/applications`，Bearer + JSON + `Idempotency-Key`，请求体 `{"profile_version":2,"statement":""}`。不额外输入申请说明，不增加第二次系统确认。成功仅限 HTTP200/code200，使用项目通用自动消失提示“申请成功，等待送养人反馈。”，刷新状态/列表/提醒。
- 一次用户确认生成一次 key（1–64 字符，建议 UUID），网络重试保持原 key 和原内容；服务端返回已有有效申请也使用其真实 ID/status/result。资料变化/缺项按照第 9 节处理，重新确认后才发起新的操作。
- 全流程不调用 GetContact/checkPhone/短信验证码。历史获取记录可以用于展示联系，但不跳过创建申请，不把旧获取直接当作 communicating。

application-state DTO 保留 `topic_id/workflow_status/workflow_version/contact_version/can_apply/can_manage/application_id/application_status/application_result/application_version/contact_authorized`，兼容后端流程 `version` 别名。发布人响应的 application_id 为 null，管理数量为 `application_count/unread_application_count`，不能据此伪造本人申请。资料版本、申请版本、流程版本分开使用。

### 10.2 收到的申请：同意、拒绝、结束、完成

收到的申请列表在卡片帖子信息下方靠右显示操作，不再要求先点申请人打开操作面板：`applying` 为“同意沟通”“拒绝申请”，`communicating` 为“结束沟通”“完成送养”，`ended` 无操作。完成只在帖子仍可用且开放时提供。卡片保留申请时资料、申请时间、帖子缩略图/摘要及原帖入口；点击申请人信息不弹资料操作窗，卡片中不另放“取消”按钮。

所有操作先 GET `/api/v2/adoption-applications/{application_id}`，验证目标帖子、权限及最新状态；自定义弹窗取消只关闭，确认先关闭再请求，提交期间禁用重复点击。

| 操作 | 确认文案与请求 |
| --- | --- |
| 同意沟通 | applying 时提示“同意沟通将会把联系方式显示给申请人，确认沟通吗”；PATCH `/api/v2/adoption-applications/{application_id}`，`{"action":"communicate","version":3}`，version 为最新申请 version |
| 拒绝申请 | applying 时提示“确认拒绝该申请人的沟通申请吗？”；PATCH 同一路径，`{"action":"end","version":3,"end_note":"拒绝沟通申请"}` |
| 结束沟通 | communicating 时确认后 PATCH 同一路径，`{"action":"end","version":4,"end_note":"选填，最多200字"}`；只结束该份申请，不关闭整帖 |
| 完成送养 | 先确认该申请仍为 communicating，提示“确认该申请人已实际接走宠物并完成领养吗？确认后，这条送养帖子将结束，其他进行中的申请也会自动结束。”；确认后重新 GET 该帖 application-state，校验 can_manage=true、workflow_status=open 并取最新 workflow_version，然后 POST `/api/v2/adoptions/{topic_id}/status`，`{"action":"complete","application_id":501,"confirmed":true,"version":7}`，携带独立 UUID Idempotency-Key |
| 关闭整帖 | 发布人确认后 GET 最新 application-state，POST 同一 status 路径，`{"action":"close","confirmed":true,"version":7,"end_note":"选填"}`；version 为流程版本，不选择或伪造完成申请 |

上述数字仅为示例。PATCH 的 version 来自申请详情；status 的 version 来自 workflow_version（兼容流程 version）。不能把申请 version 用于完成送养，也不能在 409 后只替换版本自动重发。PATCH 可携带 Idempotency-Key 记录重试；完成操作应固定本次 key、对象及请求内容。超时先查最新状态，再决定是否以原请求恢复。

同意成功在同一后端事务写 communicating 和原 GetedContact，通知申请人；多人可同时沟通。申请人刷新本人列表/详情，通过 `topic.getedcontact/contact_info` 展示送养人联系，无独立 V2 contact 请求。完成由后端将选中申请置为 ended/completed、其他有效申请置为 ended/not_completed，整帖 adopted；关闭为 closed，不产生 completed 申请。客户端不能自行逐份结束、直接写旧 is_complete 或回退旧完成接口。

拒绝没有独立 action=reject，也没有新增 rejected 状态。仅同时满足 `status=ended && result=not_completed && end_reason=owner_ended && end_note="拒绝沟通申请"` 才显示“被拒绝”，仍归已结束筛选。普通结束沟通、申请人放弃、整帖关闭不能显示被拒绝；未知/缺失原因沿用通用结束文案。AdoptionApplication DTO 增加可空 `end_reason`，双方列表和详情共用同一判断。

成功刷新申请列表、状态和提醒/未读计数；失败展示后端原因并刷新合法操作。进入列表或查看详情仍不自动将通知已读。

### 10.3 我的申请、联系复制与 Android 改动

我的申请卡片不再通过整条点击弹操作 alert；帖子区域进入原详情。applying 显示“放弃申请”，communicating 时“复制联系方式”“放弃申请”并排靠右（联系不可用时隐藏复制），ended 不显示放弃。放弃弹窗“确认放弃申请吗？”，取消关闭；确认关闭后 GET 最新申请详情，再 PATCH action=end、最新申请 version，不要求填原因。

复制前重新 GET 申请详情确认当前账号及 `topic.contact_info`，成功使用通用提示“已复制联系方式”；联系区用主题色文字/浅色背景、复制按钮用主题色背景/白字圆角。整帖完成/关闭/删除后清空显示和缓存联系；单份申请结束保留获取事实。收到的申请不能用申请人身份读取私人联系。

图片兼容 preview_img 字符串（逗号分隔取首个有效值）和图片数组，优先 V2 图片，无图/失败可通过原帖详情补取 imgs，按 topic_id 去重请求；失败保留占位。Android 当前 TopicSummary.preview_img 为 String?，需使用 Gson 自定义适配或规范化 DTO，避免数组导致整页解析失败。时间解析 ISO8601 后按设备时区显示 `yyyy-MM-dd HH:mm:ss`。顶部无提醒时隐藏提醒容器/标题和额外间距。

本次核对 Android 当前 AdoptionService 的 action/topicAction 尚无 Idempotency-Key 头参数；补可选/对应必传参数并由 Repository 传入，TopicAction 增加可空 end_note，AdoptionModels 增加 end_reason、流程 version 别名及管理未读数量。AdoptionApplicationAdapter、AdoptionApplicationSheet、两个申请 ViewModel、AdoptionDetailFlow 和 HomeDetailActivity 共用同一状态/操作逻辑，避免列表与详情出现不同拒绝判定。原 Sheet 可继续用于必要的详情展示，不能保留与新卡片重复的强制操作入口。

## 11. 观看视频增加曝光：Android 技术细节

### 11.1 规则与入口

后端契约以 API_promotion.md 为准。最新后端已取消推广启用开关，旧 `REWARDED_TOPIC_BOOST_ENABLED` 不生效；不能照搬 demand_ad.md 的旧开关说明。部署新接口和迁移后按服务端资格决定入口。推广时长固定 3600 秒；默认每账号每日成功 3 次，可由服务端配置，`remaining_today=null` 表示不限，Android 不写死额度。额度按 Asia/Shanghai 自然日计算，显示时间按设备时区。

同一 `(topic_id,user_id)` 永久复用同一推广记录；首次生成，新的观看奖励成功只更新 update_time，create_time 和 promotion_id 不变。`effective_until=服务端本次update_time+3600秒`，不累加旧剩余时间。10:00 成功、10:40 再次新观看成功，结束为11:40。同一次观看重试不续期、不重复扣额度。不修改原帖内容、发布时间、联系方式、申请流程或 is_complete。

- 我的发布：可用本人帖提供“观看视频，增加曝光”；推广中为“观看视频，延长推广”，允许再次观看，显示“推广中 · 截至HH:mm”/“推广已结束”。原 authpublishlist 批量摘要用于卡片，点击前重新查询资格。
- 发布成功：原发布成功后以结果页展示“发布成功”“观看视频，增加曝光”“查看帖子”“完成”，替代成功后固定 1.5 秒自动退出。仅使用后端返回真实 topic_id；缺少/非法 ID 保留发布成功及退出功能，隐藏推广入口，不能取最近帖子猜目标。草稿清理、成功通知及回调只执行一次，不观看或播放失败不影响发布结果。
- 个人主页领养列表进入本人详情：查询 promotion-state，在“管理申请”上方显示可用推广按钮，复用 my_posts 场景；回前台/返回/成功后刷新，底部随入口显隐调整高度。本人帖子结束后保留浅色禁用状态按钮：workflow_status=adopted 显示“完成领养”，closed 显示“结束领养”，不播放广告；列表需要查询流程状态区分两者，不能只看旧 is_complete。

播放前用项目自定义弹窗说明“观看视频后，该帖子将获得一小时优先展示。再次观看成功将重新计算推广时间，是否继续？”，按钮“取消”“观看视频”，均先关闭弹窗。仅用户主动选择才播放，不给申请/获取联系方式增加广告门槛。

### 11.2 新 REST 接口与 DTO

复用 AdoptionServiceCreator 的独立 JSON/Bearer 与错误解析能力，为推广建立独立 PromotionService/Repository/DTO；身份从当前账号获取，不用旧 Form body.token 调新接口。成功要求 HTTP200 且 code200。

| 接口 | 请求/响应 |
| --- | --- |
| GET `/api/v2/topics/{topic_id}/promotion-state` | Bearer，无请求体，仅本人可用帖子；data 为 topic_id、can_promote、reason、duration_seconds、remaining_today、promotion、server_time、promotion_version |
| POST `/api/v2/topics/{topic_id}/promotion` | Bearer + JSON + Idempotency-Key；body 仅 reward_attempt_id、scene；data 为 topic_id、promotion_id、is_active、create_time、update_time、effective_until、duration_seconds、created、remaining_today、server_time |

资格响应示例（无推广记录）：

```json
{"code":200,"message":"成功","data":{"topic_id":123,"can_promote":true,"reason":null,"duration_seconds":3600,"remaining_today":3,"promotion":null,"server_time":"2026-10-04T04:00:00+00:00","promotion_version":null}}
```

已有 promotion 为 `{promotion_id,is_active,create_time,update_time,effective_until}`，过期也返回记录。`can_promote=false` 时不播放；reason=DAILY_LIMIT_REACHED 表示额度用完，TOPIC_NOT_OPEN 表示送养结束。推广资格不用 can_apply 判断（发布人本来不可申请自己的帖）；提交仍会重新校验。

观看成功提交示例：

```http
POST /api/v2/topics/123/promotion
Authorization: Bearer <token>
Content-Type: application/json
Idempotency-Key: 0835f97e-8e42-4cb0-92ce-9b587796dd90
```

```json
{"reward_attempt_id":"0835f97e-8e42-4cb0-92ce-9b587796dd90","scene":"my_posts"}
```

scene 仅允许 `my_posts`、`publish_success`，本人详情复用 my_posts，不能新增 topic_detail。头部 key 必须等于 body UUID；不提交客户端 user_id、观看时长、reward=true、客户端时间等额外字段。首次 created=true，续期 false。服务端不接广告平台奖励回调/验签，不提供 start/events/cancel 广告会话接口；当前方案不能由后端证明真实观看，只对账号、所有权、帖子状态、幂等、频率与额度校验。

### 11.3 Android SDK 奖励、协调器与生命周期

现有 `ads/FullscreenAdController.kt` 使用 Taku/AnyThink `ATRewardVideoAd/ATRewardVideoListener`，已有 onReward 和单次 rewardDelivered 防重；复用此 SDK，不照搬 iOS rewardedVideoDidRewardSuccess。现有 SupportActivity 的观看支持功能只展示感谢，不能因接入推广而无目标创建推广。

| SDK 事件 | 推广处理 |
| --- | --- |
| onReward | 唯一可以记录奖励并进入提交的事件；绑定原 attempt/account/topic/scene，重复回调复用同一请求 |
| onRewardedVideoAdPlayEnd | 播放结束，不代表奖励，不调用推广 |
| onRewardedVideoAdClosed | 关闭不发奖；若 SDK 在关闭后补发 onReward，继续处理原观看尝试，不能提前丢弃上下文 |
| onRewardedVideoAdFailed / onRewardedVideoAdPlayFailed | 加载/播放失败，不发奖、不调用推广，恢复入口并提示 |

新增共用 `RewardedTopicPromotionCoordinator`，我的发布、成功页及本人详情共用，状态为 `idle → preparing → playing → reward_observed → submitting → succeeded/retry_pending/failed/cancelled`。顺序：主动确认 → 查询资格 → 为本次观看生成并绑定 UUID → 加载/播放 → 收到真实 onReward → 持久化待提交 → POST → 后端成功 → 再查当前推广状态并刷新。准备、播放、提交阶段禁止重复点击；App 级协调器与 TakuAds 全屏状态共同防止跨页面/支持页同时播放，不仅靠单个 Activity 布尔值。

当前控制器的 update() 在 Activity destroyed 时丢回调，onDestroy 会解除监听/销毁广告；直接把业务提交写在 Activity 的 onReward 闭包不够。改为类型化事件与固定观看上下文，奖励记录/恢复归账号绑定的协调器或应用级 Repository。旋转/导航关闭时不得把旧事件转交给新 attempt；使用 Activity 仅呈现广告，避免全局单例持有 Activity 泄漏。已观察到的奖励在 Activity 销毁前可靠落盘，业务提交不依赖原页面继续存活；关闭后晚到奖励仍归原尝试，没有真实奖励记录的播放不能补发奖励。

### 11.4 幂等、奖励持久化与失败处理

同账号同 UUID/同 topic/同 scene 重放原成功响应，不刷新 update_time、不扣第二次额度；换目标/场景返回409。服务端成功幂等记录至少保留7天，Android 自动恢复窗口不超过24小时。仅新的主动观看生成新 UUID，SDK回调和重试中不能重新生成。

奖励后待提交记录至少保存 account_id、topic_id、scene、reward_attempt_id、reward_observed_at、retry_until 和重试状态；可用应用私有 DataStore/Room，禁止 token、联系、资料快照或广告隐私信息落盘/日志。此处是允许恢复的最小推广请求记录，不放宽第7节的领养私人数据缓存限制。只有观测到真实奖励才标记为待提交。退出/切号停止原账号任务并移除其 UI 状态，记录按账号隔离，仅原账号再次登录且24小时内恢复；不能用新账号 token 提交旧奖励。超过窗口不自动提交，先查状态并提示，不要求无依据地重新看视频。

| HTTP/code 或 error_code | Android 处理 |
| --- | --- |
| 400：INVALID_REQUEST 或 data.errors | 校正请求，保留诊断，不自动换 UUID 重试 |
| 401：AUTH_REQUIRED | 重新登录；只在原账号恢复，旧请求401不能登出新账号 |
| 403：ACCOUNT_RESTRICTED / NOT_OWNER | 提示账号受限/非本人，停止该目标提交，不回退其他接口 |
| 404：TOPIC_UNAVAILABLE | 刷新列表/详情，停止不可用目标请求；非业务JSON路由404按服务未部署处理 |
| 409：TOPIC_NOT_OPEN / DAILY_LIMIT_REACHED | 提示送养结束/今日额度用完，刷新资格，不提示推广成功，不改推广其他帖子 |
| 409：IDEMPOTENCY_CONFLICT | 停止请求并核对 UUID 的目标/场景绑定，不自动换 key |
| 429：RATE_LIMITED | 按 Retry-After 或 data.retry_after 秒等待，原 UUID 重试；记录下次可重试时间，不能循环连发 |
| 503：RETRY_LATER、其他5xx、超时/断网 | 结果可能已提交，保存待确认记录，原 UUID/原内容重试，不要求再看广告 |

业务拒绝不伪造成功；可重试拒绝保留原请求，在恢复窗口内按当前资格重新校验。查询资格、播放失败、成功重放不消耗每日成功额度；频率限额与成功额度不同。

只有后端200/code200确认成功才清除对应待提交奖励并更新页面。随后 GET 当前 promotion-state，按真实 effective_until 展示；重放结果是历史快照，不能凭它承诺又获得一小时。当前已过期提示“本次推广已处理，当前推广已结束”；状态查询失败提示已处理、需刷新确认。成功通知与延迟提示仍绑定原账号，不显示到新账号页面。

### 11.5 首页/同城曝光与快照分页

沿用原 POST `/api/v1/topiclist/` 和 `/api/v2/addresstopiclist/`。Android 原 HomeService 为 FormUrlEncoded，继续原编码/token/城市字段，追加 `promotion_feed=true`；后续页另传首屏 `snapshot_id`，不将原列表整体改为新REST JSON。旧身份可用 body.token 或 Bearer，同时存在须一致；匿名也支持快照。我的发布、搜索、收藏、消息和申请列表不增加此排序开关。

首页首屏字段示例：`page=1,size=10,promotion_feed=true`。同城还保留原 `address`。响应 data 仍为帖子数组，不是 V2Page.items；每帖增加 `promotion:{is_promoted,label,effective_until}`，普通为 false/null/null。顶层增加 meta，必须在 Retrofit/Gson 解析中保留：

```json
{"meta":{"snapshot_id":"9b24abfe-d1f4-478c-bf3e-42552bc07aac","expires_at":"2026-10-04T04:30:00+00:00","page":1,"size":10,"has_more":true,"total":25,"effective_order":"promotion_update_time","promotion_version":"9b24abfe-d1f4-478c-bf3e-42552bc07aac"}}
```

先按城市筛选，有效推广全部按 `update_time DESC,id DESC` 排在普通帖前，无固定第2/4位、无容量/排队、推广可占多页。普通帖按 `create_time DESC,id DESC` 确定分页范围，再只打乱该页普通帖；混合页推广位置不动。服务端固定顺序，Android 不二次排序/洗牌；原生广告行在业务顺序后插入，分页 size/page/去重只按业务帖子，不按含广告行的 adapter itemCount。

- 快照有效30分钟，后续页保持同一账号、城市、size和 snapshot_id；刷新第一页建立新批次并替换旧数据。筛城市、切号/登录身份改变、size改变或推广成功后主动刷新需清掉旧快照，异步返回按批次校验。
- 快照固定整批顺序、页内随机结果及捕获的推广有效期，续期不延长旧快照窗口；新推广/续期排序在刷新第一页后体现，不自动滚到顶部。
- 每页读取仍过滤删除、账号限制与推广失效，不从其他页补齐；不足一页甚至空页不能判断结束，唯一以 meta.has_more 决定继续。total 是原快照条数。按 topic_id 去重并保留服务端顺序，追加失败不清已有页。
- 409 FEED_SNAPSHOT_EXPIRED/FEED_SNAPSHOT_MISMATCH 清快照并从第一页重载，不能把新首屏追加到旧批次。原列表错误解析也要保留业务JSON错误体，支持此分流。
- promotion_version 在列表是快照身份，在单帖状态是最近推广时间，不能混作领养 workflow_version。推广状态以 server_time/is_active/effective_until 为准，本地显示倒计时可用服务器时间偏移，到边界刷新，不能仅用设备时钟授予推广。

### 11.6 原发布/我的发布扩展及 Android 文件清单

POST `/api/v1/releasetopic/` 成功 data 增加真实 topic_id，保留原字段与成功语义。Android 当前 releaseTopic 为 BaseResponse<Any>，需定义 ReleaseTopicResult 或显式解析 topic_id，发布 ViewModel 只发一次成功事件，再路由成功页。POST `/api/v1/authpublishlist/` 每项增加批量 promotion 摘要：promotion_id、is_active、create_time/update_time/effective_until（有记录时）、can_promote、reason、duration_seconds、remaining_today、server_time；无记录为 promotion_id=null/is_active=false，其他时间字段可缺失。列表顺序不变，不能卡片逐条查推广状态；观看前单独 GET 资格。本人详情/结束卡片必要的 workflow 查询是为区分 adopted/closed，不能用 promotion.reason 猜完成结果。

Android 当前 UserTopicViewModel 调用 UserService.userPublishNetworking，路径为 `/api/v2/getuserpublish/`，并非 authpublishlist；不能假定这个响应已经带批量推广摘要。本人“我的发布”需在 UserService 增加 `/api/v1/authpublishlist/` 调用并接入对应分页/DTO，其他用户主页继续原 getuserpublish。若本人主页仍走原接口，详情观看入口单独查 promotion-state，不为该列表逐卡片补推广查询。

路径均相对 rescue_xml/app/src/main/java/com/rescue/flutter_720yun/，下列新增类名为建议，按现有包结构实现即可：

| 文件/模块 | 具体调整 |
| --- | --- |
| network/PromotionService.kt（新增）、promotion/models、promotion/repository（新增） | 两个JSON/Bearer接口、显式DTO、错误包/Retry-After、sessionRevision保护、原UUID重试 |
| promotion/RewardedTopicPromotionCoordinator.kt（新增） | 三个入口共用状态机、App级互斥、固定观看上下文、真实奖励持久化、24小时原账号恢复 |
| ads/FullscreenAdController.kt、ads/TakuAds.kt | 类型化奖励/关闭/失败事件、晚到回调处理、跨页面全屏互斥；保留原支持/插屏/开屏行为 |
| user/fragment/UserTopicFragment.kt、user/adapter/UserTopicListAdapter.kt、user/viewmodels/UserTopicViewModel.kt | 本人我的发布接authpublishlist批量摘要、入口、时间及结束禁用状态；其他用户无推广操作；成功/回前台刷新 |
| home/activity/HomeDetailActivity.kt、home/viewmodels/HomeDetailViewModel.kt | 本人详情推广资格与底部按钮，独立于 can_apply/申请按钮，结束状态来自 workflow_status |
| home/activity/ReleaseTopicActivity.kt、home/viewmodels/ReleaseTopicViewModel.kt、home/models/ReleaseModel.kt | 真实发布ID与一次性成功事件，结果页和主动观看；缺ID正常退出 |
| network/HomeService.kt、network/UserService.kt、home/models 中列表响应/帖子模型 | 保留旧Form接口，增加promotion_feed/snapshot_id、顶层meta、帖子promotion、发布topic_id及我的发布摘要DTO；不要把Any解析后丢meta |
| home/viewmodels/HomeViewModel.kt、LocalListViewModel.kt、对应Fragment/TopicListAdapter | 按服务端顺序展示、快照独立分页、has_more、城市/账号批次隔离、推广标签及广告行插入 |

新增成功页与卡片/XML底部布局沿用项目现有样式。Android 不依赖 iOS 的 ADProject、Moya、QMUI、xy_show；SDK和提示均使用当前Android实现。推广中的非终态资格错误显示真实原因，不一律显示“结束领养”。

### 11.7 联调验收

1. 申请先取最新状态/资料，完整只展示一次领养须知；缺资料保存不自动申请，无手机号验证码或广告门槛。
2. 同意沟通即时写获取记录；拒绝按end+固定备注显示被拒绝，普通结束/放弃不误标；完成用流程版本和实际communicating申请，其他申请由后端联动。
3. 同一新观看成功获得一小时；再次新观看复用推广行且重置一小时。重复奖励/重试不重复续期或扣额，同UUID换帖/场景拒绝。
4. 播放结束、提前关闭、加载/播放失败不提交；广告关闭后晚到奖励、旋转/导航与重复回调仍绑定原观看。支持页奖励不创建帖子推广。
5. 奖励后断网/5xx/进程重启按原账号原UUID恢复，不要求重看；429等待，24小时超期停止自动恢复，切号不跨账号提交或提示。
6. 每日额度用完、非本人、帖子结束/删除、账号受限均停止新观看/提交；结束按钮区分完成领养与结束领养，推广中允许新的合法观看。
7. 发布真实ID用于推广；缺ID不猜目标，不看视频仍发布成功，草稿/通知只清理/触发一次。
8. 首页/同城多页推广、城市过滤、页内普通随机、原生广告插入和has_more均正确；失效造成空页仍继续，快照409从首屏替换，刷新采用新排序。
9. 接口200/code200后再查当前状态，历史重放已过期不宣称新增一小时；联系隐私、消息未读、原发布/浏览/收藏/搜索/广告回归。

实施顺序：补领养DTO/版本与幂等头 → 卡片操作与确认 → 推广接口/奖励协调器与恢复 → 我的发布、发布成功页、本人详情入口 → 首页/同城快照 → 真机SDK及后端联调。上线前确认服务端新版接口、领养及推广迁移已完成；没有广告平台服务端验签或推广启用开关作为本方案前置条件。本次文档核对不代替 Android 编码、构建或真机广告验收。
