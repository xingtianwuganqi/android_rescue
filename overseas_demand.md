# Android 海外版开发功能需求

修订日期：2026-10-07。

开发基线：`rescue_xml`（Kotlin、XML、ViewBinding、Activity/Fragment、ViewModel、Retrofit）。业务基线：仓库根目录 `front_demand_v2.md`。本文件只定义需求，不表示已经完成代码改造、广告账号配置、构建、真机验收或上架。

## 1. 目标与确定方案

使用同一套业务代码，生成国内版和海外版。两个版本保持同一个 applicationId：`com.rescue.flutter_720yun`，不追加包名后缀，不迁移到 `rescue_compose`。

| 项目 | 国内版 china | 海外版 global |
| --- | --- | --- |
| 包名 | com.rescue.flutter_720yun | com.rescue.flutter_720yun |
| 广告平台 | 保留现有 Taku/AnyThink 接入及广告位 | Google AdMob 直接接入 |
| 广告 SDK | 仅国内广告 SDK 及必要适配器 | 仅 Google Mobile Ads、UMP；本期不使用 Taku 聚合或国内适配器 |
| 业务功能 | 保留当前行为 | 复用当前帖子、领养、消息、推广等功能 |
| 分发 | 国内渠道 APK | Google Play AAB，并提供联调用 APK |
| 版本识别 | BuildConfig.APP_REGION=china | BuildConfig.APP_REGION=global |

广告供应商在构建时确定。禁止按 IP、系统语言、SIM 卡或定位在运行中切换广告平台；海外广告失败也不得回退请求国内广告。

同包名表示同一 Android 应用：两版不能同时安装。相同兼容签名且 versionCode 满足更新条件时可以覆盖更新；安装来源不同可能发生串版。本期接受这一约束，不承诺从系统层面阻止跨渠道覆盖。

## 2. 范围与业务兼容

必须保留首页、同城、搜索、找宠、展示、详情、发布、图片上传、登录、收藏、点赞、评论、消息、领养资料与申请、我的发布、激励推广和观看支持功能。页面入口、后端字段、帖子 ID、账号和现有权限判断不因版本变化而改变。

海外版保留当前中文资源，同时提供英文界面：`values/` 保留中文，`values-en/` 提供英文；英文系统使用英文，中文系统使用中文，其他语言本期回退英文（通过明确的应用语言选择/初始化策略实现并验收，不能依赖默认中文资源自然回退）。设置页提供“跟随系统 / 中文 / English”，选择持久化并在重启后生效。国内版现有语言行为保持不变。

翻译范围包含导航、按钮、表单、校验、空态、错误提示、广告提示、领养/推广状态和隐私入口。用户帖子、评论及其他用户输入不自动翻译。后端自由文本没有英文契约时原样展示；已知业务错误通过 error_code 映射本地文案，未知错误显示可理解的通用提示并保留重试能力。不得把所有领养年龄限制自动当作广告受众年龄。

本期不新建海外账号体系、聊天服务、支付、国家城市数据库或自动内容翻译。现有中国城市/区县和联系方式规则继续沿用；本版本是可在海外分发的现有业务客户端，不代表已经支持全球领养地域。若目标市场需要海外手机号、国家地址或全球定位，另立接口与业务需求，不在客户端伪造兼容。

## 3. 构建、签名与版本管理

### 3.1 构建配置

在 `rescue_xml/app/build.gradle.kts` 增加 `region` flavorDimension 和 `china`、`global` productFlavors，均沿用现有 applicationId。必须提供以下变体：

- chinaDebug / chinaRelease。
- globalDebug / globalRelease。

共用业务代码放 `src/main`，国内广告实现/清单/资源放 `src/china`，海外放 `src/global`。当前 `implementation(fileTree("taku_libs"))` 改为 `chinaImplementation`；AdMob 和 UMP 使用 `globalImplementation`。供应商专用 Manifest 组件、权限、metadata 同样按 flavor 隔离；共用网络等权限可保留。禁止仅用运行时 if 隐藏 SDK。

