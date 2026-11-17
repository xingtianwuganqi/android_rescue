# Android 端多语言与日韩接入（中日韩）

- 文档版本：V1.0
- 更新日期：2026-11-16
- 工程：`rescue_xml`（当前完整业务工程）
- 后端基线：`rest_framework/docs/japanese_korean_support_backend.md`
- 客户端基线：本文件
- 当前资源语言：
  - 简体中文：`values/`
  - 日文：`values-ja/`
  - 韩文：`values-ko/`

> `rescue_compose` 是独立工程，本文件默认只覆盖 `rescue_xml`；如要同步 Compose 工程，需要单独确认。

## 1. 产品约束

日韩首期只支持：

- 邮箱注册、邮箱登录、邮箱找回
- 不支持手机号登录、手机号绑定、短信验证码
- 不支持微信登录
- 当前后端 Apple 登录 `client` 只允许 `ios`，Android 日韩端不接 Apple 原生登录
- 用户发布的帖子、评论、昵称等 UGC 不做自动翻译

因此 Android 端：

1. 日韩界面只展示邮箱登录入口。
2. 不展示手机号、微信、短信验证码、Apple 原生入口。
3. 所有请求带当前语言和 region。
4. 根据后端 `auth_methods` 动态隐藏不支持的登录方式。
5. 用户可见文案优先使用 `values-ja`/`values-ko`；后端 `message` 只作为未知错误的兜底。
6. 不要依赖后端把错误文案完整翻译；按 `error_code` 做本地映射。

## 2. 语言与 region

| 概念 | 取值 | 说明 |
| --- | --- | --- |
| 展示语言 | `zh-Hans`、`ja`、`ko` | 请求头可发送 `zh-CN`、`ja-JP`、`ko-KR` |
| 业务地区 region | `CN`、`JP`、`KR` | 两个大写字母，不能用 `ja`/`ko` 代替 |
| 默认 region | `CN` | 未传/非法时后端按 CN；日韩客户端必须显式传 JP/KR |
| 默认语言 | `zh-hans` | 日韩客户端必须显式带 `Accept-Language` |

### 2.1 首次启动

- 日本版：`region = JP`
- 韩国版：`region = KR`
- 语言跟系统或用户选择；语言不能决定 region。
- 账号 region 第一期固定；切区走单独账号迁移流程，不能只改 `X-Region`。
- 修改系统语言可以刷新本地界面，但不能自动修改账号 region。

### 2.2 请求头

日本：

```http
Accept-Language: ja-JP
X-Region: JP
Content-Type: application/json
```

韩国：

```http
Accept-Language: ko-KR
X-Region: KR
Content-Type: application/json
```

可选：

```http
X-Language: ja
```

优先级建议：

1. App 内用户选择语言（`X-Language`）
2. 系统/当前 `Accept-Language`
3. 后端账号 `language`
4. 默认 `zh-hans`

`X-Region` 优先；如果 body 里也传 `region`，两者必须一致，否则后端返回 `REGION_MISMATCH`。

### 2.3 响应头

后端可能返回：

```http
Content-Language: ja
Vary: Accept-Language, X-Region
```

Android 不需要依赖 `Content-Language` 切换本地文案；以本地语言状态为准，但可用于日志和排障。

## 3. 当前 Android 资源与语言配置

已有：

```text
rescue_xml/app/src/main/res/
  values/strings.xml
  values-ja/strings.xml
  values-ko/strings.xml
  xml/locales_config.xml
```

`AndroidManifest.xml` 已配置：

```xml
<application
    android:localeConfig="@xml/locales_config"
    ... />
```

`locales_config.xml` 当前包含：

```xml
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="zh-Hans" />
    <locale android:name="ja" />
    <locale android:name="ko" />
</locale-config>
```

当前工程没有发现统一的 App 内语言切换实现；日韩接入时需要补：

```kotlin
val locales = LocaleListCompat.forLanguageTags("ja-JP")
AppCompatDelegate.setApplicationLocales(locales)
```

建议在设置页提供语言选择，并持久化到 `SharedPreferences`/DataStore。日韩版本默认跟随系统语言，系统不是日/韩时回退到对应市场默认语言。

## 4. 登录与认证

### 4.1 日韩登录入口

| 平台 | Apple | 邮箱 | 手机号 | 微信 | 短信 |
| --- | --- | --- | --- | --- | --- |
| Android 日本 | 不支持 | 支持 | 不支持 | 不支持 | 不启用 |
| Android 韩国 | 不支持 | 支持 | 不支持 | 不支持 | 不启用 |

登录/注册/资料响应会返回：

```json
{
  "region": "JP",
  "language": "ja",
  "auth_methods": ["apple", "email"],
  "service_node": "overseas",
  "api_base_url": ""
}
```

Android 端应：

- 根据 `auth_methods` 展示登录入口；Android 日韩只展示邮箱；
- 不展示手机号、微信、短信、Apple 原生入口；
- `api_base_url` 非空时切换到该地址；为空时使用当前环境地址；
- `service_node` 用于埋点和排障，不作为权限判断。

