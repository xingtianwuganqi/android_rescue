# Android 找宠 V3：主页找宠 Tab、找宠资料与同城推荐

修订日期：2026-10-19。目标工程：`/Users/jingjun/Desktop/Githup/android_rescue/rescue_xml`，Kotlin、XML、ViewBinding、Activity/Fragment、ViewModel、Retrofit/Gson；不迁移 Compose。

实现基线：`/Users/jingjun/Desktop/Githup/LaveCat/LoveCat/Class/FindPet/`、`Class/FindPetV3/`，包括当前未提交的首页头部及推荐列表样式调整。业务基线：iOS `front_demand_v3.md`、`docs/FIND_PET_V3_IMPLEMENTATION_2026-10-12.md`；后端契约：`/Users/jingjun/Desktop/Githup/rest_framework/API_v3.md`、`backend_demand_v3.md` 及当前找宠旧接口源码。送养申请继续以 Android `front_demand_v2.md` 为准。

本文把首页找宠、本人找宠资料、相关入口和 icon 集中成独立 Android 开发需求。现有 Android `front_demand_v3.md` 保留为推荐业务基线；本文件对首页、资料、发布确认、图标及列表交互提供更具体要求，相关冲突以本文和最新后端契约为准。**功能部分为开发需求；2026-10-19已同步10类图标、16份密度资源，并将消息“我的找宠”改为专用图标引用。资源处理及Kotlin编译通过；不表示其他待开发功能、后端部署或真机联调已完成。**

## 1. 范围与术语

“找宠”是发布**想领养宠物的意愿**，让合适的宠物和送养人找到自己；不是走失宠物寻回模块。保留原找宠发布、编辑、本人信息、公开列表、点赞收藏评论、获取联系、关闭和重新开启。V3唯一新增服务端业务是同城送养帖推荐。

| 页面/数据 | 用途与边界 |
| --- | --- |
| 首页 → 找宠 Tab | 公开找宠意愿列表；顶部“找宠小助手 / 去找宠”进入本人找宠资料 |
| 找宠资料 | 本人原 SearchPetModel 的类型、描述、地址、联系方式及开启/关闭管理；复用 FindPetDetailActivity |
| 消息 → 我的找宠 | 本人收到的同城送养帖推荐；不是公开找宠列表，也不是第二份资料 |
| 领养资料 | AdoptionProfile，年龄/住房/工作等，供提交送养申请使用；与找宠资料分开，不互相覆盖 |

不新增联系申请、主动推荐审批、我发出的推荐、V3专用contact、账号城市登记、联系方式公开开关、version/round/confirmed或新找宠工作流。浏览/找宠意愿不自动提交领养申请；推荐不授予新的送养联系权限。

## 2. 首页“找宠”Tab

### 2.1 顶部入口

复用当前 HomeLayoutFragment 内的 FindPetFragment，不新增底部Tab或另一套首页。顶部主题色圆角卡片参考 iOS FindPetHeaderView：主标题“找宠小助手”，副标题“发布领养意愿，让合适的宠物和送养人找到你”，右侧浅色胶囊按钮“去找宠”和箭头，右部保留icon_find_line装饰。

iOS当前参考尺寸：卡片左右15pt、内高92pt、圆角6pt；左侧文字内边距20pt，主副标题间距4pt，副标题12pt可多行；右侧按钮98×36pt、圆角18pt、字号16pt、右内边距20pt。Android用dp/sp和约束布局对齐，不能沿用70dp高度导致新增副标题被裁切；320dp窄屏及大字体允许增加高度，文字不与按钮重叠。

整张卡片与“去找宠”按钮执行同一动作：登录检查 → 打开同一个FindPetDetailActivity。当前Android只绑定backImage，findButton可能覆盖点击区域，需显式共用监听；避免一次点击导航两次。未登录可浏览公开列表，点击管理/需要身份的操作才登录；取消登录不提交任何资料。

列表为空、首屏失败也要保留或提供可达的“去找宠”入口，不能用整个contentFrame隐藏把唯一入口一起遮掉。发布/修改或重新开启成功返回刷新第一页；关闭成功后移除本人公开项或重查首屏，推荐列表/相关角标另行刷新。只浏览/取消不伪造成功。

### 2.2 公开卡片与操作

