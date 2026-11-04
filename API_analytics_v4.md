# V4 埋点接口与部署说明

所有事件由客户端上报，后端登录、注册、发布流程不自动记录事件。登录/发布成功事件属于客户端自报数据，不能代替业务表、审计日志或安全判断。

## 1. 单事件接口

`POST /api/v4/analytics/events/`（兼容无末尾斜杠），`Content-Type: application/json`。

未登录不传认证头；已登录传 `Authorization: Bearer <token>`，复用现有 UserToken。传了无效/失效 Token 返回401，不会降为匿名。客户端不应因埋点401弹登录页或自动刷新凭证。

```json
{
  "event_id": "14b921cb-458b-457e-a655-04aec062d8f0",
  "event_name": "publish_page_view",
  "anonymous_id": "21a2cfc1-0304-4ecb-a52c-67b471095d9e",
  "session_id": "75477b3d-4fa8-43bc-8094-a86cba8a6f6d",
  "sequence": 4,
  "occurred_at": "2026-10-21T10:01:05+08:00",
  "platform": "ios",
  "app_version": "1.0.0",
  "page": "publish_adoption",
  "properties": {"publish_type": "adoption"}
}
```

示例时间为格式示意，实际填写事件发生时间。每个请求只有一个对象，不能传数组或 events 包装。请求最多8 KiB，properties 最多2 KiB。未知顶层字段和扩展字段一律拒绝，包括 user_id、source、ip、password、token 等；认证凭证只在请求头中使用。

| 字段 | 要求 |
| --- | --- |
| event_id | 必填 UUID，每次真实操作生成一个；重复投递沿用原值 |
| event_name | 必填，见事件白名单，大小写敏感 |
| anonymous_id | 必填 UUID，首次运行随机生成并持久化，代表一个安装实例 |
| session_id | 必填 UUID，首次会话生成；连续闲置达到30分钟后下次操作更换，退出/换账号也更换，登录成功沿用 |
| sequence | 必填整数1～2147483647，同一会话客户端事件递增；不是唯一键 |
| occurred_at | 必填，带时区 ISO 8601；最近10分钟至服务端当前时间后5分钟 |
| platform | 必填，ios / android / other / unknown |
| app_version | 可选字符串，最多32字符，默认空 |
| page | 可选，固定页面编码；page_view 必填 |
| properties | 可选 JSON 对象，默认空对象，只允许相应事件的白名单属性 |

页面白名单：`home`、`login`、`publish_selector`、`publish_adoption`、`publish_find_pet`、`publish_show_pet`、`topic_detail`、`messages`、`profile`、`adoption_profile`（领养资料）、`message_list`（消息详细列表）。未传页面时默认空字符串。

## 2. 事件与属性

| 事件 | 说明 | 允许的 properties（* 为该属性必填） |
| --- | --- | --- |
| app_open | 冷启动进入前台，或后台达到30分钟后恢复 | referrer_page |
| app_quit | 退出前台时尽力发送，强制结束进程可能丢失 | 无 |
| page_view | 页面实际展示，普通重绘不重复上报 | object_id、referrer_page、message_list_type（message_list 页面必填） |
| login_click | 点击登录入口 | login_method、referrer_page |
| login_success | 取得登录成功响应和 Token 后上报 | login_method |
| login_failed | 登录失败，可匿名上报 | login_method、reason_code* |
| publish_click | 点击发布入口 | publish_type、referrer_page |
| publish_page_view | 实际打开发布表单，page 与类型一致 | publish_type*、referrer_page |
| publish_success | 收到新建成功响应后上报，编辑不算新发布 | publish_type*、object_id* |
| logout | 清除当前登录态前尽力上报，然后更换会话 | 无 |
| logOff | 发起注销操作前尽力上报，账号删除会清理相关数据 | 无 |
| export_image | 帖子详情页点击导出分享图 | publish_type*、object_id* |
| export_video | 帖子详情页点击导出分享视频 | publish_type*、object_id* |
| click_video_ad | 点击观看推广激励视频的操作按钮 | ad_placement*、object_id* |
| video_ad_success | 推广接口确认原观看请求已成功取得权益 | ad_placement*、object_id*、reward_attempt_id* |

