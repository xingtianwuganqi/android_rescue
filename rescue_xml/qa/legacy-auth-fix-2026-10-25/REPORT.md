# 找宠旧接口登录失效兼容修复 · 2026-10-25

## 修复

FindPetRepository.legacy在解析旧Form接口失败包后，仅将业务code=401且尚未识别为401的错误归一化为认证失效，再调用现有fail/invalidate流程。由原UserManager.logout及LoginEvent清理登录态和私人状态，复用原页面登录流程。

原HTTP200/code401、HTTP401及HTTP403/code401都可进入认证失效处理。业务202拒绝及真正的403权限错误不登出；V2/V3 REST解析和公共AdoptionError状态规则不作修改。错误message、error_code、字段信息和retryAfter沿用解析结果。请求完成和触发登出前仍检查原账号快照，旧账号迟到失败不能退出新账号。

此修复覆盖使用该legacy适配层的找宠旧接口及旧消息统计调用，没有将新REST权限403统一当成登录失效，也没有更改旧awaitResp或埋点传输行为。

## 验证

- 全量208项JVM测试通过，0失败、0错误、0跳过。
- 新增7项：HTTP403/code401、HTTP200/code401、HTTP401非JSON、真正的旧403、业务202、新V3不套旧归一化规则、切号后旧HTTP403/code401不能登出新账号。
- Debug APK及Android设备测试源码编译通过，git diff --check通过。
- 真实测试地址http://test.rxswift.cn/的原失败用例legacyInfoExpiredTokenMustTriggerLoginInsteadOfAccountRestriction通过。使用故意失效凭证，只读查询本人信息；生产Repository识别status401并触发一次注入的退出回调。
- 真机未连接，未验证屏幕上的登录导航。没有创建/修改实际用户业务数据。V4测试环境路由404为独立问题，本轮未部署或修复。

证据：regression-results.json、FindPetFlowTest.xml、staging-fixed-results.xml/json。

```sh
# rescue_xml目录
./gradlew -I qa/adoption/qa.init.gradle -I qa/promotion/qa.init.gradle \
  :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin --offline --console=plain

./gradlew -I qa/adoption/qa.init.gradle -I qa/promotion/qa.init.gradle \
  -I qa/test-environment-2026-10-25/staging.init.gradle \
  :app:testDebugUnitTest \
  --tests 'com.rescue.flutter_720yun.staging.StagingContractTest.legacyInfoExpiredTokenMustTriggerLoginInsteadOfAccountRestriction' \
  --offline --console=plain
```

APK：app/build/outputs/apk/debug/app-debug.apk。