按服务端顺序展示头像、昵称、更新时间、地区、“想领养 猫咪/狗狗”标签、描述及联系/点赞/收藏/评论区域；长描述可展开/收起，不新增照片上传字段。列表数据是找宠意愿，不将送养帖的图片/推广标签套入找宠卡片。时间按设备时区格式化，空头像/昵称/地址用现有占位，复用时重置展开状态、图标与按钮状态。

- **获取联系方式**：登录后走原getcontact，topic_id为findId、topic_type固定2。有有效getedcontact和非空contact_info时真实写入系统剪贴板，再提示“已复制”；不能只弹复制成功。未经获取不得通过原始contact字段绕过校验。关闭/不可用后不再展示/复制缓存联系，重新获取由服务端校验。
- **点赞/取消点赞**：登录，like_mark=1/0、find_id；成功后按响应状态更新liked和likeNum，失败保留原值，连点防重。
- **收藏/取消收藏**：登录，collect_mark=1/0、find_id；成功更新collection及collectionNum，失败保留。本人找宠收藏页继续原接口，不调用V3推荐。
- **评论**：复用原评论页，找宠评论topic_type=3、topic_id=findId；回复沿用现有通路。成功后刷新commNum，不能误用取联系的topic_type=2；非法ID停止，不默认帖子1。
- **更多**：保留现有屏蔽/拉黑与投诉举报；投诉report_type=7、report_id=findId。屏蔽命名空间使用找宠列表的find_pet_list，不能污染首页送养屏蔽集合；不屏蔽本人信息。投诉请求沿用原举报模块，不新增V3屏蔽契约。

头像点击iOS当前无额外业务动作；Android可保留已有有效个人主页导航，但不能把头像点击变成推荐、联系申请或申请领养。

当前Android的likeActionClick/collectionClick/commentClick为空实现，需补齐；getContactClick目前复制分支仅提示且依赖原始contact判断，也需按上面规则修正。不要把这些尚未实现的回调写成已支持。

### 2.3 原生广告、列表与失败态

保留现有NativeFeedAdapter、FeedAdPlacement.FIND_PET及真实广告行。iOS使用统一ListNativeAdCell/MessagePageNativeAds（业务数据插入索引3），Android继续项目现有广告策略，不新增推广帖排序、不调用promotion_feed。页码/去重/数据数量只按业务findId，广告不参与业务ID、点击、空列表判断；生命周期销毁释放广告，回调不覆盖新页面。

公开列表沿用旧page/size分页，刷新第一页、追加成功才提交页码；data=[]或旧data={}可作为成功空数据，不能将异常/非200业务码也转为空数据。按findId去重；同账号刷新失败保留已有内容和成功页码，下一页失败仍能重试该页。公开列表旧接口没有V3 has_more，不强行按V3分页DTO解析。

列表状态复用Android现有SmartRefreshLayout/MaterialHeader/ClassicsFooter、ProgressBar与项目空态。正常空数据显示“暂无数据 / 请点击重试”，首屏失败显示“网络加载失败 / 请检查网络后重试”，点击请求第一页；首次无数据时转圈，下拉已转动不重复显示。已有内容刷新/追加失败保留内容，不用阻断弹窗，不在列表顶部另加“重新加载”按钮。参考iOS AGENTS.md的现有列表约定，Android用本地等价控件实现，不引入UIKit/DZN。

## 3. 找宠资料入口与页面

### 3.1 统一入口和鉴权

以下入口复用FindPetDetailActivity及同一个PetRequestViewModel/Repository：

1. 首页找宠卡片“去找宠”。
2. 消息“我的找宠”推荐页顶部，根据no_request/open/closed显示“发布找宠信息 / 管理找宠信息 / 开启找宠信息”。开启仍经本人资料管理及原确认操作，不凭推荐state直接写数据。
3. **设置增加“找宠资料”入口**，放在领养资料附近、复用找宠图标，category建议find_pet_profile。此项是本次Android明确要求补齐的可发现入口：当前核对的iOS设置仅有领养资料，Android设置也尚无找宠资料，不能误写为两端已实现。入口名称不改变后台数据模型，也不复制一份领养资料表单。

保留现有个人页/找宠本人信息入口时也指向同一页面，不新增重复管理页。设置、公开列表、推荐页中的路由使用稳定category/action，不因新增一行造成改密码/反馈/账号安全/退出登录位置错位。