SDK 版本固定到版本目录或 Gradle 配置，实施时根据 Google 官方文档选定稳定版本，记录准确版本及对应 minSdk/compileSdk 要求。当前工程 minSdk=28、compileSdk/targetSdk=34；SDK 或 Google Play 要求升级时，形成明确变更并回归两版，不强行使用不兼容配置。本期不要同时混用不同代 Google Ads SDK 的 API 示例。

两个 release 构建共用一次发布的 versionName/versionCode；后续任一正式渠道发布使用统一递增的 versionCode 台账，禁止海外自动生成独立的低版本序列。需要从海外切回国内时，应发布更高版本并验证覆盖更新，不能承诺同版本或降级安装一定成功。

交付构建命令（从 `rescue_xml` 执行）：

```bash
./gradlew :app:assembleChinaDebug :app:assembleGlobalDebug
./gradlew :app:assembleChinaRelease :app:assembleGlobalRelease :app:bundleGlobalRelease
```

### 3.2 签名与数据

国内版继续使用现有正式签名，不因拆分更换。海外最终安装包的应用签名须与国内正式包兼容；Google Play 上传密钥与应用签名密钥是不同概念，不能以“上传使用同一 keystore”推断最终签名相同。启用 Play App Signing 前确认可复用现有应用签名的方案；已存在 Play 应用时先核对实际证书。

签名密码与密钥通过受控本地配置或 CI 注入，不新增硬编码、不输出到日志、不复制进需求文档。验证从 Play 下载的包与国内包的签名证书，而不仅检查本地构建。

跨版本覆盖更新保留账号、本地语言设置和有效待提交推广记录；不得无故清空数据。现有国内 `firstOpen` 记录不能被当作已完成 UMP 隐私选择。隐私状态由对应平台管理；地区版本字段与首次引导版本分开保存。

## 4. 广告接口与代码改造

定义共用 `AdsManager`/广告工厂或等价接口，提供初始化、可请求状态、广告位配置、全屏占用管理、开屏/横幅/原生/插屏/激励控制器。业务页面只能依赖共用接口和 `RewardedAdEvent`，不能引用 ATSDK、ATNativeAdView、Google Ads 类型或 `TAKU_*`。

| 当前文件/模块 | 必须完成的改造 |
| --- | --- |
| ads/TakuAds.kt | 迁入国内实现；全屏互斥从 Taku 单例抽为共用能力 |
| ads/SplashAdController.kt、WarmSplashActivity.kt、ForegroundSplashObserver.kt | 国内保留现有行为；海外使用独立 App Open 管理逻辑，不能直接照搬 Taku 开屏触发规则 |
| ads/BannerAdController.kt、DetailBannerAdapter.kt | 共用广告位入口，供应商实现分离 |
| ads/NativeAdAdapter.kt、NativeFeedAdapter.kt、FeedAdLayout.kt、FeedAdPlacement.kt | 列表位置策略共用，供应商视图及生命周期按 flavor 实现 |
| ads/FullscreenAdController.kt、FullscreenOwnership.kt、RewardedAdEvent.kt | 共用展示互斥及类型化事件；供应商监听分别实现 |
| SplashActivity.kt、BaseApplication.kt | 根据构建版本初始化对应流程；隐私和广告失败不能阻塞主页 |
| HomeFragment.kt、FindPetFragment.kt、ShowFragment.kt | 移除直接读 TAKU 广告位配置，改为按业务 placement 获取 |
| promotion/RewardedTopicPromotionCoordinator.kt | 替换 Taku 全屏判断与广告工厂调用；复用现有奖励与网络状态机 |
| user/activity/SupportActivity.kt | 使用共用激励接口，保留观看支持语义 |

抽象后国内仍使用原 Taku 广告位、列表插入策略、回调及业务行为，不重新设计国内广告投放。所有回调绑定原广告实例与展示上下文；广告销毁后不更新失效页面，禁止全局强引用 Activity。

## 5. 海外广告位与展示要求

