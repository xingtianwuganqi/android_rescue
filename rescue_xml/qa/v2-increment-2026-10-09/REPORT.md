# Android V2 增量实现与验证（2026-10-09）

依据 `front_demand_v2.md` 第 12 节，仅修改 `rescue_xml`。保留用户的需求文档及 `overseas_demand.md`，不修改 Compose、后端或既有 QA 报告。

## 已实现

1. **领养资料**：Activity 和 ViewModel 共用 10–100 整数年龄校验，trim 后解析。历史 1–120 资料正常回填，仍尊重服务端完整性，不自动修改年龄或快照。年龄/已选值强调，空选择提示保持常规样式，保存按钮居中、主题色、白字圆角，保存期间禁用，键盘弹出使用滚动与 resize。
2. **可见提醒与角标**：先确认 role，再隐藏 received 的 communicate/refresh；mine 同类及未知 kind 保留。新后端分页读取全部未读，可信 role 不获取申请历史；旧/混合字段分页读取本人全部申请历史，包括终态。ID 去重、移除已读、过滤后统一计算可见 mine/received/total，单帖再过滤。全部读取成功才发布计数，不使用 raw unread-count 捷径。失败保留上次结果；列表及消息页提供重试。隐藏记录不调用已读。
3. **刷新快照**：同范围刷新使用临时 cursor，有效首屏成功后才替换 snapshot/seen/page/hasMore 和列表。失败保留旧批次，仍可继续旧快照的正确下一页。换城市、账号 revision 或快照 409 立即作废旧批次；成功空页按 has_more 继续。首页/同城保存完整业务列表供页面重建，广告仍使用实际新增业务条数，搜索/收藏不启用推广排序。
4. **分享海报**：详情工具栏提供生成海报、系统分享链接、复制链接及 debug 的无二维码海报入口。独立 Bitmap/Canvas/StaticLayout 渲染公开内容，1170px 宽，白底品牌区、真实送养状态、最多约 8 行的介绍、完整比例的首张原图、真实搜索编号及下载区。按完整 grapheme cluster 截断中文、组合符和 emoji。无二维码版本移除整个下载区。
5. **图片与保存生命周期**：使用原有品牌 PNG/下载二维码，非页面截屏；不传作者、账号、联系方式、申请数据或广告到渲染器。下载/采样解码/渲染在后台，预览在主线程；下载后及保存前核验资源和状态。页面离开、账号或目标变化取消任务并清理文件/Bitmap；只保存完成的 PNG。API 29+ 使用 MediaStore pending 写入，不申请存储权限；API 28 仅在保存时申请 WRITE_EXTERNAL_STORAGE；权限覆盖层的暂停保留预览，授权后等页面恢复再保存，真正进入后台则取消。失败/取消清理未完成媒体记录，成功后才提示相册保存完成。

安全资源上限：下载最多 16MiB，解码目标最多约 400 万像素，输出最多 1170×6000；超长/异常图片以“图片生成失败”结束，不分段保存半张海报。

补充核验第 12.6 节：推广 401 在核对当前账号后显示明确的登录失效及原账号恢复提示；新增协调器测试验证退出后暂停、切入另一账号不提交、原账号重新认证并到达重试时间后仍使用同一 UUID。

## 自动验证

执行：

```sh
./gradlew -I qa/adoption/qa.init.gradle -I qa/promotion/qa.init.gradle \
  :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin \
  --offline --console=plain
```

- **104 项 JVM 测试，0 失败、0 错误、0 跳过**。包含原有领养/推广 QA；本轮新增 20 项。已有提醒追加失败测试的服务端夹具更新为完整可见集合，其保留成功页/计数和失败重试断言继续验证。
- 新覆盖：年龄边界与历史完整资料、直接 ViewModel 保存拒绝；多页原生/旧后端提醒、角色优先、隐藏种类、未知 kind、去重、失效响应和后页失败；海报状态/公开输入/真实链接/组合 emoji；真实 HomeViewModel + MockWebServer 的三页后刷新失败续页、无效 data 不提交 meta、空页、跨城、切账号、快照 409。
- **Debug APK 构建成功**：`app/build/outputs/apk/debug/app-debug.apk`。
- **4 项设备端渲染测试源码编译通过，未执行**：`TopicSharePosterRendererTest` 覆盖二维码下载区高度、横/竖图、异常比例和 Android regex 的组合 emoji。
- `git diff --check` 通过。

Lint 使用工作区外的空基线完整扫描，不新增项目忽略文件：6 errors、257 warnings，与既有数量相同。6 项错误均为主/debug Manifest 原有 Leanback launcher、touchscreen optional 和 Leanback feature 声明。新增分享/资料模块未报告错误。报告位于 `app/build/reports/lint-results-debug.html`；“Lint 任务执行成功”不表示这些既有错误已修复。

## 二维码与品牌核对

导入 PNG 与 iOS 参考文件逐字节相同；SHA-256：

- App 图标：`25abe3bc66a001523c175244e5bd1b41d011b50615d59d48046434c5353c5056`
- 下载二维码：`b5335686d7e0591b3e66fab65541d89c1b140e6067a19e61328fd3cb75b97371`

本机 CoreImage 解码原始二维码及 Android 导入资源，均为 [现有 App 下载入口](https://xingtianwuganqi.github.io/autodownload/index.html)。没有生成帖子详情二维码。此项验证不等于最终 Android 相册海报的真机扫码验收。

## 验证边界与设备验收

当前 adb 没有连接设备；未执行真实 Canvas 截图、MediaStore 保存/权限拒绝、外部分享返回、键盘/字体视觉或实际广告 SDK 回归。未调用生产申请/推广写接口、未部署后端、未提交或推送 Git。

设备端应执行 `:app:connectedDebugAndroidTest` 并人工覆盖：长/空正文和组合 emoji、无图与下载失败、横/竖图、三种 V2/旧状态、匿名导出、无二维码版、最终海报扫码、权限拒绝/保存失败/成功、连点和页面离开；同时回归隐藏通知不自动已读、分类角标、同范围失败续页及视频外部跳转返回互斥。