资料页需登录。登录返回只加载本人数据，不自动发布；取消返回原入口，失效401按当前账号处理。进入调用旧searchpet/info，业务code=200回填，**207才是原“无本人找宠信息”**，显示初次表单；其他code/网络失败显示错误和重试，不能变成无资料。首屏加载失败/未完成前禁止保存和开启/关闭，不能用空表覆盖服务端已有数据。

页面使用“找宠小助手”作为与iOS一致的导航标题，设置入口叫“找宠资料”；Android当前“我的找宠信息”标题可统一调整，文档中三者均指同一找宠表单，不指领养资料或消息推荐页。

### 3.2 表单字段和视觉

顶部复用小助手提示区和猫咪头像，说明：

> 没有找到想要的宠物？可以提交相关信息给小助手，有合适的宠物后会通知您~

已有关闭信息显示关闭标记和“找宠已关闭”，无资料为发布状态，已有资料回填。输入区依次为：

| 字段 | 参数和校验 |
| --- | --- |
| 类别 | pet_type=1猫咪、2狗狗，默认猫咪；沿用单选样式与icon_pet_select/unselect，不增加任意枚举 |
| 描述 | desc，必填，trim后非空，提示“请输入简短的描述”；未定义新的后端长度上限，不自行套用领养statement的500字或经验200字限制 |
| 地址 | address，必填，复用省/市/区县选择，保留原“省.市.区县”格式；city_id可选，见3.4 |
| 联系方式 | contact，必填，原手机号或微信等文本，保留原校验；不因为资料填写增加手机号验证码或新审批门槛 |
| 提交 | 首次“发布”，已有“修改/保存”，沿用项目主题按钮，加载/写入时禁用；同一账号已有信息由原create接口更新，不重复建独立需求 |

联系字段下固定显示：

> 发布后，其他用户可通过平台获取你留下的联系方式。不希望继续被联系，可以关闭找宠信息。

**此处联系是找宠人留给他人的联系，不能复制送养发布页“仅同意沟通后展示”的文案。**公开卡片的获取仍由旧接口授权，资料页不能把私人contact当成公开展示字段。页面返回、切换类别/地区时保留未提交输入，键盘出现可滚动到按钮，长文本不遮挡；退出/切号清空私人草稿和响应。

### 3.3 发布确认、编辑及关闭/开启

表单校验通过后先收起键盘，显示当前iOS原有自定义 **“特别提醒”**，按钮“取消”“继续发布”。取消不提交；继续发布先关闭弹窗，再调用原create，提交期间防重；修改已有找宠信息使用同一路径并保留防骗提示。

提醒正文：

> 请勿相信以任何名义(包括运费、押金、定金等)要求的提前转帐与打款的行为，不要提前转账或打款，若是红包领养，请当面给送养人。请提高警惕，以防被骗！

保留“查看案例->”打开原黑名单/案例入口，以及防骗指南链接；查看说明不提交，返回保留输入。这是已恢复的原发布提醒，**覆盖现有front_demand_v3.md“只增加说明，不弹额外确认框”的过时要求**，不再额外叠加一次系统确认，不新增confirmed请求字段。

create成功不保证data含完整对象（当前成功data={}）。只有code200才提示成功；随后刷新searchpet/info取得真实findId/effective，通知原入口及推荐/未读刷新，不用空成功data覆盖资料。不因刷新失败重复发布。当前后端create会设置effective=1，因此保存关闭中的信息也会重新开启；界面说明和返回状态须体现该原行为，不宣称编辑关闭资料一定保持关闭。

已有资料保留右上角更多菜单或Android现有明确操作按钮：

- 当前effective=1时显示“关闭找宠”，确认后提交effective=2。关闭后本人仍可管理，公开列表隐藏，停止新增推荐和后续获取找宠联系，已复制的联系无法追回；推荐历史保留。
- 关闭时显示“打开/重新开启找宠”，确认后提交effective=1。开启后恢复原列表展示，并可补齐同城推荐。
- 取消不写；失败保留原effective，不先本地变更成成功。成功重查本人信息及相关列表。没有新增独立DELETE/V3状态接口，也不引入领养workflow_status控制找宠有效性。

### 3.4 城市ID

