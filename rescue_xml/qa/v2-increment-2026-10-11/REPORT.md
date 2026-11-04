# 2026-10-11 Android V2 增量实现与验证

对照根目录 `front_demand_v2.md`，本轮补齐第 13 节。第 10/11 节的领养、推广与快照，以及第 12 节的表单、可见提醒和海报沿用现有实现并执行回归。未改动用户需求文档、海外需求或 Compose 工程。

## 实现对应

| 需求 | 实现 |
| --- | --- |
| 13.1 设置里的领养资料 | 首项使用现有 iOS `icon_mi_zeliao` 资源，打开同一个 AdoptionProfileActivity，由其处理登录及取消；设置按 category 分派，账号变化刷新列表，原密码/反馈/账号安全/退出入口保持对应 |
| 13.1 资料说明与加载保护 | 用途说明明确申请时快照；加载/保存期间锁定保存；查询失败后禁止使用旧版本保存，保留草稿供重试；PROFILE_CHANGED 重载新版本保留草稿，用户再次确认保存，不自动创建申请；空资料查询清空旧字段 |
| 13.2 领养相关中的流程说明 | 追加在原领养说明之后，导入独立流程图标，保留防骗/说明及原协议入口状态 |
| 13.2/13.4 本地阅读页 | 导入 iOS adoptionFlow.html，补充首次保存后再次申请、提交时资料及沟通中实际接宠申请的说明；六步纵向卡片、双方操作、状态、关闭与防骗完整；本地资源不依赖网络，适配大字体及长行 |
| 13.2 只读容器 | WebPageActivity 增加受控流程页模式，固定正确标题及 asset 路径，关闭 JavaScript/网络/内容访问，不注册账号安全或注销桥，不允许流程页跳转；资源/加载失败提供点击重试 |
| 13.3 发布页说明 | contact_edit 紧下方灰色 12sp 正文与主题色可点击链接，支持换行；点击清除焦点和键盘，再打开同一个流程页；导航不调用发布、保存或清理草稿逻辑 |

保留上轮“我的”页面去除空白区域的修改，领养资料入口仍在头像昵称栏。

## 验证结果

- 110 项 JVM 单元/流程测试通过，0 failures、0 errors。使用现有 adoption/promotion QA sourceSets，覆盖原领养、提醒、推广、快照与海报文本逻辑。
- 新增 6 项资料流程测试：初次查询失败禁止写入、重载失败保留草稿且锁定保存、冲突重载保留草稿并用新版本显式保存、空资料清掉旧字段、初次保存省略 version 且不创建申请、账号 revision 清理资料及草稿。
- `:app:assembleDebug` 成功；`:app:compileDebugAndroidTestKotlin` 成功。
- 新增 4 项 Android 设备测试源码：离线六步内容/标题/只读模式/200% 字体、领养相关入口、发布页返回保留内容/联系/地区/照片、设置条目稳定路由。与已有 4 项海报渲染测试一起编译通过。
- HTML 静态核对六步完整、无远程/脚本资源；XML 解析正常；两份图标与 iOS 原资源 SHA-256 一致；git diff --check 通过。

执行命令：

```sh
./gradlew -I qa/adoption/qa.init.gradle -I qa/promotion/qa.init.gradle \
  :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin \
  --offline --console=plain
```

## 设备执行限制

检测到已连接 Android 设备，Debug 主 APK 安装成功。Gradle connectedDebugAndroidTest 因离线缓存缺少 UTP 测试组件不能启动；随后使用 adb 安装已编译测试包，设备返回 `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`。测试包未安装，8 项设备测试没有执行，不计为通过，也不以编译或静态检查代替真机视觉、大字体、草稿返回和海报验收。

未调用后端生产写接口；真实多人领养、激励广告 SDK、奖励恢复、相册保存权限及部署仍需对应环境验收。远程业务推送接收渠道仍沿用实现记录中的既有限制，本轮未配置推送服务。

APK：`app/build/outputs/apk/debug/app-debug.apk`。