| 现有类型/场景 | 海外实现 | 要求 |
| --- | --- | --- |
| 冷启动/符合条件的前台恢复 | App Open Ad | 仅在启动/恢复的等待场景显示，遵守第 5.1 节 |
| 详情横幅 | 自适应 Banner | 根据实际容器宽度计算；失败隐藏广告区域 |
| HOME / SEARCH / LOCAL / FIND_PET | Native Ad | 沿用已有业务插入位置；失败移除广告行，分页不漏帖/重复帖 |
| SHOW | Native Ad 独立 placement | 独立配置广告位，不误用其他页面参数 |
| 现有插屏入口 | Interstitial Ad | 保留经核对的自然页面切换触发点；不得新增随机弹出或拦截业务按钮 |
| 帖子推广、观看支持 | Rewarded Ad | 用户主动触发；两类业务目的独立，支持页不创建推广 |

原生广告由 Google NativeAdView 绑定对应素材，明显显示“广告 / Ad”、AdChoices 及 SDK 所需元素；不伪装成普通帖子，不覆盖点击层或人工代点。Banner 不覆盖导航、输入、正文和业务按钮。没有广告时不留大块空白。

广告对象展示一次后失效，不重复 show；展示/失败/关闭均释放相应全屏锁。App Open、插屏、激励、隐私表单展示互斥，推广准备与播放时不能插入开屏。加载只在有效前台页面需要时发起，禁止无上限重试；超时后的回调不能突然展示。

### 5.1 海外 App Open 规则

以下时间是本产品默认策略，可配置但须有测试，不是 Google 强制统一值：

1. 首次安装/首次完成引导当次不展示；完成首次正常使用后，后续启动才可参与开屏展示。
2. 冷启动仅在加载界面有已就绪、未过期广告且允许请求时展示；广告等待最多 3 秒，未就绪直接进入主页。进入主页后不得因加载回调补弹。
3. 前台恢复须已在真实后台停留至少 30 秒，且距上次开屏展示至少 120 秒；页面切换、旋转、分屏短暂停顿不算新的开屏机会。
4. 登录、发布/编辑、相机/图片选择、权限对话框、外链跳转、隐私表单、领养申请提交、支持页与激励广告返回不触发恢复开屏。显式维护这些流程的抑制标记，不能仅凭后台计时判定。
5. 不展示加载后超过 4 小时的 App Open 广告；缓存记录加载时间并主动失效。
6. 无可用前台 Activity、离线、配置缺失、加载失败或展示失败均跳过；一次启动只导航一次、只尝试展示一次，不形成 WarmSplash 循环。

## 6. UMP 与隐私流程

海外首次运行展示本应用对应语言的隐私政策/服务条款；广告隐私选择另外使用 Google UMP，由 AdMob Privacy & messaging 配置对应消息。禁止以国内隐私弹窗的同意状态代替 UMP 结果，也不得用自制弹窗冒充 UMP。

每次启动调用 UMP `requestConsentInfoUpdate()`；在合适的前台 Activity 调用 `loadAndShowConsentFormIfRequired()`。请求任何广告前必须满足 UMP `canRequestAds()`，并满足本应用需要的初始化条件；可请求才初始化并请求广告。已有历史有效状态也按官方流程处理，两个成功路径不能导致重复初始化/请求。

UMP 查询/表单失败时，重新读取 canRequestAds：允许请求才继续广告，不允许则无广告进入应用，不凭失败推断用户同意。用户的不同隐私选择不一定全部对应“禁用广告”，以 SDK 可请求结果为准。拒绝或表单失败不影响普通业务；激励不可用时说明原因且不发奖励。

设置页提供隐私政策、服务条款、广告隐私选项；当 UMP privacyOptionsRequirementStatus 要求入口时可调用其隐私选项表单。选择更新后重新评估广告请求资格，销毁不再适用的缓存，避免继续展示按旧状态加载的广告。

调试地理位置、测试设备、UMP reset 仅用于调试。正式包不得自动 reset、强制测试地区或绕过隐私选择。广告受众、未成年人相关标签按实际产品受众及平台要求配置，不靠猜测年龄设置。

## 7. 激励推广与奖励处理

严格沿用 `front_demand_v2.md` 第 11 节和当前后端契约：查询资格 → 用户主动观看 → SDK 真实奖励 → 本地持久化 → 原 UUID 幂等提交 → 后端确认 → 刷新推广状态。

AdMob 事件映射必须明确：