资料保存保留原address，能可靠解析时才追加字符串city_id，使用规范的城市级六位码，不是省码/区县码。直辖市使用北京市/天津市/上海市/重庆市市级码；本地完整地址与raw/city.json对照，重名地名需省市消歧。不确定时省略，不发空值/伪造码。非法非空ID后端拒绝，不能靠同时发文本绕过。

首页定位城市、同城浏览城市、AdoptionProfile的居住地区都不能覆盖找宠资料地址。当前Android直接取选择器items[1].code，需核对直辖市和目录编码是否为规范市级码；不要把“已有代码取了code”当作已完成所有兼容验证。

## 4. 接口清单：原找宠协议

以下接口均沿用原POST及FormUrlEncoded/common参数，需鉴权的请求保留body.token；可匿名浏览的旧列表也可携带当前token以返回本人的liked/collection/getedcontact。不把它们整体迁成V3 Bearer JSON。findId是找宠信息ID，不能与送养topic_id混用。

| 动作 | 方法与完整路径 | 请求关键字段 |
| --- | --- | --- |
| 首页公开找宠列表 | POST `/api/v2/findpet/list` | page、size（当前10）、原公共参数/可选token |
| 查询本人找宠资料 | POST `/api/v2/searchpet/info` | token，200回填、207无本人信息 |
| 发布/修改本人资料 | POST `/api/v2/searchpet/create` | token、pet_type、address、desc、contact，可选city_id；身份从token取得 |
| 关闭/重新开启 | POST `/api/v2/searchpet/effective` | token、effective=2/1；不发findId冒充他人管理 |
| 获取找宠人联系 | POST `/api/v1/getcontact/` | token、topic_id=findId、topic_type=2 |
| 点赞/取消 | POST `/api/v2/find/action/like` | token、find_id、like_mark=1/0 |
| 收藏/取消 | POST `/api/v2/find/action/collect` | token、find_id、collect_mark=1/0 |
| 本人找宠收藏列表 | POST `/api/v2/find/collect/list` | token、page、size=10；现有后端路由兼容该前缀，不擅自新增V3路径 |
| 找宠评论列表 | POST `/api/v1/commentlist/` | token及原公共参数、topic_type=3、topic_id=findId、page/size |
| 找宠发表评论 | POST `/api/v1/commentaction/` | token、content、topic_id=findId、topic_type=3、to_uid等原评论参数 |
| 评论回复/更多回复 | POST `/api/v1/replycomment/`、`/api/v1/replypageinfo/` | 复用原评论模块参数，不新增找宠专用回复API |

原API_v3没有替代发布/管理接口；create按当前账号update_or_create，当前成功data可为空对象；effective成功也按旧业务包消费。无Idempotency-Key/version/round要求，不能从送养V2直接套入。

发布请求示例（Form，不是JSON）：

```text
POST /api/v2/searchpet/create
Content-Type: application/x-www-form-urlencoded

token=<当前登录token>&pet_type=1&address=上海市.上海市.浦东新区&desc=希望领养性格亲人的猫咪&contact=<本人填写的联系>&city_id=310000
```

示例文字实际通过Retrofit表单编码，不手工拼接中文和特殊字符。创建/修改成功包示例：`{"code":200,"message":"成功","data":{}}`。原公开列表data通常为数组，旧空结果也可能为{}；本人信息data为单对象，DTO分开，不把三种结果混成同一分页对象。

公开FindPetModel保留findId/create_time/update_time/pet_type/address/address_info/user_id/effective/desc/userInfo/liked/collection/likeNum/collectionNum/commNum/getedcontact/contact_info，新增可空city_id以兼容新旧部署。本人对象兼容id/findId，并允许私人的contact回填；公开列表不得直接拿contact做联系人展示。

### 4.1 原业务码处理

原接口通常HTTP200，仍检查code，不以HTTP成功代替业务成功。200成功，207仅在本人info表示无信息；201/204等参数错误提示并保留表单，202拒绝访问停止，401失效走登录。获取联系原209/210分别按旧未绑定/未验证手机号规则处理，不能因找宠资料保存或V3推荐失败随意跳验证码。现有不同账号类型的旧联系校验保持，Android不引入iOS Apple身份模块替代自身登录实现。

旧接口错误没有新版data.error_code时按旧协议处理；服务端5xx/非JSON/断网结束loading并允许重试，不假装空结果、已保存或已复制。写操作超时先重读info/effective，确认状态后由用户决定重试；同一账号请求结果校验sessionRevision，旧账号401不能登出新账号。联系/资料和token不进日志、推送、公开帖子模型或永久缓存。

