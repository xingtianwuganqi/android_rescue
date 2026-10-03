# Android 广告接入

接入工程为 `rescue_xml`（现有完整业务 XML 工程），SDK 来自下载目录的
`20261002_Taku_Sdk_release_China_v6.6.51_20261002085557.zip`。
`rescue_compose` 未改动。

## SDK 和配置

`app/taku_libs` 保留下载包中的 6 个 AAR，包括 Taku 6.6.51、穿山甲 7.6.1.2、
酷盈 SDK 和对应适配器。AAR 自带组件清单及 consumer ProGuard 规则；
压缩包额外要求的 Apache HTTP legacy 声明已加入应用清单。
资源保留规则位于 `app/src/main/res/raw/taku_keep.xml`，资源优化白名单在
`app/taku_libs/whitelists.txt`。项目已有 AndroidX/Jetifier 和 HTTP 网络配置。

在 `taku-ads.properties` 填入 Taku 后台创建的 **Android** 配置：

| 配置 | 用途 |
| --- | --- |
| TAKU_APP_ID / TAKU_APP_KEY | Android 应用初始化 |
| TAKU_SPLASH_ID | 开屏 |
| TAKU_BANNER_ID | 领养详情正文与图片之间的横幅 |
| TAKU_NATIVE_ID | 首页、同城、搜索、找宠、晒宠列表信息流 |
| TAKU_NATIVE_SECONDARY_ID | 晒宠独立广告位；为空时使用 TAKU_NATIVE_ID |
| TAKU_INTERSTITIAL_ID | 插屏 |
| TAKU_REWARDED_ID | 激励视频 |

已填入用户于 2026-10-02 提供的 Android App ID、App Key 和五类广告位 ID。
第二个原生广告位未提供，`TAKU_NATIVE_SECONDARY_ID` 保持为空。
App ID/Key 为空时不初始化、不请求广告；单个广告位为空时跳过对应广告。
支持同名 Gradle 属性覆盖（`-PTAKU_APP_ID=...` 等，或用户级 `gradle.properties`）。
更新配置后需重新编译。广告平台本身的 App ID/Key 等参数在 Taku 后台配置，
不应直接复制 iOS 广告位用于 Android。

## 与 LaveCat iOS 的展示位置对照

| iOS 实现 | Android 对应位置和规则 |
| --- | --- |
| `AppDelegate` 冷启动/回前台开屏 | 首次安装跳过开屏；以后 `SplashActivity` 冷启动等待最多 5 秒，后台返回由 `WarmSplashActivity` 等待最多 3 秒；关闭后返回原页面，不重置导航；激励视频及开屏广告落地页返回不叠加开屏 |
| `TopicDetailViewModel.setupSection` / `TopicDetailBannerADCell` | `HomeDetailActivity` 正文之后、图片之前的横幅（按 Android 最新要求调整），左右 15dp 边距；失败/关闭后折叠，首页底部不再显示横幅 |
| `HomePageListReactor` 首页推广卡片及分页原生广告 | 首页首批第 3 项按用户要求使用已提供的原生广告替换 iOS 文字壁纸推广卡片；下一页原生广告位于该页开头 |
| `HomePageListReactor` 同城原生广告 | 同城每批第 4 项，不足 4 条时追加到该批末尾 |
| `SearchResultReactor` | 搜索首批超过 3 条时第 4 项显示原生广告；下一页第 4 项，不足时放在末尾 |
| `FindPetListReactor` | 找宠每批第 4 项，不足时放在末尾 |
| `ShowPageListReactor` | 晒宠每批第 4 项，不足时放在末尾；支持独立第二广告位，当前为空则复用已提供的原生广告 ID |
| `SupportViewController` | 首页侧边栏“支持我们”打开 `SupportActivity`，进入时预加载激励视频，点击“开始支持”才展示；奖励回调时感谢用户 |
| 插屏 | iOS 业务没有调用插屏，Android 保留接口及 Debug 调试入口，不在业务页面增加自动插屏 |

原生广告使用 `NativeFeedAdapter`，每批按业务记录数定位，SDK 加载成功后才插入广告行；
失败、关闭不留空白，不将广告伪装成业务模型，不改变分页参数或点击对应的业务记录。
刷新释放旧广告，加载更多为空时不插入新广告。支持模板及自渲染、曝光点击和下载广告信息链接。
收藏页不添加广告。各广告对象随 Activity/Fragment View 生命周期释放。

`FullscreenAdController` 提供 `loadInterstitial()` / `showInterstitial()`、
`loadRewarded()` / `showRewarded()` 接口。返回 `false` 表示未配置、未就绪或当前不能展示。
加载不会自动弹出广告，`onReward` 仅由 SDK 奖励回调触发，同一展示去重。
当前“支持我们”不增加积分或收费逻辑；iOS 领养详情仅保留奖励回调，并没有播放触发入口，
因此 Android 不给获取联系方式增加视频门槛。

## 调试

```sh
cd rescue_xml
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.rescue.flutter_720yun/.SplashActivity
# 从启动页完成隐私同意后，打开仅 debug APK 包含的广告调试页：
adb shell am start -n com.rescue.flutter_720yun/.ads.AdDemoActivity
adb logcat -s TakuAds
```

调试页可加载/展示插屏、激励视频、横幅及两个信息流广告位，奖励仅显示回调状态。
release APK 不包含此调试页。SDK 网络日志仅在 debug 开启。

真机需确认：首次拒绝/同意隐私及首次跳过开屏、已有同意记录的冷启动、后台返回、
开屏失败/断网/超时回退、加载中退到后台、广告落地页返回、详情页横幅关闭、五类列表信息流曝光点击与分页位置、
旋转或退出页面后没有旧回调更新 UI、激励完成与提前关闭的回调区别。
ARM 真机用于穿山甲等渠道的完整验证。
应用线上隐私政策由现有服务端网页提供，发布前应更新其中的 Taku、穿山甲及酷盈 SDK 披露。

官方接入参考：[中国内地版接入配置](https://help.takuad.com/docs/Fm1OTO)、
[初始化](https://help.takuad.com/docs/jjZJbi)、[开屏](https://help.takuad.com/docs/j4SZ6n)、
[原生自渲染](https://help.takuad.com/docs/oct5Mn)。

## 本次验证（2026-10-02）

- 使用本机 Gradle 8.9 / JDK 17，`:app:assembleDebug` 成功生成 Debug APK。
- `:app:compileReleaseKotlin :app:processReleaseMainManifest` 成功；release 合并清单没有 `AdDemoActivity`。
- 广告位置与列表布局单元测试覆盖短列表、空页、分页插入、广告关闭/失败后业务项映射；`:app:testDebugUnitTest` 共 8 项测试通过。
- 使用提供的 Android 配置重新构建成功；Debug/Release 的 BuildConfig 与配置文件逐项一致，第二个原生广告位为空。
- 下载包的 AAR 与原始 ZIP 内容一致；AAR 自带 consumer ProGuard 规则。
- 原项目缺失 wrapper JAR，已从仓库根目录补入并修正忽略规则，保留 wrapper 指定的 Gradle 8.4。
- 本机没有已连接的 Android 设备；尚未进行真实广告请求/展示或奖励回调验证。
- 打包 SDK 存在 Jetifier 混用及 D8 stack map 警告，未阻止 Debug 构建。
