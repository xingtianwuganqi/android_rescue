# front_demand 完成度审查 · 2026-10-25

结论：**尚未全部开发完成，也尚未达到全部验收完成。** XML 主流程已有较完整实现；仍有可复现的错误分支、埋点覆盖缺口、依赖后端的未接入事件，以及设备/部署联调缺项。不能以构建成功或现有用例全绿代替全部需求完成。

## 范围与方法

本仓库共有4份 front_demand 文档：front_demand_v2.md、front_demand_find_v3.md、front_demand_v3.md、front_demand_v4.md。当前仓库没有名为 front_demand_analytics_v4.md 的文件；Android 埋点需求对应 front_demand_v4.md。

核对当前工作区（包括原有未提交代码）、需求正文及修订日期、实际业务入口、Repository/ViewModel/Adapter、XML资源与既有测试。旧 QA 中已经注明修复的问题没有重新算作本次缺陷。范围为 rescue_xml；Compose在需求中明确排除，不能因未接入而算本期漏开发。overseas_demand.md不是本次front_demand文档范围。

本次仅审查并增加QA复现和本报告，未修复/重构业务代码，也没有调用生产写接口、执行服务端迁移或核实线上部署。

## 发现的问题（需继续开发）

### F1 · P2 · 推荐已读返回404后仍显示有效卡片【已复现】

需求：front_demand_v3.md 第3/5节、front_demand_find_v3.md 第5节要求资源不可用显示占位；已读失败保持未读，不等于继续把失效资源显示为有效。

路径：已加载公开推荐 → 点击 → read返回HTTP404、error_code=RESOURCE_UNAVAILABLE。MyFindPetViewModel.open在 source.read 失败时不会进入后面的卡片替换；clearUnavailableState为空实现，基类捕获404后不处理目标项。RecommendationAdapter继续根据原 available=true 渲染旧图片、正文和城市。未发生导航，但缺少需求规定的不可用占位。

证据：

- [FindPetViewModels.kt:46](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/findpet/FindPetViewModels.kt:46) read失败后直接进入基类错误分支。
- [FindPetViewModels.kt:62](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/findpet/FindPetViewModels.kt:62) clearUnavailableState不处理目标项。
- [RecommendationAdapter.kt:23](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/findpet/RecommendationAdapter.kt:23) 旧available继续控制公开内容显示。

独立 MockWebServer + 真实 ViewModel 复现保留在 proofs/FrontDemandGapProofTest.kt；测试明确断言当前错误行为（available仍为true、old.jpg和旧正文仍在）。该用例通过表示缺口已复现，不表示需求验收通过。

建议仅对明确的业务RESOURCE_UNAVAILABLE处理本次目标卡片，保留其未读和其他历史；不要将未知路由404误判为全部帖子已删除。

### F2 · P2 · 登录HTTP401/403时失败事件无法正常采集【代码确认】

需求：front_demand_v4.md 第1/3节要求本次登录尝试明确失败时记录 login_failed。

LoginViewModel.loginNetworking/register仍调用业务awaitResp；ServiceCreator.awaitResp的HTTP401/403分支只弹提示、登出及安排页面处理，既没有resume，也没有resumeWithException。因此登录协程不会正常返回到失败分支或catch，直到被取消；取消分支直接抛出而不采集失败。HTTP200内的业务失败及一般网络异常已有记录，但这两种HTTP错误路径未覆盖。

证据：[ServiceCreator.kt:74](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/network/ServiceCreator.kt:74)、[LoginViewModel.kt:53](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/home/viewmodels/LoginViewModel.kt:53)、[LoginViewModel.kt:113](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/home/viewmodels/LoginViewModel.kt:113)。

这是原业务awaitResp遗留缺陷影响新埋点；不是AnalyticsTransport在埋点401时登出。独立埋点传输层静默失败的实现和测试仍然成立。建议使登录尝试所有HTTP失败均明确结束，再由唯一完成点采集；避免为修埋点全局改变其他旧接口语义。

### F3 · P2 · 推荐空态“发布找宠信息”漏记发布点击【代码确认】

需求：front_demand_v4.md 的 publish_click 应在用户点击发布入口时记录。

