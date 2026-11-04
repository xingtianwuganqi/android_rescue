# 真实测试环境联调 · 2026-10-25

**后续修复状态：旧找宠HTTP403/code401识别问题已修复，原真实环境失败用例现已通过；208项离线回归及Debug构建通过。见[修复报告](../legacy-auth-fix-2026-10-25/REPORT.md)。以下保留修复前实测结果，V4路由404仍未解决。**

用户确认使用 http://test.rxswift.cn/。本次已连接该真实环境，测试时间约上海时间14:26–14:31（请求记录使用UTC）；不是本地Mock。**结果未全部通过。**

没有修改业务实现、部署服务器或执行迁移。没有可用测试账号及连接的Android设备，因此只进行了匿名/故意失效凭证的真实接口验证，以及Android生产网络类在JVM中的真实HTTP联调；未执行设备UI或登录后的双账号业务流程。

## 核心结果

| 项目 | 实测结果 | 判断 |
| --- | --- | --- |
| V4埋点 POST /api/v4/analytics/events/ | 正常匿名page_view、重复ID、消息列表类型、校验错误和无效认证等8种请求均为HTML HTTP404 | 当前测试地址路由不可用，无法验收accepted/duplicate/字段与身份校验。8项失败来自同一环境阻断，不能视为8个独立客户端bug |
| V4路由补充诊断 | GET带斜杠、OPTIONS无斜杠同为HTML404，没有重定向到其他主机 | 不是仅改变末尾斜杠即可解决；需要核对测试环境部署/URL/代理路由 |
| 旧找宠本人资料 + 无效Token | 服务端HTTP403，JSON业务code401；FindPetRepository实际抛status403而非401 | 客户端兼容缺陷：未按旧业务码识别登录失效。不能误当403账号受限，也不能把所有新V2的403改成401 |
| V2资料/本人申请/收到申请/通知/未读 | 匿名GET返回JSON401；实际AdoptionRepository对无效Token识别401 | 鉴权入口可达并正确拒绝，不能据此证明登录后的权限/迁移全可用 |
| V3本人推荐 | 匿名GET返回401 AUTH_REQUIRED；实际FindPetRepository对无效Bearer识别401并触发注入的失效回调 | 推荐鉴权入口可达；未获取真实账号的推荐历史或修改已读 |
| 公开找宠列表 | HTTP200/code200；实际FindPetRepository解析成功，原始contact被清除 | 匿名列表及现有DTO适配通过 |
| 首页推广feed | 有效首屏meta、page2同一snapshot、无重复topic；实际FeedRepository+FeedCursor通过 | 当前环境匿名快照分页链路可用 |
| 同城推广feed | 上海市查询为有效成功空列表，has_more=false，meta完整 | 成功空态协议可用；未验证有数据城市的全部地域匹配规则 |
| 快照范围不匹配 | 首页/同城使用原snapshot把size从10改为11，均返回409 FEED_SNAPSHOT_MISMATCH | 错误分流协议可用 |
| 推广资格/领养状态 | 匿名GET返回JSON401；推广POST路径OPTIONS为200 | 路由存在；没有播放广告、伪造奖励或提交推广POST |
| 旧未读统计匿名调用 | HTTP200/code200，含find_pet_unread；私人like/collect/comment/find-pet计数为0，系统公共公告可非0 | 保留原匿名公共公告行为；不能要求所有计数为0或该接口必须401 |

## 数量与可复现记录

- HTTP合同检查：25项，17通过、8失败；另有2个路由诊断不计入上述25项。
- Android实际网络类：6项，4通过、2失败。失败分别为V4预期200实际404，以及旧找宠info预期认证失效401实际被客户端识别为403。
- Android网络类测试使用真实 Retrofit/Gson、FindPetRepository、AdoptionRepository、FeedRepository/FeedCursor、AnalyticsTransport。JUnit失败是实测缺陷证据，本次没有把它改成错误行为断言来制造全绿。
- 本次上报尝试只使用随机匿名标识和白名单页面，不含真实账号、联系方式或正文。V4请求均404，没有获得任何accepted响应；无法证明事件入库。未保存网络完整响应正文、Token或个人资料，仅保存状态、字段名、数量和错误码。
- 命令中的Gradle --offline仅限制依赖解析；本测试的HTTP请求确实发送到了test.rxswift.cn，不表示本次为离线Mock。

证据文件：summary.json、smoke-results.json、extra-readonly-results.json、route-diagnostics.json、android-staging-results.xml/json。smoke.py及kotlin/StagingContractTest.kt为可复用脚本；staging.init.gradle显式启用网络测试，不加入默认离线201项测试。

## 新发现的客户端兼容问题

旧接口返回HTTP403/业务code401时，FindPetRepository.legacy将错误交给AdoptionErrors.parse；AdoptionError.status在非200时优先使用HTTP状态，因此变成403。fail仅在status==401时清理失效会话。真实测试已复现，找宠本人资料页面会走403处理，用户仍保留失效凭证，不能按需求自动进入重新登录流程。

修复应限定旧Form接口的业务码归一化，同时保留账号revision隔离；新V2/V3的权限403、BLACKLISTED等不能因此被全局登出。此问题不同于上轮审查发现的旧awaitResp在HTTP401/403时不恢复协程；本次FindPetRepository使用独立suspend请求，仍有自己的状态归一化缺口。

## 尚未进行的测试

缺少两个专用测试账号，以下仍未验证：实际登录、领养资料保存、申请→同意→读取联系→完成/关闭、两账号未读互不串号、本人找宠首次创建/修改/开启/关闭与真实推荐投递、推荐单条已读、实际获取联系、真实SDK奖励和推广权益写入。没有使用或猜测已有业务账号，也未改动现有用户数据。

当前adb没有连接设备，未执行Android页面交互、旋转、权限弹窗、后台30分钟、海报保存或真广告验收。上轮审查中的推荐404占位与发布点击漏埋点，不能据本次鉴权探测宣称已在真机复现或修复。

## 后续顺序

1. 核对并修复测试地址的V4部署/代理/路由，使正常事件返回JSON200 accepted、同ID返回duplicate，再重跑埋点校验用例。
2. 修复旧找宠info的HTTP403/code401兼容处理并重跑真实Repository测试。
3. 使用送养人/申请人两个专用测试账号补齐登录后业务链路；连接设备完成页面及SDK验收。

## 重跑

在仓库根目录运行HTTP探测（会尝试写入少量匿名测试埋点，不写领养/找宠业务）：

```sh
python3 rescue_xml/qa/test-environment-2026-10-25/smoke.py
```

在rescue_xml目录执行Android真实HTTP测试：

```sh
./gradlew -I qa/adoption/qa.init.gradle -I qa/promotion/qa.init.gradle \
  -I qa/test-environment-2026-10-25/staging.init.gradle \
  :app:testDebugUnitTest --tests 'com.rescue.flutter_720yun.staging.StagingContractTest' \
  --offline --console=plain
```

修复前该JUnit命令预期因上述两项失败返回非零；不要将其结果表述为构建编译失败或全部测试通过。