| AdMob 事件 | 共用事件/处理 |
| --- | --- |
| onAdLoaded | Loaded；当前尝试和前台宿主仍有效才允许 show |
| OnUserEarnedRewardListener.onUserEarnedReward | Reward；唯一可记录奖励并发起推广提交的事件 |
| onAdDismissedFullScreenContent | Closed；未观察到 Reward 不提交 |
| onAdFailedToLoad / onAdFailedToShowFullScreenContent | Failed；释放占用，不凭失败补奖励 |

播放结束、曝光、点击、关闭、观看时长都不能替代真实奖励回调。奖励值/类型也不能由客户端直接决定帖子推广时长，推广时长和额度仍由服务端决定。

同次观看仅一个 reward_attempt_id，固定绑定原账号、sessionRevision、topic_id、scene；重复奖励、晚到奖励及网络重试不得产生新 UUID。保留当前按原账号持久化、24 小时恢复、Retry-After、幂等防重与切号保护。App 关闭后再次启动不伪造未观察到的奖励。

推广仍只使用现有 promotion-state 和 promotion 接口，scene 为 `my_posts`/`publish_success`，本人详情复用 my_posts；观看支持仅感谢、不提交帖子推广。禁止新增客户端 reward=true、观看时长或 user_id 字段。

当前服务端没有广告平台回调验签，不得将客户端奖励方案描述成已具备 AdMob SSV。SSV 不属于本期默认交付；若启用，必须先补充服务端验签、尝试关联与重复事件处理契约，再联调上线。

跨 flavor 更新继续恢复已观察到的合法待提交业务记录，但旧 SDK 未完成的播放不得在另一 SDK 中续接。新增记录结构须兼容已有记录，不能通过升级清库丢弃奖励。

## 8. 配置、网络与上架前置条件

通过 Gradle 属性/受控配置注入 `ADMOB_APP_ID`、`ADMOB_APP_OPEN_ID`、`ADMOB_BANNER_ID`、`ADMOB_NATIVE_ID`、`ADMOB_NATIVE_SHOW_ID`、`ADMOB_INTERSTITIAL_ID`、`ADMOB_REWARDED_PROMOTION_ID`、`ADMOB_REWARDED_SUPPORT_ID`。最后两项使用独立广告位，区分推广和支持数据。

App ID 写入海外 Manifest 的 `com.google.android.gms.ads.APPLICATION_ID`，广告位 ID 用于对应格式。调试默认使用 Google 官方测试 ID；release 校验 App ID 和启用的广告位已填写、格式合理且不是官方测试值，失败应阻断发布构建。单广告位通过明确启用配置关闭，不用空值混淆“缺配置”和“主动禁用”。AdMob ID 不是签名密钥，配置文件不得混入账号凭证。

保留现有 API_BASE_URL/ADOPTION_BASE_URL 配置机制。本地调试可使用测试服务，海外 release 必须明确注入已验证的 HTTPS 正式地址，不能回退当前 HTTP 测试默认值。核查上传、图片/CDN、登录、城市/定位、隐私政策 URL 在目标国家可达；不在客户端把 HTTP 替换成 HTTPS 后就宣称服务可用。

定位不可用、权限被拒绝、Google 服务/广告服务不可用时能手动选择现有城市并继续业务，无广告崩溃或无限加载。不得把已确认的海外网络故障自动切回国内广告。

上线前提供真实 AdMob 应用/广告位、UMP 消息、可公开访问的隐私政策与支持链接、Google Play 应用资料、目标国家、受众与 Data safety 信息；开发者依据实际 SDK/数据流填写。实施和发布时重新核对 Google Play 当前 targetSdk 与 SDK 要求，不以当前工程 targetSdk=34 推断已满足上架条件。缺账号/生产配置不妨碍测试包开发，但不得标记正式发布验收通过。

设置“关于”页显示应用版本和版本区域，便于用户/客服识别；输出文件包含 region、versionName、versionCode。日志可包含 region、placement、加载结果/错误码；不记录 token、联系人、领养资料或隐私选择明细。

## 9. 验收标准