## 5. 消息“我的找宠”：推荐接口与状态

消息入口顺序对齐当前iOS：**系统消息 → 我的找宠 → 我的申请 → 收到的申请 → 点赞 → 收藏 → 评论**。找宠资料与推荐列表入口分别命名，不能点击“我的找宠”直接只打开资料、丢掉推荐历史；顶部管理按钮才进入资料页。

| 新接口 | 协议 |
| --- | --- |
| GET `/api/v3/find-pet/me/recommendations/` | `Authorization: Bearer <token>`；query page=1、size=20，最大100 |
| POST `/api/v3/find-pet/recommendations/{recommendation_id}/read/` | Bearer、Content-Type:application/json、body={}，重复幂等，无额外幂等键 |

成功同时要求HTTP200/code200；列表data字段find_id/state/items/page/size/total/has_more/unread_count。state为no_request/open/closed，find_id可空；items含recommendation_id/find_id/topic_id/notification_id/read/created_at/topic_status/topic_available。可公开项另含imgs/preview_img数组、content、city_id/city_name、owner{user_id,nickname,avatar}，字段在**item顶层**，不是领养topic嵌套结构。不含contact/contact_info或联系授权。

```json
{"code":200,"message":"成功","data":{"find_id":88,"state":"open","items":[{"recommendation_id":501,"find_id":88,"topic_id":1024,"notification_id":901,"read":false,"created_at":"2026-10-19T10:00:00+08:00","topic_status":"open","topic_available":true,"imgs":["pet.jpg"],"preview_img":["pet-thumb.jpg"],"content":"猫咪找家","city_id":"310000","city_name":"上海市","owner":{"user_id":12,"nickname":"小林","avatar":""}}],"page":1,"size":20,"total":1,"has_more":false,"unread_count":1}}
```

推荐卡片复用Android现有送养预览样式，显示图片、描述摘要、城市、送养状态、推荐时间、未读。topic_status=open为正在送养，adopted/closed仍可公开时显示送养已结束；unavailable/topic_available=false只显示不可用占位及必要历史ID/时间/已读信息，清掉图片/描述/城市、辅助功能文本，避免复用泄漏。显示图片优先preview_img后imgs，沿用项目图片前缀与占位。

no_request提示发布找宠；open为空显示“暂无同城推荐，下拉刷新试试”；closed显示已关闭和开启入口，保留全部历史推荐。点击推荐先POST已读，确认返回recommendation_id匹配/read=true才更新、刷新角标及跳原HomeDetailActivity；不可用历史也能标记已读，但不打开私人内容。已读失败保留未读/内容并重试。只GET/进入页面/刷新不自动将推荐已读。

新错误码：400 INVALID_REQUEST参数错误，401 AUTH_REQUIRED登录并重查，403 ACCOUNT_RESTRICTED提示限制并停止动作，404 RESOURCE_UNAVAILABLE显示不可用，503 TEMPORARILY_UNAVAILABLE/网络异常保留数据并重试；HTTP200内code非200同样失败。不要用原207判断新推荐无资料，state=no_request才是新推荐对应状态。

推荐只按城市匹配，不按猫狗/描述筛选；同需求同帖只推荐一次。每条送养帖累计最多推荐10个不同用户，按有效找宠需求update_time/id倒序，关闭/已读不释放名额、编辑不重置；这是服务端规则，不在Android截断历史列表为10条，不承诺每次开启一定收到10条。关闭、改城市后旧推荐仍可看，推荐本身不自动申请或开放送养联系；进入原详情继续V2申请/同意沟通/联系读取。

### 5.1 通知、已读与角标

保留原POST `/api/v1/systemnotification/` 及旧未读接口（旧参数/token/分页），推荐通知附kind=find_pet_recommendation、recommendation_id、topic_id。原个人通知查询即已读语义保留；推荐查询不自动已读，两个页面的已读最终由服务端Notification一致性决定，不另建永久客户端已读表。

“我的找宠”未读是系统未读子集，**总角标=原系统未读+点赞+收藏+评论+V2两类可见申请未读**，不再加my_find_pet_unread。系统公告原本地已读规则保持，个人通知和推荐按服务端同步。可GET推荐size=1读取全量unread_count，但不能拿当前页未读条数替代全局数。

