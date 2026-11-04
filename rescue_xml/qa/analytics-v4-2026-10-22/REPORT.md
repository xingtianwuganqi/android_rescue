# Android V4 埋点实现与验证 · 2026-10-22

本仓库对应需求为 front_demand_v4.md，协议为 API_analytics_v4.md。同名 front_demand_analytics_v4.md 在 LaveCat 仓库，属于 iOS 需求；本次没有修改 iOS 或 rescue_compose。

## 已接入

- 主进程 BaseApplication 初始化独立 AnalyticsManager、AnalyticsCore、AnalyticsTransport、AnalyticsLifecycle；启动默认开启，不依赖原隐私、登录或权限弹窗。不在辅助进程或仅 Application 创建时生成 app_open。
- 安装 UUID、会话 UUID、递增序号及时间使用 analytics_v4 SharedPreferences，apply 异步写入；存储失败保留进程内状态，不影响点击。备份和设备迁移排除 analytics_v4.xml，账号退出不删除安装标识。
- ActivityLifecycleCallbacks 汇总 started Activity，700ms 延迟确认整个 App 退后台；Activity 交接、旋转和权限短暂停顿不会直接计退出。主导航按当前可见目的地记录 home/messages/profile，不监听 ViewPager cell、数据绑定或分页。
- 活动实例 UUID 保存在 savedInstanceState；页面键与消息列表类型去重，旋转/重绘保持原访问，离开后返回重新计访问。三种发布表单、登录、送养详情、领养资料及七类消息详细列表接入；页面/属性只使用协议枚举。
- Window 输入事件更新真实交互时间。后台不足30分钟保持会话且不重新报打开；达到30分钟先换会话再报打开。前台闲置超时由下一次实际操作换会话；网络回调和业务完成不延长闲置。
- 登录入口、业务登录/注册自动登录成功、明确业务失败和网络异常接入。成功在保存新 Token 后记录；持久账号恢复不补报登录。匿名登录沿用会话，退出/换账号更换会话；Token 轮换不直接重建会话。
- 发布入口在拦截登录前记录；送养新建业务 code=200 且 topic_id>0 时使用现有 published 防重分支发送成功。查找本人、修改、开启不伪装新建。
- 主动退出最终确认清 Token 前记录 logout；实际注销最终确认发请求前记录 logOff。被动401清理不记录主动退出。
- 正式海报生成入口点击记录 export_image；调试无二维码入口和后续权限/下载/渲染/保存回调不重复记录。
- 原推广入口的“观看视频”确认按钮记录 click_video_ad；推广 repository.submit 确认成功后才记录 video_ad_success。scene、topic、account、reward_attempt_id 均来自原 PendingReward，成功 event_id 沿用原 UUID。原推广业务恢复/重试不受埋点影响，同一进程内同 UUID 只发送一次成功；跨进程回放使用相同 ID 供服务端去重。
- 独立 OkHttp 立即 enqueue 单个 JSON 对象，5秒总超时、无连接重试/重定向、无认证刷新或业务拦截器。所有响应静默结束；不走 awaitResp、不登出、不弹 Toast、不建立离线队列。事件创建即固定 Token，原匿名事件不会变为新账号事件。内部 setEnabled(false) 停止生成并取消在途请求。

## 明确缺口

- 找宠/秀宠新建成功缺少发布响应中可靠的对象 ID；依需求暂不发送相应 publish_success。本人资料重查不作为新建与对象 ID 的猜测依据。
- 当前没有实际视频导出入口，不发送 export_video；当前没有独立发布选择页，不虚构 publish_selector 曝光。
- 本轮只接入 rescue_xml，Compose、iOS 不计入覆盖范围。

## 验证结果

全量 JVM 测试 201 项，0失败、0错误、0跳过。其中新增埋点 39 项：核心27、真实 Manager 单例8、HTTP传输4。使用内存时钟/存储及 MockWebServer，不调用生产上报接口。

覆盖精确30分钟边界、短后台/冷启动恢复、持续操作、自动回调不续会话、匿名登录/退出/切号/Token轮换、并发序号、持久化异常、曝光去重、枚举/属性/JSON整数/时区/字节上限、原attempt UUID、Bearer快照、401不改业务账号、各HTTP错误不重试和取消在途请求。

:app:assembleDebug、:app:compileDebugAndroidTestKotlin 与 git diff --check 通过。构建存在原广告SDK的D8警告和已有源码警告，不阻断构建。

adb 当前无连接设备，未执行真机/模拟器生命周期和端到端业务验收。未核实目标环境是否部署 V4，后端 Admin 路径展示、首次安装隐私弹窗前采集、多进程、实际旋转/权限弹窗、真实奖励恢复仍需设备与部署联调，不能将单元测试视为这些场景已通过。

```sh
./gradlew -I qa/adoption/qa.init.gradle -I qa/promotion/qa.init.gradle \
  :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin \
  --offline --console=plain
```

安装包：app/build/outputs/apk/debug/app-debug.apk。
