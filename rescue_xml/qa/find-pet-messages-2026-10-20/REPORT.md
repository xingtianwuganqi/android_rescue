# 找宠推荐消息适配验证 · 2026-10-20

依据 front_demand_v3.md 的 2026-10-20 修订实现 Android 适配。

## 结果

- 全量 JVM 测试 162 项，失败 0，错误 0，跳过 0。其中 FindPetFlowTest 51 项，使用真实 Retrofit、MockWebServer 和 ViewModel。
- :app:assembleDebug、:app:compileDebugAndroidTestKotlin 通过；最终增量复核 BUILD SUCCESSFUL。
- git diff --check 通过。
- adb 检测到设备处于 unauthorized，未执行真机页面/设备测试。
- 没有调用生产写接口，未执行服务端迁移；真实推荐投递及迁移后的历史已读状态需部署后联调。

## 本轮覆盖

1. 推荐只使用 V3 GET，查询不自动已读；没有新增 authmessage 推荐查询。
2. message_id=7、recommendation_id=11、topic_id=123 独立；已读路径使用 11，导航使用 123。
3. authunreadnum 保持原 POST/body token，读取 find_pet_unread。缺失和 null 按 0；sys=0、like=1、find_pet=1 总数为 2，V2 申请提醒继续单独计入原总数。
4. 系统通知保留历史正文，不再解析推荐路由，不触发找宠状态变化；推荐独立统计，不再将列表 unread_count 额外相加。
5. 已读失败保持未读；成功即时更新。随后目标刷新失败不会撤销已读；重复已读不重复扣减。旧账号成功及 401 响应均被隔离。
6. 关闭需求历史、不可用帖子占位、分页失败保留数据及原发布/编辑/联系/互动流程保持回归通过。
7. 推荐来源显示“系统推荐”。依据最新版文档移除发布额外确认框，页面固定说明、原校验及写后本人资料重查保留。

## 构建命令

```sh
./gradlew -I qa/adoption/qa.init.gradle -I qa/promotion/qa.init.gradle \
  :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin \
  --offline --console=plain
```

安装包：app/build/outputs/apk/debug/app-debug.apk。

构建仍有现存广告 SDK 的 D8 栈表警告及已有测试代码的可空类型警告，未阻断构建。