`login_success`、`publish_success`、`logOff`、`click_video_ad`、`video_ad_success` 必须携带有效 Token；其他事件允许匿名。Token 只证明上报账号，后端不核验事件对应的业务结果、对象存在性或所有权，也不由埋点触发业务操作。

属性枚举：

- publish_type：`adoption`（送养）、`find_pet`（找宠）、`show_pet`（秀宠）。
- object_id：正整数，最大9223372036854775807，不接受布尔值或数字字符串。
- login_method：`password`、`sms`、`email`、`wechat`、`apple`、`other`。
- reason_code：`invalid_credentials`、`invalid_code`、`account_unavailable`、`cancelled`、`network_error`、`provider_error`、`unknown`。
- referrer_page：同页面白名单。
- ad_placement：`my_posts`（我的页面领养/我的发布入口）、`publish_success`（发布成功页），与原推广接口 scene 对应。只用于两种广告事件。
- reward_attempt_id：原推广请求使用的 UUID 字符串，只用于 video_ad_success，不新造或使用广告 SDK 的 placement ID 代替。

`publish_page_view` 的 page 必须分别为 `publish_adoption` / `publish_find_pet` / `publish_show_pet`。选择发布类型前可以只报 publish_click，不传 publish_type。

## 3. 响应与客户端发送规则