[MyFindPetActivity.kt:41](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/findpet/MyFindPetActivity.kt:41) 的管理按钮只导航，无publishClick调用；同页第62行在no_request状态把它显示为“发布找宠信息”。表单曝光会由生命周期记录，但缺少这次明确发布入口点击，漏掉“消息→我的找宠→首次发布”的转化路径。首页找宠入口已记录，不能据此视为所有发布入口都覆盖。

建议按no_request区分首次发布入口与已有资料管理，不将普通编辑也全算成发布点击。

## 明确未完成、但不是本次新增bug的项目

| 项目 | 状态与原因 | 后续工作 |
| --- | --- | --- |
| 找宠 publish_success | 未接入；save响应未提供可靠新建ID，当前save返回Unit，未区分首次新建后采集 | 核对/扩展业务响应或约定可靠关联方案，区分新建/修改/开启后接入 |
| 秀宠 publish_success | 未接入；当前只消费response.code，没有可靠发布对象ID | 后端提供可解析对象ID，客户端新建成功唯一分支采集 |
| 业务远程推送接收/资源路由 | 未发现完整渠道及接收链路；现有AdoptionNotificationsActivity仅为内部兼容转发。Manifest无业务推送receiver/service，只有启动页intent-filter | V2第7节所述渠道接入、载荷校验及登录后重新鉴权；站内消息不依赖此项 |
| 真机、真实广告与部署联调 | 未完成/未核实 | 多账号沟通与完成联动、真实SDK奖励/晚到回调/旋转、海报保存/扫码、实际生命周期及Admin路径等 |

找宠/秀宠成功事件的缺口已由front_demand_v4.md第5节明确允许暂缓，当前不伪造ID是正确处理；但不能因此称所有事件开发完成。没有实际视频导出和独立发布选择页时，不虚构export_video/publish_selector，是按需求处理，不算漏开发。

## 按文档核对结果

| 文档/章节 | 当前可定位的实现 | 判断 |
| --- | --- | --- |
| V2 §1/6/9/10：资料、申请、版本、同意/拒绝/结束/完成、原联系兼容 | adoption/Repository、ViewModels、AdoptionDetailFlow、AdoptionActionFlow、ContactCopy；JSON/Bearer、HTTP+业务码、快照、流程/申请版本与幂等逻辑存在 | 核心代码已实现；多人及线上权限联动待验收 |
| V2 §2/4/5/12.3：两类列表与提醒、可见角标 | 两类Activity与Adapter；visibleUnread分页读取完整集合、旧字段归类、received的communicate/refresh过滤；MessageBadge使用同一可见申请总数 | 已实现；消息顺序按更新的V3增加我的找宠，不按旧六项判缺陷 |
| V2 §7：账号隔离/导航/推送 | sessionRevision、请求取消、内部兼容转发存在；完整远程推送链路未发现 | 部分完成，推送渠道待接入 |
| V2 §11：推广入口、真实奖励、持久化、恢复与快照 | PromotionEntry/Coordinator、PendingRewardStore、RewardObservation、PromotionRepository；本人authpublishlist、成功页及详情入口；FeedRepository与FeedBatches | 已实现主要代码；真实SDK/跨进程恢复端到端待验收 |
| V2 §12.2/12.4/12.5：年龄、海报、刷新失败续页 | ProfileInputPolicy 10–100；Canvas独立1170px海报、字素截断、MediaStore；临时FeedCursor成功后提交、失败保留旧批次 | 已实现；实际保存权限、大字体和二维码设备验收待完成 |
| V2 §13：设置资料入口、流程说明、发布页链接 | 稳定category设置路由、同一资料页、本地adoptionFlow.html及受控WebPageActivity、联系方式下链接 | 已实现代码与资源；不把需求文档的旧“未编码”声明当成现状 |
| find V3 §2/4：首页Tab/公开互动/收藏/评论/原协议 | HomeLayoutFragment第三Tab、FindPetFragment/ViewModel/Adapter；Form旧接口、findId/contact type2/comment type3/report type7、真实复制与独立屏蔽 | 已实现主要代码；未发现这些功能仍为空的情况 |
| find V3 §3/6：资料、207、城市、状态、图标与入口 | FindPetDetailActivity/PetRequestViewModel、FindPetCities、设置资料入口、图标资源；写后GET本人资料、失败不重复写 | 已实现；发布确认约定有文档冲突，见下节 |
| V3 §2/3/5：推荐列表、历史、不可用、错误处理 | V3 GET/POST、recommendation_id去重/已读、真实topic路由、关闭历史、占位与分页 | 主体已实现；F1的已读404分支仍不合要求 |
| V3 §4/7：MessageList与独立未读 | message_id模型；authunreadnum.find_pet_unread缺失按0；全局一次相加；系统通知不再组装/跳推荐 | 已按2026-10-20新协议实现 |
| V4 §1–4/6：标识、会话、生命周期、页面和独立传输 | analytics模块、主进程初始化、Window交互、实例/页面键去重、5秒独立OkHttp、默认开启与内部关闭、备份排除 | 主要实现存在；F2/F3为尚未补齐的覆盖分支，实际Android生命周期待设备验证 |
| V4 §5：发布成功对象ID | 送养使用可靠topic_id且有published防重；找宠/秀宠未接 | 部分完成，依赖接口 |
| V4新增消息列表/领养资料、激励事件 | AnalyticsPages七类message_list_type及adoption_profile；按钮唯一点击点、推广submit确认后使用原attempt UUID | 已接入；SDK与后端Admin待联调 |

