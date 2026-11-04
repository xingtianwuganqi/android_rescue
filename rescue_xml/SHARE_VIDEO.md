# 详情分享与视频导出

## iOS 对照

对照项目：`/Users/jingjun/Desktop/Githup/LaveCat`。

- `LoveCat/Class/MinePage/Views/ShareAlertView.swift`：四项底部分享面板。
- `LoveCat/Class/HomePage/ViewControllers/TopicDetailViewController.swift`：分享、原图下载、导出与进度规则。
- `LoveCat/Class/HomePage/Views/TopicShareVideoRenderer.swift`：画面和时间线。
- `LoveCat/Class/HomePage/Views/ShareVideoMusic.swift`：配乐、循环、音量与淡入淡出。
- `LoveCat/Class/HomePage/ViewControllers/TopicShareVideoPreviewController.swift`：预览、保存、分享。

## Android 行为

详情页分享打开底部面板，按顺序横排显示“导出视频、生成海报、分享链接、复制链接”，
使用浅灰背景、60dp 白色图标框、12sp 标题，底部独立“取消”按钮。

- 生成海报：沿用现有海报渲染和保存流程。
- 分享链接：Debug 按 iOS 调试规则生成无二维码海报；Release 调起系统分享，附带当前环境帖子链接、首张原图和正文。
- 复制链接：使用真实帖子 ID 和当前 API 环境，不使用固定测试编号。
- 导出视频：使用原始 `imgs` 列表，剔除空项后保持顺序。任意图片无效或下载失败时停止导出，不跳过图片生成不完整视频。

视频使用本地 Canvas 绘制、MediaCodec H.264 编码和 MediaMuxer MP4 封装，不请求服务端视频生成接口，
不需要新的第三方视频处理依赖。输入图片按方向解码、缩小到最长边 1280，并缓存在工作目录；
编码时只保留当前原图和少量帧，避免同时解码全部原图。

| 项目 | 规则 |
| --- | --- |
| 输出 | 720×1280 竖屏，30fps，H.264，约 2.8Mbps |
| 原图 | 每张 3 秒，完整展示并缓慢放大，柔化背景，多图显示位置指示点 |
| 介绍 | 5 秒，展示公开标签、介绍、地址和送养状态；过长内容截断并提示下载 App 查看完整介绍 |
| 搜索引导 | 2 秒，突出真实帖子编号 |
| 下载引导 | 3 秒，下载二维码、帖子编号及“最新状态以 App 内为准” |
| 切换 | 内容区域 0.35 秒淡入过渡，底部品牌区固定 |
| 配乐 | 默认“快乐口哨”；也可选“阳光漫步”“轻快尤克里里”“无音乐” |
| 音量 | 循环到视频结尾，45% 音量，0.7 秒淡入、1.2 秒淡出 |

“设置 → 背景音乐管理”支持保存选择、试听和停止试听，离开页面停止播放。
配乐及其来源记录迁移自 iOS，位于 `app/src/main/assets/ShareMusic`，许可记录见其中 `LICENSE.md`。

生成完成进入独立全屏“视频预览”页，提供播放控制、“保存到相册”和“分享视频”。
保存失败可重试；不会自动保存或自动发送到其他应用。
Android 10+ 使用 MediaStore，不申请保存权限；Android 9 仅在点击保存时申请写入权限。
保存中或正在系统分享时禁用重复操作。

导出期间显示进度并防止重复发起；页面退出、切后台、账号变化时取消正在进行的导出。
编码和配乐处理各有 120 秒超时检查。输出前以及保存/分享前重新检查帖子公开内容和状态，
避免导出已删除帖子或继续使用已变化的内容。渲染器不接收联系方式、申请人资料或登录凭证。
关闭预览清理原临时视频；提供给其他应用的分享副本保留最多约一天，在下次进入分享流程时清理。

## 验证

仅编译及单元测试，不运行 APK 打包任务：

```sh
./gradlew :app:compileNoAdsDebugKotlin :app:compileDomesticDebugKotlin \
  :app:testNoAdsDebugUnitTest :app:testDomesticDebugUnitTest
```

新增 `VideoExportPlanTest` 覆盖照片顺序和时长、无图输入、无编号时间线、配乐音量曲线及内容快照变化。
分享/视频开发时两个 Debug flavor 各 103 项测试通过；后续联系方式链路调整后，合并验证各 110 项通过，Debug/Release 编译均通过。编译和测试无法证明设备编码器兼容性或真实分享目标行为。
本次没有连接 Android 测试设备，尚未验证真机画面、音画同步、相册保存和系统分享。

真机验收需覆盖：单图/多图/旋转照片、长介绍和标签、结束送养状态、三首配乐/无音乐、
中途切后台、下载失败、API 28 权限拒绝与重试、API 29+ 保存、分享后返回及文件可读性。
只有用户明确要求后才打包 APK。