原通知/外部跳转先鉴权、POST对应推荐已读，再解析本人历史推荐的真实topic_id/可用性并进入原详情；不能只信传入topic_id。需要时按has_more查后续历史，不只查第一页。没有部署推送渠道不影响站内推荐；不声称后端已发送远程推送。账号切换清旧pending及计数，迟到结果不更新新账号。

### 5.2 同城推荐列表样式

按仓库AGENTS.md与iOS FindPetV3Lists的最近调整：分组卡片、项目浅色背景、无列表分隔线；顶部只保留找宠状态说明和资料管理按钮，**不额外加“重新加载”专用按钮**。空列表“暂无数据 / 请点击重试”，首屏失败“网络加载失败 / 请检查网络后重试”；标题/描述用现有项目内容/次要文字颜色和对应字号，空态整体向上约屏高10%，空态点击刷新第一页。

Android用已有RecyclerView、SmartRefreshLayout/现有下拉上拉组件和中号ProgressBar对齐，不直接引入BaseRefresh、DZNEmptyDataSet等iOS库。首次空数据转圈与下拉动画互斥；保留约0.2秒收尾，空页脚隐藏、可续页可重试、无更多显示no more。已有数据刷新/下一页失败保留内容和最后成功页码，不弹阻断式网络错误对话框；账号受限/登录失效单独处理。推荐按has_more，不按items不足20判结束；以recommendation_id去重，不拿topic_id替代推荐身份。

## 6. Icon与资源同步表

iOS源目录统一为 `/Users/jingjun/Desktop/Githup/LaveCat/LoveCat/Assets.xcassets/`。下列尺寸已按实际PNG核对；Android用相同语义资源名，不拷贝Contents.json，不把iOS@3x文件名直接当Android资源名。

| 资源名 | 用途 | iOS实际文件/像素 | Android要求 |
| --- | --- | --- | --- |
| icon_message_find_pet | 消息“我的找宠”专用icon，亦可用于设置找宠资料入口 | imageset内@1x 40×40、@2x 80×80、@3x 120×120 | 40dp基准，依次导入drawable-mdpi/xhdpi/xxhdpi并去掉@倍率后缀；不得继续借用icon_message_sys |
| icon_find_line | 首页小助手卡片右部装饰 | @2x 338×160、@3x 507×240 | 已有drawable-xxhdpi，核对比例和裁剪，装饰不拦截点击 |
| icon_find_right | “去找宠”右箭头 | @2x 26×26、@3x 39×39 | 已有drawable-xxhdpi，约13dp，保留原色不被主题tint覆盖 |
| icon_home_find | 原找宠导航小图标，可作紧凑入口备选 | @2x 40×40、@3x 60×60 | 约20dp；不得与40dp消息tile同一缩放规则混用，入口选一种统一风格 |
| icon_cat_header | 找宠资料页顶部猫咪提示图 | @3x 900×900 | 按布局缩放/采样；iOS显示头像约66pt，不按图片像素当dp |
| icon_find_hadclose | 资料已关闭状态标记 | @3x 200×200 | 按状态显示/隐藏，不仅依靠图片表达关闭；配可读文字 |
| icon_pet_select / icon_pet_unselect | 猫咪/狗狗类别单选 | @3x 54×54 / 54×57 | 同等可点击区域和选中态，装饰尺寸约18dp；可用原生RadioButton对齐，保留无障碍选中状态 |
| icon_detail_more / icon_more_close | 本人信息更多、开启/关闭操作 | more @2x 34×34、@3x 51×51；close @3x 96×96 | 复用现有图标，触摸区域至少项目常用44–48dp；不是关闭推荐历史的按钮 |

消息专用图标样式：蓝色背景`#5C83EB`、圆角约7.5dp（40dp基准）、白色放大镜中带爪印。生成源参考 `/Users/jingjun/Desktop/Githup/LaveCat/scripts/design/generate_find_pet_message_icon.swift`；这不是“系统消息”铃铛，也不是领养资料icon_mi_zeliao。设置“找宠资料”优先复用此专用tile并按设置行现有icon尺寸显示，不额外生成另一套含义不明图标。