| 编号 | 场景 | 通过条件 |
| --- | --- | --- |
| O01 | 构建四变体与海外 AAB | 成功构建，包名一致，region 正确，release 配置校验有效 |
| O02 | SDK 隔离 | 检查依赖图与最终 APK/AAB：global 无 AnyThink/Taku/国内适配器类及组件，china 无 AdMob/UMP 类及组件；共用 Google 布局库不算广告 SDK |
| O03 | 国内回归 | 原开屏、信息流、横幅、插屏、激励、支持和推广流程无行为退化 |
| O04 | 覆盖更新 | 用实际兼容正式签名及更高 versionCode 做 china→global、global→china；账号、本地数据和待提交奖励保留；双版本不能并存符合预期 |
| O05 | Play 签名 | 对 Play 分发包核对应用签名证书并验证覆盖路径；仅本地通过不算完成 |
| O06 | 隐私 | 首次/历史状态/需要表单/无需表单/更新选择/网络失败均测试；不可请求时广告请求为零，业务可进入 |
| O07 | 开屏 | 首次引导不弹、3 秒超时跳过、进入主页无补弹、后台/间隔阈值有效、缓存超过 4 小时失效，敏感流程返回不弹 |
| O08 | 信息流与横幅 | 素材/AdChoices/广告标识正确，失败无空白广告行，刷新分页无重复漏帖，旋转/销毁无崩溃或泄漏 |
| O09 | 全屏互斥 | 开屏/插屏/激励/隐私表单不重叠，关闭/失败释放锁，无效宿主不展示 |
| O10 | 激励正常与异常 | Reward 才提交；提前关闭、仅播放结束、加载/展示失败不提交；重复/晚到奖励不重复推广 |
| O11 | 奖励恢复 | 奖励后断网、5xx、429、进程重启、跨版更新仍用原 UUID；切号不跨账号提交；支持页不创建推广 |
| O12 | 语言 | 中文/英文/其他语言回退、手动切换和重启均正确；未翻译用户内容无误改，长英文不截关键按钮 |
| O13 | 海外业务 | 在明确目标国家网络验证登录、列表、详情、上传、申请、消息、推广及隐私 URL；定位失败有可用降级路径 |
| O14 | 发布配置 | 正式 HTTPS、真实启用广告位、无测试地区/reset、无测试 ID；当前 Play 检查与信息填写完成 |

自动化验证聚焦奖励状态机、幂等/账号保护、配置校验和开屏资格判断；SDK 展示、UMP、签名覆盖与海外可达性需真机/发布渠道验证。全程使用官方测试广告或获准测试设备，禁止通过点击真实广告进行验收。

## 10. 实施顺序与交付物

1. 固化国内当前广告行为和业务基线，整理供应商依赖与 Manifest。
2. 增加 flavor、region 标识、签名/版本台账和配置校验，确认同包名产物。
3. 抽共用广告接口及全屏互斥，把 Taku 实现迁到 china，先完成国内回归。
4. 接入 global 的 UMP、初始化、Banner/Native/Interstitial/Rewarded，再接入 App Open。
5. 联调推广奖励事件与恢复，完成语言、隐私/关于入口及网络降级。
6. 执行验收矩阵，最后补齐生产账号、签名、海外网络和 Play 发布检查。

交付物：代码与构建配置、广告配置示例（无敏感值）、两个 debug APK、两个签名 release APK、海外 release AAB、构建/SDK 隔离证据、真机验收记录及待解决问题清单。记录中明确测试设备、系统版本、构建版本、签名证书指纹、目标网络及测试/正式环境。

仅完成测试包时标记“可联调”，不得标记“可上架”；只有 O01–O14 全部通过且生产前置条件齐备，才标记海外版可发布。

## 11. 官方参考

实施时重新核对官方最新文档；本需求中的产品频控参数与平台规则分别处理。

- [Android product flavors 与构建变体](https://developer.android.com/build/build-variants)。
- [Google AdMob Android 接入](https://developers.google.com/admob/android/quick-start)。
- [Google UMP 隐私流程](https://developers.google.com/admob/android/privacy)。
- [App Open 生命周期、缓存有效期与展示建议](https://developers.google.com/admob/android/app-open)。
- [Rewarded Ads 奖励回调](https://developers.google.com/admob/android/rewarded)。
- [Google 官方测试广告](https://developers.google.com/admob/android/test-ads)。
- [Play App Signing](https://developer.android.com/studio/publish/app-signing)。