### 4.2 邮箱注册

```http
POST /api/v1/register/
Accept-Language: ja-JP
X-Region: JP
Content-Type: application/json
```

```json
{
  "email": "user@example.jp",
  "password": "password",
  "confirm_password": "password",
  "language": "ja"
}
```

- 日韩不要传 `phoneNum`。
- `email` 必须是真实邮箱；Apple private relay 邮箱不能用于邮箱密码注册。
- 后端 `UserInfo.email` 已扩展到 254。
- 成功后返回 `token`、`region`、`language`、`auth_methods`、`service_node`、`api_base_url`。
- token 存入 `EncryptedSharedPreferences`/DataStore，不要写日志。

### 4.3 邮箱登录

```http
POST /api/v1/login/
Accept-Language: ko-KR
X-Region: KR
Content-Type: application/json
```

```json
{
  "email": "user@example.kr",
  "password": "password"
}
```

不要传 `phoneNum`。如果返回 `AUTH_METHOD_NOT_SUPPORTED`，不要自动改走手机号登录。

### 4.4 邮箱验证码和绑定

日韩只使用邮箱验证码：

- `POST /api/v2/tecent/code/`
- `POST /api/v2/tecent/check/`
- `POST /api/v2/login/bindphone/`：接口名保留历史命名，日韩用于绑定/校验邮箱
- `POST /api/v3/login/email/status`

请求示例：

```json
{
  "email": "user@example.jp",
  "code": "1234"
}
```

不要调用：

- `/api/v2/login/checkphone/`
- `/api/v2/login/phonestatus`
- 任何手机号/短信验证码接口

### 4.5 用户语言设置

用户切换语言后：

```http
POST /api/v1/updateuserinfo/
```

```json
{
  "token": "<token>",
  "username": "现有昵称",
  "avator": "现有头像",
  "language": "ko"
}
```

- `language` 只在用户主动切换时传。
- 登录/资料响应返回 `language`，用于恢复用户选择。

## 5. 传参清单

### 5.1 通用请求头

| Header | 必填 | 说明 |
| --- | --- | --- |
| `Accept-Language` | 是 | `ja-JP` / `ko-KR` |
| `X-Language` | 可选 | App 内强制语言，如 `ja` |
| `X-Region` | 是 | `JP` / `KR` |
| `Content-Type` | 是 | `application/json` |
| `Authorization` | V4 接口 | `Bearer <token>`；legacy 内容接口仍可能使用 body `token` |
| `Idempotency-Key` | Apple 接口 | Android 当前不走 Apple 原生登录，保留给未来 |

### 5.2 注册/登录字段

| 字段 | 类型 | 日韩要求 |
| --- | --- | --- |
| `email` | string | 必填 |
| `password` | string | 邮箱登录/注册 |
| `confirm_password` | string | 注册时必填 |
| `language` | string | 可选；`ja`/`ko` |
| `phoneNum` | string | 日韩不要传 |
| `region` / `X-Region` | string | `JP`/`KR`；优先 header |
| `phone_type` | string | 设备信息；不是登录凭据 |

### 5.3 响应字段

| 字段 | 说明 |
| --- | --- |
| `data.token` | 业务 token |
| `data.region` | 账号 region |
| `data.language` | 用户语言 |
| `data.auth_methods` | 如 `["apple","email"]`；Android 日韩只使用 `email` |
| `data.service_node` | `domestic`/`overseas` |
| `data.api_base_url` | 受控 API 地址，空字符串表示使用当前地址 |
| `error_code` | 稳定错误码，优先用它做判断 |

## 6. 错误处理

| HTTP/业务码 | `error_code` | Android 行为 |
| --- | --- | --- |
| 202/401 | `AUTH_REQUIRED` | 清理 token，跳登录 |
| 202 | `AUTH_METHOD_NOT_SUPPORTED` | 隐藏该方式；不要改走手机号/微信 |
| 202 | `REGION_NOT_AVAILABLE` | 展示“地区暂未开放”，不要回退 CN |
| 202 | `REGION_MISMATCH` | 展示账号 region 与请求不一致；不要自动切区 |
| 202 | `NODE_MISMATCH` | 使用响应 `api_base_url` 重新请求一次；避免循环 |
| 401 | `APPLE_CODE_EXPIRED` / `CHALLENGE_EXPIRED` | Android 当前不走 Apple；如未来接入，重新授权 |
| 409 | `APPLE_LINK_CONFLICT` | 不自动覆盖；提示联系支持 |
| 429 | `RATE_LIMITED` | 按 `Retry-After` 等待，不自动发短信 |
| 503 | `AUTH_TEMPORARILY_UNAVAILABLE` | 保留页面，稍后重试 |

原则：

- 以 `data.error_code` 为主，`message` 只做兜底。
- 不解析中文/日文/韩文 `message` 做逻辑判断。
- 日韩界面不能显示中文错误；本地必须有 `ja`/`ko` 文案。
- 后端未翻译的 `message` 可用本地通用文案替代。

## 7. 网络层接入要求

