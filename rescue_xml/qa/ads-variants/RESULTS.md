# 广告版本验证（2026-11-04）

- `assembleNoAdsDebug`、`assembleDomesticDebug` 构建成功。
- `testNoAdsDebugUnitTest`：98 项通过，0 失败。
- `testDomesticDebugUnitTest`：98 项通过，0 失败。
- `verify_apks.py` 检查实际 APK 的 DEX：无广告版不包含 Taku、穿山甲、酷盈 SDK 类；国内版包含对应 SDK。
- 无广告版不包含 AdDemoActivity；国内 Debug 版包含调试入口。
- 合并清单检查：无广告版没有广告 SDK 组件、SupportActivity 或 WarmSplashActivity 声明。
- Debug APK 大小：无广告版 29.1 MiB，国内广告版 50.2 MiB。

环境：项目 Gradle 8.4、Android Studio 内置 JDK、离线依赖缓存。
国内 SDK 有 D8 stack map 警告，未阻断构建。
本次未进行设备 UI、真实广告填充或 Release 签名包验证。

## 我的领养列表观看按钮修复

修复 UserTopicListAdapter 绕过版本开关显示观看按钮的问题，并在点击和确认弹窗处加上版本判断。
两个 Debug APK 重新构建成功，各 98 项单元测试通过，APK SDK 隔离检查通过。
检查无广告版编译字节码，promotionAction 固定调用 setVisibility(8)，即 View.GONE，且 setEnabled(false)。
未进行真机 UI 验证。