成功和重复均返回 HTTP 200：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "event_id": "14b921cb-458b-457e-a655-04aec062d8f0",
    "status": "accepted"
  }
}
```

重复 event_id 返回 `status: duplicate`，不覆盖原事件，不返回原事件内容。

| HTTP 状态 | data.error_code | 含义 |
| --- | --- | --- |
| 400 | INVALID_EVENT | 结构/字段/时间错误，data.fields 给出字段说明；超出时间窗口含 EVENT_TIME_OUT_OF_RANGE |
| 401 | AUTH_REQUIRED | Token 格式错误、失效或该事件必须登录 |
| 403 | ACCOUNT_RESTRICTED | 已删除或受限制的账号 |
| 413 | REQUEST_TOO_LARGE | 请求大于8 KiB |
| 415 | REQUEST_FAILED | 不是 application/json |
| 429 | RATE_LIMITED | 请求过多，包含 Retry-After 响应头 |
| 503 | STORAGE_UNAVAILABLE / DISABLED | 暂时无法写入 / 全局关闭采集 |

每次点击/进入页面立即异步发送，建议请求超时5秒；业务与导航不等待响应。第一期无离线队列、定时发送、批量上报或自动重试，失败不弹错误，允许少量漏报。事件生成时固定身份、会话和时间；不能在登录后用新账号 Token 发送原匿名/旧账号事件。

App 启动即默认启用本次使用情况采集，不等待用户同意、不新增埋点授权弹窗或用户设置入口；不以登录、推送、定位或 ATT 授权作为开始采集的前置条件。原业务接口不需要额外埋点请求头，接口失败、离线或未接入客户端时后端不会补造事件。

## 4. Admin 与统计

- Admin 首页“埋点统计 → 行为事件”进入概览，或访问 `/admin/analytics/overview/`。
- 用户列表和用户详情页提供“行为路径”链接；`/admin/analytics/users/{user_id}/path/` 也可查看。
- 匿名访客路径：`/admin/analytics/visitors/{anonymous_id}/path/`；原始事件入口位于概览导航。
- 时区固定 Asia/Shanghai，默认今天，支持昨天、7天、30天、自选最近90天；按 occurred_at 归属日期。
- 次数为去重 event_id 后的记录数；访客按 anonymous_id 去重，账号按有效 Token 确定的 user_id 去重。区间 UV 不等于每日 UV 相加，访客和账号不能相加。
- 用户路径只关联同安装实例、同会话、客户端 login_success 之前30分钟的匿名行为；存在其他账号冲突时不关联。匿名历史数据不会回填成已登录数据。
- IP 位置是接收时网络出口估算，VPN、代理、移动网络等会影响准确性。

员工需要分配：`analytics.view_analytics_overview`（概览）、`analytics.view_analyticsevent`（事件/路径）、`analytics.view_analytics_sensitive`（完整 IP）。普通 staff 不自动获得访问权。事件只读，不允许 Admin 新增/修改/删除；完整 IP 默认掩码，原始详情也受敏感权限控制。

## 5. 部署与离线地址库

先安装项目 requirements（新增 `py-ip2region==3.0.6`），再执行：

```sh
python manage.py migrate analytics --noinput
python manage.py install_analytics_geo
python manage.py check_analytics_geo
```

start.sh 已包含 analytics 迁移。部署迁移只新增 `analytics_analyticsevent` 一张表，不修改旧业务表。不要使用 fake 跳过。

安装命令显式访问官方仓库，默认固定提交 `f8b0792b0cd729f5b2593f690aeed225e49c1967`，先下载校验 IPv4/IPv6 两份文件，再替换。源码参考：[ip2region](https://github.com/lionsoul2014/ip2region)。文件在 `var/ip2region/`，已加入 .gitignore，不随 Git 分发；服务器需自行执行安装或挂载已校验的数据库目录。`source.json` 记录数据提交。不得将本机已下载文件误认为部署服务器已安装。

每月检查上游更新后，可通过 `--ref <官方提交SHA>` 指定新版本重装；更新后重启 Web worker 重载。运行期只做本地查询，不在每次上报时联网。未配置或查询失败仍正常存储事件，位置显示未知。

| 环境变量 | 默认值 | 用途 |
| --- | --- | --- |
| ANALYTICS_ENABLED | 1 | 0 表示全局停止接收 |
| ANALYTICS_GEO_DIR | 项目根目录/var/ip2region | IPv4/IPv6 xdb 的挂载目录 |
| ANALYTICS_TRUSTED_PROXIES | 空 | 逗号分隔可信代理 IP/CIDR，默认忽略转发头 |
| ANALYTICS_IP_RATE | 600/min | 单来源 IP 的请求上限 |
| ANALYTICS_VISITOR_RATE | 120/min | 单安装实例请求上限 |

可信代理仅配置实际部署的 Nginx/CDN 网段，不能设置为所有地址；按代理链从右向左找到第一个不可信节点作为客户端来源。限流复用项目现有 Django cache，生产多 worker 应共用现有缓存；DRF 缓存限流不是严格的并发配额或防攻击服务。

每天通过部署现有调度执行以下命令（本次代码未在服务器设置调度）：

```sh
python manage.py purge_analytics_events --dry-run
python manage.py purge_analytics_events --batch-size 1000
```

保留上海时间今天及之前89个自然日，旧数据分批删除。第一期没有长期汇总，过期日期不提供统计。现有账号软删除（UserInfo.save）或物理删除会清除该账号事件及其关联会话内匿名事件；直接使用 QuerySet.update 修改删除状态会绕过 Django 信号，维护脚本须显式调用 `apps.analytics.signals.purge_user` 或使用 save。

## 6. 隔离验证

本地可使用 SQLite 快速测试：

```sh
python manage.py test apps.analytics.tests --settings=apps.analytics.test_settings --noinput
```

MySQL 测试使用已有隔离测试参数 `V2_TEST_MYSQL_DATABASE`、`V2_TEST_MYSQL_USER` 等；可另设 `ANALYTICS_TEST_MYSQL_SOCKET` 指向隔离 socket。测试配置不会使用部署数据库或 Redis。真实客户端的生命周期、会话持久化、启动默认采集、异步不阻塞和失败不重试仍需客户端联调验收。


## 2026-10-22 新增：领养资料与消息详细列表

新增页面编码：`adoption_profile`（领养资料页）、`message_list`（消息详细列表页）。消息首页仍用 `messages`，不是 `message_list`。

领养资料页在内容实际可见时上报；编辑输入、校验、保存、加载完成和普通重绘不重复计页面曝光。消息列表在实际打开或切换到另一种列表时上报；刷新、分页、已读状态变化不重复计曝光。

以下仅展示事件相关字段，实际请求仍需补齐 event_id、anonymous_id、session_id、sequence、occurred_at、platform 等必填字段：

```json
{
  "event_name": "page_view",
  "page": "adoption_profile",
  "properties": {"referrer_page": "profile"}
}
```

```json
{
  "event_name": "page_view",
  "page": "message_list",
  "properties": {
    "message_list_type": "like",
    "referrer_page": "messages"
  }
}
```

`page_view` 且 `page=message_list` 时，`properties.message_list_type` 必填，取值如下：

| message_list_type | 消息列表 |
| --- | --- |
| like | 点赞 |
| collect | 收藏 |
| comment | 评论 |
| system | 系统消息 |
| find_pet | 我的找宠（推荐列表） |
| my_applications | 我的申请 |
| received_applications | 收到的申请 |

该字段只允许用于 `page_view` 的 `message_list` 页面；不能放在顶层，不能使用后端 msg_type 数字、列表标题或自由文本代替。`adoption_profile` 不携带该字段。新增页面也可作为 properties.referrer_page；message_list_type 描述当前页面的列表类型，不描述来源页面。

后端 Admin 的概览、事件列表和路径页支持按页面及消息列表类型筛选；这次扩展使用现有 page 和 properties，不增加数据表或迁移。


## 2026-10-22 新增：推广激励视频事件

统一使用 `click_video_ad` 和 `video_ad_success`，不使用 click_my_video_ad / my_video_ad_success，也不为两个入口分别定义同名重复事件。通过 `properties.ad_placement` 区分 `my_posts` 和 `publish_success`；后端 Admin 可按“推广广告入口”筛选。

两种事件都需要有效 Bearer Token，object_id 是送养帖子 ID。page 可不传；不要把推广 scene=publish_success 直接填入 page，它不是当前页面编码白名单中的值。属性不传广告素材标题、SDK 原始回调或自由文本。

- 点击：用户点击“观看视频/增加曝光”操作按钮时记录，不是广告素材的落地页点击。一次实际按钮点击生成一个 event_id，广告加载、SDK 播放与关闭回调不重复生成点击。
- 成功：收到推广 POST `/api/v2/topics/{id}/promotion` 的 HTTP200、业务 code=200 后记录一次。仅 SDK 播放结束、广告关闭或奖励回调都不能说明已经取得推广权益。
- 使用原 attempt 的 scene、帖子 ID、reward_attempt_id 和账号上下文；原业务奖励恢复提交/重试逻辑继续保留，不能因为埋点失败再提交推广业务。
- 同一 reward_attempt_id 只记一次成功。建议成功 event_id 直接沿用该 UUID（点击事件单独生成 UUID），使重复成功回调/幂等回放仍由事件表唯一约束去重。不能在每次回放成功时创建新的成功 event_id。
- 成功 occurred_at 使用首次得到推广业务成功确认的当前时间；不把很久以前的观看时间当成新上报时间。来源始终取原 attempt，不能取恢复提交时所在的新页面，也不能使用已切换账号的 Token。
- 原业务可以恢复奖励提交；埋点本身仍是单次异步发送，不新增离线补报、队列或自动重试。不保证埋点与权益写入原子一致，统计是客户端自报数据。

以下为部分字段示例，实际请求需携带完整必填字段和 Bearer 头：

```json
{
  "event_name": "click_video_ad",
  "properties": {"ad_placement": "my_posts", "object_id": 123}
}
```

```json
{
  "event_id": "67f5c6a9-2ba1-4910-a305-8d0ac8d1a68b",
  "event_name": "video_ad_success",
  "properties": {
    "ad_placement": "publish_success",
    "object_id": 123,
    "reward_attempt_id": "67f5c6a9-2ba1-4910-a305-8d0ac8d1a68b"
  }
}
```

后端接收白名单、属性校验及员工筛选已支持这两个事件，不在后端推广流程自动产生埋点。迁移 `analytics.0002_alter_analyticsevent_event_name` 只同步事件选项状态，不新增表或列；升级执行 `python manage.py migrate analytics --noinput`。