当前 Android 网络层分散：

- `network/ServiceCreator.kt`：主 Retrofit；已有 `headerInterceptor`，但 `.client(okHttpClient)` 当前被注释，需确认实际生效。
- `network/AdoptionServiceCreator.kt`：独立 Retrofit。
- `findpet/FindPetRepository.kt`：独立 OkHttp/Retrofit。
- `home/share/PosterRepository.kt`：独立 OkHttp。
- `analytics/AnalyticsTransport.kt`：独立 OkHttp。
- 其他直接 `Request.Builder()` 的调用。

日韩接入需要统一注入：

```kotlin
class I18nHeaderInterceptor(
    private val languageProvider: () -> String,
    private val regionProvider: () -> String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Accept-Language", languageProvider())
            .header("X-Region", regionProvider())
            .header("Content-Type", "application/json")
            .build()
        return chain.proceed(request)
    }
}
```

要求：

- 主 Retrofit、`AdoptionServiceCreator`、找宠、分享、统计等所有业务客户端都使用同一个 header 策略。
- `X-Region` 来自持久化 region，不从 `Locale.getDefault()` 推导。
- `api_base_url` 变化时重建 Retrofit 实例或使用动态 BaseUrl；不要固定死生产域名。
- 图片上传/下载等直接请求也要带 region；是否带语言按接口需要。

### 7.1 Base URL

- 主工程当前使用 `AppBuildConfig.BASEURL`。
- 领养使用 `BuildConfig.ADOPTION_BASE_URL`。
- 登录/资料响应出现非空 `api_base_url` 时，后续请求使用该地址；为空时保持当前环境地址。
- 不接受客户端任意传入的 URL；只使用服务端受控地址。
- `NODE_MISMATCH` 时最多重试一次，避免死循环。

## 8. 日期、数字、地址和 UGC

- API 时间戳按 ISO 8601 解析，展示时使用 `DateTimeFormatter.ofLocalized...` + `Locale.forLanguageTag("ja-JP"/"ko-KR")`。
- 数量/金额用 `NumberFormat.getNumberInstance(locale)`。
- 地址按市场使用日/韩格式；当前后端 `/api/location/` 仍返回中国地址目录，日韩地址数据未完成前不要在日韩界面展示中国省市数据。
- 日本联系方式可能包含 LINE/Instagram；韩国可能包含 KakaoTalk；不要强制填写微信。
- 帖子、评论、昵称保持原文；Android 不做自动翻译，除非产品另立翻译功能。
- 标签、违规类型、搜索热词使用后端返回的当前语言名称。

## 9. 推送、邮件和 Google Play

- 推送由后端按 region + language 选择受众。
- 客户端注册 FCM/推送 token 时同时上报 region、language、platform、app version、device token。
- Android 收到推送后按 payload 的 region/language 展示本地文案；不要把日文推送翻译成中文。
- 日韩不发送短信；验证码通过邮箱。
- 日韩用户协议/隐私政策需要日/韩版本和版本号；当前后端法律静态页仍以中文为主，日韩页面完成前不要链接中文法律文本作为日韩用户协议。
- Google Play 日/韩商店文案、截图、隐私标签、支持 URL 需要本地化。

## 10. 联调与验收清单

- [ ] 日文/韩文系统语言下，App 文案为日/韩，不出现中文
- [ ] 注册/登录请求带 `Accept-Language: ja-JP`/`ko-KR` 和 `X-Region: JP`/`KR`
- [ ] Android 日韩界面只展示邮箱登录，不展示手机号/微信/短信/Apple 原生入口
- [ ] `auth_methods` 从后端读取，不写死
- [ ] 邮箱注册/登录/验证码可用
- [ ] 手机号/微信/短信接口被调用时展示“不支持”，不静默失败
- [ ] `REGION_NOT_AVAILABLE` 不回落 CN
- [ ] `NODE_MISMATCH` 使用 `api_base_url` 重试且无循环
- [ ] `REGION_MISMATCH` 不自动改账号 region
- [ ] 发帖/评论/收藏/历史/消息按 region 展示
- [ ] 标签/违规类型/热词为当前语言
- [ ] 日期、数字、地址按日/韩格式
- [ ] 推送/邮件语言正确
- [ ] 切换语言后登录/资料接口返回新语言，后续请求使用新语言
- [ ] `values-ja` / `values-ko` 与默认中文 key、占位符一致
- [ ] 旧版本/旧接口回归通过

## 11. 待后端完成或联调确认

- 日韩地址目录和 `/api/location/` 的 region 适配
- 邮箱验证码/邮箱找回的最终接口契约
- 完整日/韩错误文案、协议/隐私、帮助页面
- 推送/邮件模板和发件人配置
- 日韩应用商店/版本/渠道配置
- 推荐、消息、领养、推广、图片的 region 隔离上线进度
- Android 是否未来通过 Web 方式接入 Apple 登录；当前后端 Apple 登录 `client` 只允许 `ios`

联调时以后端 `docs/japanese_korean_support_backend.md` 和实际接口响应为准。
