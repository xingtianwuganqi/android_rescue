# Android V3需求：保留原找宠，仅新增推荐

修订日期：2026-10-20。本文替换之前独立联系申请/主动推荐授权/无联系方式发布方案。后端已完成精简重构，接口契约见新版backend_demand_v3.md和API_v3.md；尚未部署，本文不表示客户端页面已实现。

## 1. 原找宠流程不变

保留原找宠发布、编辑、列表、本人信息、点赞收藏、获取联系方式、关闭和开启。发布仍填写城市、宠物类型、描述、联系方式，必填与校验沿用现有表单。不添加version/round/confirmed、领养问卷、独立申请审批、联系方式公开开关或账户城市登记。

发布页固定显示：

> 发布后，其他用户可通过平台获取你留下的联系方式。不希望继续被联系，可以关闭找宠信息。

只增加说明，不弹额外确认框。别人仍通过原获取联系方式入口和校验联系找宠人，不直接新增联系方式展示方式、不改原接口授权规则。关闭停止新增推荐和后续获取联系，重新开启沿用原逻辑，已复制的联系无法追回。

城市选择提交可选city_id，保持原address格式和其他参数；新版使用规范城市级ID，旧数据缺失仍按地址推荐。不额外改变原表单必填或旧响应格式。

## 2. 唯一新业务：同城送养帖推荐

每条送养帖累计最多推荐给10个不同用户，按符合条件的找宠信息update_time降序、ID降序选择；不足10人则按实际人数发送。发布及重新开启匹配共用上限，并发不突破10人；已读、关闭或删除需求不释放该帖推荐名额，帖子编辑不重置名额。已有历史推荐保留，不主动删除。

新送养帖发布后，平台按地域匹配有效找宠需求，并通过“我的找宠”推荐消息展示。发布找宠和从关闭重新开启时，最多检查10条候选历史送养帖；未通过资格复核时不继续扩扫。同需求同帖子只推荐一次；改描述、反复开启或城市改回不会再次提醒同帖。

只按地域，不按猫狗类型或描述偏好筛选。有合法city_id用ID，没有才可靠解析原地址；同市不同区县算同城，重名县需省市消歧。定位/浏览城市不能覆盖需求所在地。

消息页新增“我的找宠”推荐列表，展示原帖卡片：图片、描述、城市、送养状态、推荐时间、未读。**没有“联系申请”“送养人主动推荐”“我发出的推荐”入口。**送养人想联系找宠人，仍从原找宠列表获取对方留下的联系。

点击推荐进入原送养详情。联系送养人继续按front_demand_v2.md：申请领养、送养人同意沟通、读取联系；已有V2授权按原规则使用。系统推荐不自动申请、不开放新联系权限。

## 3. 历史与页面状态

| 状况 | 显示 |
| --- | --- |
| 没有找宠信息 | 提示发布找宠，提供原发布入口 |
| 有效但无推荐 | 暂无同城推荐，支持刷新 |
| 找宠已关闭 | 已关闭，提供原开启操作；保留历史推荐 |
| 帖子已完成/关闭且仍可公开 | 显示结束状态，按原详情禁用新申请 |
| 帖子删除/不可公开/作者注销 | 不可用占位，不展示私密内容 |
| 请求失败 | 保留已有内容并重试，不当作空结果 |

需求改城市、关闭后仍可查看旧推荐；历史推荐不再判为新授权，帖当前可申请性由V2判断。旧账号响应不能更新切号后页面。

## 4. 消息和未读

推荐消息存MessageList，msg_type=15表示系统找宠推荐；系统来源不是送养人主动联系。继续通过V3推荐列表展示“我的找宠”，不增加/api/v1/authmessage/推荐列表调用。

新推荐不写系统通知及系统未读表，不从/api/v1/systemnotification/获取或跳转。普通系统通知、点赞、收藏、评论继续原行为。

推荐列表查询不自动已读。点开单条推荐，调用V3已读接口修改MessageList.is_read；标记成功后更新角标，再进入原帖详情。只有指定接收人有权标记。系统通知页阅读不影响推荐。

/api/v1/authunreadnum/新增find_pet_unread，作为“我的找宠”角标；sys_unread/点赞/收藏/评论计数不包含它。App总数只加一次find_pet_unread，不能再把V3列表unread_count加一次。退出/切号时清理旧账号数据。

推荐项message_id为MessageList ID，替代原notification_id；页面可继续仅使用recommendation_id标记已读、topic_id打开详情。历史推荐迁移保留原已读状态。

## 5. 新增接口与兼容

仅新推荐接口使用Bearer登录：

- GET /api/v3/find-pet/me/recommendations/：本人历史推荐分页，page/size默认1/20；data含find_id/state/items/page/size/total/has_more/unread_count。
- POST /api/v3/find-pet/recommendations/{recommendation_id}/read/：空JSON，幂等标记本人推荐已读。

推荐项含recommendation_id/find_id/topic_id/message_id/read/created_at/topic_status/topic_available及公共帖卡片，不含联系方式、独立授权、source或target UUID。state仅为no_request/open/closed，不引入新找宠状态管理。

原/api/v2/searchpet/create、effective、info、findpet/list及原获取联系接口继续原参数、body token和业务code。仅发布/编辑可追加city_id，不切换为新V3发布接口。新推荐HTTP处理与旧业务码处理分开，不能一律把HTTP200视为成功。