点赞/收藏状态继续复用icon_zan_se/un、icon_collection_se/un；评论、更多、占位图继续项目现有资源。截图/对齐时覆盖亮暗主题、选中态、关闭后复用、窄屏和大字体；装饰图无障碍忽略，可操作入口用文字contentDescription。不为本次需求生成新AI图标。

### 6.1 已同步的Android资源名称与引用

以下10类、16份PNG已实际同步，资源根目录为 `rescue_xml/app/src/main/res/`。XML使用 `@drawable/<资源名>`，Kotlin静态引用使用 `R.drawable.<资源名>`；通过项目现有动态icon字段加载时只填写资源名，不带扩展名、目录或iOS倍率后缀。

| Android资源名 | 实际文件路径（相对res） | 用途 |
| --- | --- | --- |
| `icon_message_find_pet` | `drawable-mdpi/icon_message_find_pet.png`、`drawable-xhdpi/icon_message_find_pet.png`、`drawable-xxhdpi/icon_message_find_pet.png` | 消息“我的找宠”；设置“找宠资料”可复用 |
| `icon_home_find` | `drawable-xhdpi/icon_home_find.png`、`drawable-xxhdpi/icon_home_find.png` | 紧凑找宠入口备选 |
| `icon_find_line` | `drawable-xhdpi/icon_find_line.png`、`drawable-xxhdpi/icon_find_line.png` | 首页小助手卡片装饰 |
| `icon_find_right` | `drawable-xhdpi/icon_find_right.png`、`drawable-xxhdpi/icon_find_right.png` | 去找宠按钮右箭头 |
| `icon_find_hadclose` | `drawable-xxhdpi/icon_find_hadclose.png` | 找宠资料关闭标记 |
| `icon_cat_header` | `drawable-xxhdpi/icon_cat_header.png` | 找宠资料顶部猫咪图 |
| `icon_pet_select` | `drawable-xxhdpi/icon_pet_select.png` | 类别已选中 |
| `icon_pet_unselect` | `drawable-xxhdpi/icon_pet_unselect.png` | 类别未选中 |
| `icon_detail_more` | `drawable-xhdpi/icon_detail_more.png`、`drawable-xxhdpi/icon_detail_more.png` | 本人找宠资料更多菜单 |
| `icon_more_close` | `drawable-xxhdpi/icon_more_close.png` | 关闭/开启操作图标 |

引用示例：

```xml
android:src="@drawable/icon_message_find_pet"
```

```kotlin
imageView.setImageResource(R.drawable.icon_message_find_pet)
// MessageListModel的动态icon字段使用同名字符串：
MessageListModel("icon_message_find_pet", "我的找宠", unread = null, category = "my_find_pet")
```

`MessageViewModel.kt`的“我的找宠”已使用上述名称；首页现有 `icon_find_line` / `icon_find_right` 引用保持同名。此次已校正 `icon_find_line` 的密度：xhdpi为338×160，xxhdpi为507×240。其余资源已导入，未接入的资料页头图、关闭标记或设置入口仍按第3/7节实现，不把资源存在当作页面功能已经完成。

验证：16份资源逐字节匹配iOS原图，`:app:processDebugResources`和`:app:compileDebugKotlin`通过。

## 7. Android当前差异与文件落点

路径相对 `rescue_xml/app/src/main`，java/...前缀为`java/com/rescue/flutter_720yun/`。本节是2026-10-19工作区核对结果，不能覆盖用户已有未提交修改。