## 文档冲突与过期描述（不可当成新增漏项）

1. find V3 §5.1仍写notification_id、系统未读子集及系统通知跳推荐；V3的2026-10-20修订改为message_id、find_pet_unread独立和禁止系统通知推荐路由。当前代码遵循10-20协议，正确；应同步旧文档。
2. find V3 §3.3要求“特别提醒”，并声称覆盖当时V3的旧“不弹额外确认”；但10-20版V3仍明确不弹。当前代码按最新V3移除了额外确认。这里需要统一产品口径，不能同时满足两个相反要求；本次不擅自改回弹窗。
3. V2旧消息顺序没有“我的找宠”，最新find V3为七项；代码七项符合较新需求。
4. find V3城市示例写310000，与正文规范市级ID及实际目录不一致；当前代码使用可靠目录解析（上海市级310100），不应为了匹配旧例改成省级码。
5. 多份文档保留“仅更新文档/尚未编码”和历史QA数量；以当前代码及本次验证为依据，不直接用这些状态句判断完成或未完成。

## 本次验证与边界

- 2026-10-25重新运行当前基线201项JVM测试，0失败、0错误、0跳过；Debug构建和设备测试源码编译通过，结果见baseline-results.json。
- 另行运行1项缺陷行为复现，真实ViewModel+MockWebServer确认F1；结果见gap-proof-results.xml/json。此测试通过是确认存在错误行为，并非需求验收通过。
- git diff --check通过。未修改生产业务代码；只增加本审查目录。
- adb无连接设备；默认SDK路径没有emulator可执行文件，未执行设备测试，未获得设备/系统版本。未核实部署环境当前版本、迁移或后端Admin联调。
- 不将相邻iOS/后端测试数量计入Android本次通过数量；不对尚未运行的交互/线上场景宣称通过。

复查命令（在rescue_xml目录）：

```sh
# 当前业务基线
./gradlew -I qa/adoption/qa.init.gradle -I qa/promotion/qa.init.gradle \
  :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin --offline --console=plain

# 仅复现当前404占位缺口；修复后应改为正确行为回归，而非继续保留错误断言
./gradlew -I qa/adoption/qa.init.gradle -I qa/promotion/qa.init.gradle \
  -I qa/front-demand-review-2026-10-25/proofs.init.gradle \
  :app:testDebugUnitTest --tests 'com.rescue.flutter_720yun.review.FrontDemandGapProofTest' --offline --console=plain
```

建议收尾顺序：修复F1/F2/F3并补正常回归 → 与后端补齐两种发布ID及成功埋点 → 确认推送渠道范围并完成所需接入 → 统一冲突文档 → 设备与部署联调。完成这些之前不标为“所有front_demand已开发/验收完成”。


## 后续：同日真实测试环境验证

用户确认使用test.rxswift.cn后已执行真实HTTP和Android网络类联调。V4路由HTML404，且额外复现旧找宠info的HTTP403/code401被客户端识别成403的问题。此后的环境可达性结论以[测试环境报告](../test-environment-2026-10-25/REPORT.md)为准；没有测试账号和设备，双账号业务与UI验收仍未完成。
