# 项目与协作约定

## 项目位置

- Android 项目根目录：`/Users/jingjun/Desktop/Githup/android_rescue`。
- 对应 iOS 项目：`/Users/jingjun/Desktop/Githup/LaveCat`。
- iOS 协作约定来源：`/Users/jingjun/Desktop/Githup/LaveCat/AGENTS.md`。
- 在本项目的需求只准参考 iOS 端的实现，不能修改 iOS 端的代码。
- 当前完整业务工程为 `rescue_xml`，默认在该工程实现 Android 业务修改；`rescue_compose` 为独立工程，不自动同步修改。
- 需要对照两端业务、文案或交互时，可阅读 iOS 实现；将规则转换为 Android 现有实现，不直接照搬 UIKit 或 iOS 专用依赖。

## 构建与验证

- 只有用户明确要求打包时才生成 APK；修改代码后不要自动运行 `assemble*`、`package*`、`build` 等会生成 APK 的任务。
- 根据改动选择必要的静态检查、编译或单元测试，并说明实际验证范围；未进行真机验证时不要声称页面已在设备上验证。
- 广告版本由 `rescue_xml` 的 `ads` flavor 管理：`noAds` 为默认无广告版，`domestic` 为国内广告版。新增广告入口必须遵守 `BuildConfig.ADS_ENABLED`，无广告版不得显示观看入口或引入广告 SDK。
- 用户要求打包时明确选择 flavor；`assembleDebug` 会构建两个版本。详细命令和输出位置见 `rescue_xml/ADS_INTEGRATION.md`。

# 列表页面样式约定

以下规则迁移自 iOS 项目的 `AGENTS.md`，已按 Android 工程调整。适用于消息页的点赞、收藏列表，以及「消息 → 我的找宠」中的同城推荐列表。新增同类列表或修改这些列表时，沿用现有控件和状态，不另做一套加载与失败提示。

## 列表与空态

- 列表沿用现有 `RecyclerView` 与卡片布局，使用项目的浅灰背景资源；对照 iOS 的 `.defIcon` 使用 `R.color.color_defIcon`，已有页面的背景按现有设计保持一致，不额外添加列表分隔线。
- 空数据或首屏请求失败时，复用页面现有空态视图和状态处理。标题使用 `R.color.color_content`，描述使用 `R.color.color_desc`，字号沿用项目同类页面的资源；空态整体向上偏移屏幕高度的 10%。不要引入 `DZNEmptyDataSet` 等 iOS 组件。
- 正常空数据文案为「暂无数据 / 请点击重试」；请求失败文案为「网络加载失败 / 请检查网络后重试」。标题与描述分行显示，空态可点击，点击后重新请求第一页。已有找宠业务状态的专用提示可保留。
- 首次加载且没有数据时显示系统 `ProgressBar`；下拉刷新控件正在转动时，不重复显示空态加载动画。
- 已有列表数据时，刷新或下一页失败应保留已有内容，允许继续下拉重试；不要用阻断式弹窗代替列表失败态。账号受限、登录失效等业务状态单独处理。

## 刷新与分页

- 下拉刷新和上拉加载复用现有 `SmartRefreshLayout`、`MaterialHeader`、`ClassicsFooter`，以及页面已有的 ViewModel 加载状态。不要在列表顶部再加一个专用「重新加载」按钮。
- 刷新重新请求第一页并重置分页状态；上拉加载下一页，沿用接口已有的页码或游标协议。
- 结束刷新时沿用现有约 200 毫秒的收尾时机，如 `finishRefresh(200)`、`finishLoadMore(200)`，并正确更新页脚状态：空列表禁用加载更多；还有下一页时允许加载；没有更多数据时设置 `setNoMoreData(true)`；下一页失败时保留数据并恢复可重试状态，不误标记为没有更多数据。
- Android 参考实现：`rescue_xml/app/src/main/java/com/rescue/flutter_720yun/findpet/MyFindPetActivity.kt`；列表状态可参考 `rescue_xml/app/src/main/java/com/rescue/flutter_720yun/util/UiState.kt`。复用前核对具体页面的状态和接口语义。
- iOS 对照实现（相对于 iOS 项目根目录）：`LoveCat/Class/MessagePage/ViewControllers/MessageListController.swift`、`LoveCat/Class/FindPetV3/FindPetV3Lists.swift`；iOS 可复用组件见 `LoveCat/Class/Adoption/AdoptionProfileController.swift`。这些路径用于对照交互，不是 Android 依赖。