| 文件/模块 | 当前状态与需要同步的内容 |
| --- | --- |
| java/.../home/fragment/FindPetFragment.kt、res/layout/fragment_find_pet.xml | 已有公开列表和广告；补首页副标题/自适应卡片、按钮共用登录路由、结果返回刷新、空失败时入口可达；补点赞/收藏/评论/复制/更多回调 |
| java/.../home/viewmodels/FindPetViewModel.kt | 已有列表和getcontact；补原互动请求、独立操作loading、findId去重、失败保留成功页码、错误与成功空态区分、账号隔离 |
| java/.../home/adapter/FindPetListAdapter.kt、home/models/FindPetModel.kt、res/layout/find_pet_list_item.xml | 更新头像/时间/类别/展开/操作状态；按contact_info复制，不绕过联系授权；city_id可空兼容，复用重置 |
| java/.../home/activity/FindPetDetailActivity.kt、findpet/PetRequestViewModel（FindPetViewModels.kt） | 已有原资料查询/保存/关闭/开启；补特别提醒与链接、资料头部/状态图、统一标题、无资料207与加载失败隔离，保存后重查 |
| java/.../findpet/FindPetService.kt、FindPetRepository.kt、FindPetModels.kt | 已有推荐及旧info/create/effective；按上表复用，新增未接的旧互动方法时仍Form，推荐仍Bearer/JSON；补当前公开owner/city_id字段有需要时使用 |
| java/.../network/HomeService.kt、MessageService.kt及旧联系/举报模块 | 旧列表/取联系/评论已有路径可复用，点赞收藏找宠路径按第4节新增；不复用送养点赞参数或秀宠ID |
| java/.../user/viewmodels/UserSettingViewModel.kt、user/activity/UserSettingActivity.kt | 增加category=find_pet_profile和“找宠资料”，路由原FindPetDetailActivity并鉴权；保留原设置项 |
| java/.../message/viewmodels/MessageViewModel.kt、message/fragment/MessageFragment.kt | 已有我的找宠category；当前排在两类申请后，专用icon已改为icon_message_find_pet，仍需调整顺序，保留未读不重复累计 |
| java/.../findpet/MyFindPetActivity.kt、RecommendationAdapter.kt、FindPetViewModels.kt；res/layout/activity_my_find_pet.xml/find_pet_recommendation_item.xml | 2026-10-19已接推荐及管理；核对本次统一空态/重试/刷新要求，使用当前模块，不重建第二套推荐页 |
| res/drawable-*、res/values/strings.xml/find_pet_ui.xml、AndroidManifest.xml | 导入/复用资源、字符串、状态说明；复用原Activity声明，新页面确有新增才注册 |

现有`V3_IMPLEMENTATION.md`记载原表单、推荐、通知/已读及账号保护已接入并有本地模拟检查，本次不把它们重复写成待重建；公开首页互动、消息顺序、设置资料入口及特别提醒是本次需补齐或校对的具体差异。无设备和线上联调结果时，不把此前测试数算作本轮功能验收。

## 8. 验收与实现顺序

1. 首页找宠Tab保留原入口和列表，副标题完整；卡片和按钮同一路由，未登录取消不提交；空列表/失败仍能去找宠。
2. 设置“找宠资料”、首页“去找宠”、推荐页管理指向同一表单，领养资料独立；code207显示新建，网络错误不清空成新建，登录/切号旧数据不回写。
3. 资料猫/狗、描述/地址/联系必填，说明文案正确；“特别提醒”取消/查看指南均不提交，仅继续发布一次；成功data={}仍可重查真实对象，保存关闭信息体现原重新开启行为。
4. 原关闭=2、开启=1，确认/取消/失败均正确；关闭后公开列表隐藏、不再获取联系，新旧联系授权校验保持，推荐历史不丢。
5. 获取联系确用findId/topic_type=2，复制真实contact_info；评论type=3，点赞/收藏1/0和计数正确，失败保留，ID不混用；举报type=7，屏蔽不影响其他列表。
6. city_id为可靠市级码，直辖市/同市异区/重名县验证；不确定省略，错误非空值被拒绝，不用浏览城市覆盖资料。
7. 我的找宠顺序与专用图标正确；推荐GET不自动已读，POST匹配ID/read=true才更新跳转，历史不可用项清掉图片正文，关闭仍能读历史，V2联系流程不被推荐绕过。
8. 系统个人通知和推荐已读一致，推荐子数不再次叠总角标；多页历史、跨账号/延迟请求、401/403/404/503/HTTP200业务失败均有对应行为。
9. 所有相关列表首次loading、正常空、失败可点击、已有数据刷新/追加失败保留、页脚重试正确；无顶部专用重新加载按钮，推荐has_more与旧列表分页分开。
10. 图标尺寸/颜色、卡片复用、320dp/常用宽/平板、大字体、键盘与关闭态正确，原广告加载失败不影响业务列表，页面退出释放广告与请求。

建议顺序：旧接口/模型及公开互动 → 本人资料入口与确认 → 首页样式/返回刷新 → 图标和消息顺序 → 推荐列表统一状态 → 单元/流程检查、Debug构建和真机联调。后端部署情况以部署环境实际版本为准；本地契约仍标注推荐接口未部署，不能据源码/文档存在认定线上可用。本次不执行后端迁移、远端写请求或Android编码。