| 新推荐HTTP / error_code | 前端处理 |
| --- | --- |
| 200 | 更新数据；已读成功刷新找宠未读及App总角标 |
| 400 INVALID_REQUEST | 修正参数 |
| 401 AUTH_REQUIRED | 登录并重读 |
| 403 ACCOUNT_RESTRICTED | 显示限制并停止操作 |
| 404 RESOURCE_UNAVAILABLE | 显示不可用，不尝试更换用户ID |
| 503 TEMPORARILY_UNAVAILABLE / 网络错误 | 保留内容，提示稍后重试 |

取消旧文档中的主动发送、contact-applications、approve/reject/revoke、专用推荐contact接口、版本/轮次冲突和账号城市登记处理，不再实现这些页面。

## 6. 验收

1. 发布及获取找宠联系方式与旧版一致，只增加发布说明和可选city_id。
2. 关闭后不能继续获取找宠联系、不新增推荐；历史推荐仍可看。
3. 新同城送养帖生成MessageList推荐消息，无类型匹配，无额外申请审批。
4. 推荐点击进入原详情，联系送养人完整沿用V2，不自动授权。
5. 推荐去重，系统通知和推荐已读互不干扰，角标不重复累加。
6. 完成/关闭显示结束状态，删除/不可公开显示不可用，错误与空列表区分。
7. 原发布、管理、获取联系、点赞收藏及V2申请回归通过，旧客户端无需迁入新策略。

## 7. 本轮后端变更与前端接入清单（2026-10-20）

| 项目 | 当前约定 | 前端调整 |
| --- | --- | --- |
| 推荐消息 | MessageList.msg_type=15，from_uid=0代表系统 | 文案标明系统推荐，不能显示为送养人主动联系 |
| 关联ID | message_id替代旧notification_id | 更新推荐模型；已读仍传recommendation_id，打开帖仍用topic_id，三个ID不可混用 |
| 已读来源 | MessageList.is_read，对应响应read | 列表加载不读掉全部；已读请求成功后更新对应卡片 |
| 找宠角标 | authunreadnum返回find_pet_unread | 新增字段缺失时按0兼容；与V3列表unread_count表示同一项，不能重复相加 |
| 系统通知 | 新推荐不写NotificationMsg及UnReadSystemMessage | 不从systemnotification/system-unread列表组装推荐，也不靠打开系统消息清除找宠未读 |
| 推荐列表 | 继续调用GET /api/v3/find-pet/me/recommendations/ | 不新增/api/v1/authmessage/的msg_type=15列表请求；后端消息类型不等于旧接口查询分类 |

消息页/App角标合计使用sys_unread + like_unread + collec_unread + com_unread + find_pet_unread；如果已有统一合计包含找宠，则不再加一次。列表返回的unread_count可更新同一个找宠计数状态，不能维护第二份独立累计数。系统通知列表已读只影响系统消息，找宠单项已读只影响找宠计数。

推荐列表请求示例（Bearer头使用本系统登录token，不在body传token）：

```http
GET /api/v3/find-pet/me/recommendations/?page=1&size=20
Authorization: Bearer <token>
```

响应结构示例：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "find_id": 12,
    "state": "open",
    "items": [
      {
        "recommendation_id": 31,
        "find_id": 12,
        "topic_id": 1024,
        "message_id": 78,
        "read": false,
        "created_at": "2026-10-20T10:00:00+08:00",
        "topic_status": "open",
        "topic_available": true,
        "imgs": [],
        "preview_img": [],
        "content": "同城宠物送养",
        "city_id": "310100",
        "city_name": "上海市",
        "owner": {"user_id": 8, "nickname": "送养人", "avatar": ""}
      }
    ],
    "page": 1,
    "size": 20,
    "total": 1,
    "has_more": false,
    "unread_count": 1
  }
}
```

点击示例中的卡片，POST /api/v3/find-pet/recommendations/31/read/，同样携带Bearer头，body为{}；成功data为{"recommendation_id":31,"read":true}。再用topic_id=1024进入原帖，不能用message_id=78代替帖子或推荐ID。已读失败保留原未读状态，按错误码处理；重复请求不会重复扣减角标，不能把计数减为负数。

/api/v1/authunreadnum/仍为原POST请求、body token，成功data新增find_pet_unread，例如：

```json
{
  "sys_unread": 0,
  "like_unread": 1,
  "collec_unread": 0,
  "com_unread": 0,
  "sys_un_list": [],
  "find_pet_unread": 1
}
```

上例总角标为2；sys_un_list沿用原业务载荷，推荐不生成新的系统未读条目。

联调还需验证：仅打开系统通知不改变找宠未读；仅获取推荐列表不改变找宠未读；已读/未读历史推荐迁移后保持状态；帖子结束或不可用仍可清除该条推荐未读；找宠关闭后仍可查看历史。部署前后端需要先执行新增0002_recommendation_message迁移；客户端不执行数据库迁移，也不自行重新生成历史提醒。原有历史系统通知正文可能保留，不能据其存在认定新推荐仍走系统消息。

本文仅同步需求和接口文档，不表示iOS或Android客户端代码已完成适配。

2026-10-20触发范围调整：推荐列表只查询已有推荐，打开、刷新、分页不扫描送养帖、不触发推荐。新送养帖发布后最多推荐给10人；发布找宠及从关闭重新开启时最多检查最新10条候选送养帖，只为当前用户匹配，不继续扩扫、不向其他人发推荐。旧送养帖编辑、状态变化不触发匹配。历史推荐保留，原全量补齐命令移除。
