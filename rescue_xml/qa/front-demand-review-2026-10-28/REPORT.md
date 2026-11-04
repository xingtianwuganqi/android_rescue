# front_demand 剩余需求复核 · 2026-10-28

结论：未全部完成。范围为当前仓库的四份front_demand文档和rescue_xml工作区；复核此前审查、测试环境联调及随后401兼容修复。没有将文档中的历史“待实现”句子直接当成当前状态，也没有重复列出已修复问题。本次只做代码/文档复核，未修改业务实现，未重跑构建、网络或真机测试。

## 当前仍需开发

| 优先级 | 文档要求 | 缺口及定位 |
| --- | --- | --- |
| P2 | V3 §3/5：资源不可用切换占位 | MyFindPetViewModel.open在read返回404时不会进入后续占位逻辑；clearUnavailableState仍为空。旧图片/正文继续作为可用卡片显示。10月25日已有真实ViewModel+MockWebServer复现，相关代码尚未改动。 |
| P2 | V4 §1/3：登录明确失败记录login_failed | LoginViewModel登录及注册仍调用awaitResp；其HTTP401/403分支不resume或resumeWithException，失败分支无法正常结束，取消时也不记失败。这与已修复的FindPetRepository旧业务码归一化是两条不同调用链。 |
| P2 | V4 §1：发布入口点击记录publish_click | 我的找宠no_request状态的按钮文案为“发布找宠信息”，但监听器只有导航，没有publishClick。首页入口已有采集，不能替代此路径。 |
| 接口依赖 | V4 §5：找宠publish_success | 目前没有事件接入，save仍不返回新建对象ID；需可靠ID和首次新建判定，不能将修改或开启记成新建。文档允许暂缓但仍是未完成项。 |
| 接口依赖 | V4 §5：秀宠publish_success | 当前发布只消费response.code，没有可靠对象ID及成功事件采集；需补响应/关联方案后接入。 |
| 渠道依赖 | V2 §7：业务远程推送接收及资源导航 | 未发现完整推送接收/渠道路由。Manifest没有业务推送service/receiver；AdoptionNotificationsActivity是内部兼容转发。需渠道接入及topic/application载荷校验、登录后重新鉴权；站内提醒已存在，不等于远程推送完成。 |

关键代码：

- [推荐read及404处理](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/findpet/FindPetViewModels.kt:46)，[空实现](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/findpet/FindPetViewModels.kt:62)。修复应仅处理明确业务不可用的目标，保留未读及其他历史；不能把未知路由404一律当资源删除。
- [旧awaitResp的HTTP401/403分支](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/network/ServiceCreator.kt:74)，[登录调用点](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/home/viewmodels/LoginViewModel.kt:53)。
- [漏记发布点击的入口](/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml/app/src/main/java/com/rescue/flutter_720yun/findpet/MyFindPetActivity.kt:41)。只在首次发布意图下采集，避免把已有资料管理全部算作发布点击。

## 按文档的整体状态

| 文档 | 已存在的主要实现 | 剩余 |
| --- | --- | --- |
| front_demand_v2.md | 领养资料、两类申请及提醒、可见未读、同意/拒绝/放弃/完成、联系授权、推广协调器和恢复、feed快照、海报、设置资料入口及本地流程说明 | 推送接收链路；双账号/真广告/设备验收 |
| front_demand_find_v3.md | 首页找宠Tab、互动/复制/收藏/评论、资料及关闭开启、城市解析、入口和图标 | 推荐错误分支；与新版V3的旧文档冲突；设备视觉/交互验收 |
| front_demand_v3.md | 推荐分页与历史、message_id、独立find_pet_unread、系统通知分离、原帖跳转与原V2联系流程 | 推荐已读404占位；实际推荐投递/双账号验收 |
| front_demand_v4.md | 统一安装/会话/序号、主进程采集、页面曝光、独立静默HTTP、送养发布成功、海报及推广事件 | 登录失败分支、发布入口漏点、找宠/秀宠成功事件、环境/设备联调 |

## 已修复，不再计为未完成

FindPetRepository.legacy已经将旧业务code401归一化为认证失效，包含HTTP403/code401；继续使用现有失效回调并保留账号快照检查。真权限403、新V2/V3规则没有全局改成登出。

10月25日修复记录中208项JVM回归、Debug构建及原真实测试环境失败用例均通过。这里引用历史结果，本次未再次执行，不能将其表述为10月28日新测试结果。

## 尚未完成验收或需要统一的约定

- 10月25日测试地址test.rxswift.cn的V4路径返回HTML404，尚无后续部署成功证据；今天未重新请求，当前环境状态待复测。不能将“客户端已接埋点”视作已成功入库。
- 尚无双测试账号完整业务和真机/模拟器验收记录：申请→同意→联系→完成/关闭、真实推荐及未读隔离、实际SDK奖励/晚到回调、生命周期和海报保存/扫码。
- find V3旧文档仍写notification_id、推荐是系统未读子集、系统通知跳推荐；10月20日V3改为message_id、独立find_pet_unread及禁止该跳转。代码遵循新版，不应改回旧行为。
- 找宠“特别提醒”存在相反要求：find V3要求保留，而后修订V3要求不弹，代码当前按后者。应先统一文档；不同时算成两个功能缺失。
- V2旧六项消息顺序已由find V3七项覆盖；城市示例310000与规范市级ID正文冲突，不能为匹配旧例修改现有可靠目录解析。
- 无实际视频导出或发布选择页时不虚构export_video/publish_selector，符合文档，不是本期漏开发。Compose明确不在本期范围。

证据参考：[10月25日完整需求审查](../front-demand-review-2026-10-25/REPORT.md)、[真实测试环境报告](../test-environment-2026-10-25/REPORT.md)、[旧认证修复与验证](../legacy-auth-fix-2026-10-25/REPORT.md)。

建议顺序：修复三处客户端缺口 → 明确两种发布ID方案 → 确认/接入推送渠道 → 统一冲突文档 → 测试环境及设备全流程验收。
