# 启动防骗提示

对照 iOS 的 `HomePageMainController.showRemindAlert`、`RemindAlertController`、`RemindAlertBtnCell`。

- 隐私同意并进入主页后检查提示。完整指南读取与 iOS 一致的本地 `prevention.html`，不依赖网络。
- 未勾选“不再提示”时，每次重新创建主页展示完整指南；同一主页返回和旋转不重复触发。
- 进入指南页即开始 10 秒倒计时，不要求滚动到底部。倒计时中继续按钮为灰色且不可点击，结束后变为 color_system 并可点击。切后台暂停倒计时。
- 首次没有“不再提示”选项；完成过一次后显示，复选框和文字整体水平居中。点击继续才保存完成次数和勾选状态，次数最多记录为 3。
- 选择不再提示后，改为每天一次简短“特别提醒”，以设备本地日期为准。点击正文打开当前环境的防骗指南网页，“查看案例”打开黑名单。
- 展示时避开开屏/全屏广告；阅读指南期间从后台返回不插入回前台开屏广告。
- 系统返回键退出本次应用浏览，不记为已读；系统 Home 键仍可正常离开。

与 iOS 一致，Release 默认启用，Debug 默认跳过。测试时可显式设置 Gradle 属性：

```sh
./gradlew :app:compileNoAdsDebugKotlin -PSTARTUP_SAFETY_ENABLED=true
```

该参数也适用于用户明确要求的 APK 打包任务；没有明确打包请求时只编译、不生成 APK。
`-PSTARTUP_SAFETY_ENABLED=false` 可显式关闭提示。此开关独立于广告 flavor。

验证：两种 flavor 的 Debug/Release Kotlin 编译通过；新增策略测试覆盖首次完整指南、首次不显示退出选项、未退出时重复完整指南、按日提示和 Debug 跳过。
两个 Debug flavor 各 125 项测试通过。本轮全量运行中一项原有 AnalyticsTransportTest 出现 dispatcher.runningCallsCount 的收尾时序失败，复跑通过；未修改埋点代码或测试。
未进行真机滚动、倒计时、系统返回和开屏衔接验证，未打包 APK。
