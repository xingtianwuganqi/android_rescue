# V2 领养流程修复记录

2026-10-05：测试报告中的 5 个问题已完成本地修复。

| 问题 | 修复后的行为 |
| --- | --- |
| 无区县选项的城市不能填写资料 | Android 为这些城市提供“全市”；后端只对地区目录没有区县的城市接受该值，其他城市继续严格验证区县 |
| 终态幂等重放误报新申请成功 | 校验返回申请 ID、版本、帖子、申请人和状态；已结束的重放显示真实结果，下一次用户确认生成新 key，不自动重新申请 |
| 写成功、刷新失败仍可申请 | 先使用成功响应更新申请 ID/状态并关闭 can_apply；刷新失败保留已提交状态，明确提示“申请已提交，状态刷新失败” |
| 筛选失败混用旧数据/分页 | 按身份、帖子、筛选跟踪数据范围；范围变化清空旧项和分页，从第一页加载；同范围刷新失败仍保留成功数据 |
| 申请资料面板失败后空白 | 显示加载状态、错误原因和“重新加载”按钮；点击可直接重试，不必关闭面板 |

## 验证

- Android：63 项测试全部通过，其中原有 44 项、新增 QA 流程测试 19 项；`:app:assembleDebug` 成功。
- 新增验证包括：终态重放后显式新确认使用新 key、成功后刷新失败保留申请状态、切换筛选失败后追加从第一页开始、同筛选失败保留旧数据、无效申请响应不报成功、真实沟通状态提示及资料面板重试。
- 本地后端：`apps.adoption_v2.tests` 57 项全部通过。新增测试对 4 个城市逐一保存资料、提交申请、验证快照及结束申请，并验证“全市”不能用于上海或不存在的城市，也不能混用其他区县。
- 后端测试使用 `apps.adoption_v2.test_settings` 和内存 SQLite，禁用 MySQL 环境变量；未使用远端账号或生产数据库。
- Android/后端 `git diff --check` 通过。构建仍有已有广告 SDK 和 Kotlin 警告。

测试统计见 [results.json](results.json)。修复前证据保留在 [results-before-fix.json](results-before-fix.json)。

## 配套后端修改和发布要求

本次同步修改相邻本地仓库：

- [地区校验](/Users/jingjun/Desktop/Githup/rest_framework/apps/adoption_v2/services/profile.py)
- [API 回归测试](/Users/jingjun/Desktop/Githup/rest_framework/apps/adoption_v2/tests.py)

“全市”需先发布配套后端修复，再发布 Android 更新；只有 Android 修改时，旧后端仍会拒绝这 4 个城市的资料。该修改无需数据库迁移。

未执行后端部署、提交或推送。真机点击、旋转、前后台、双账号联动和系统通知验收仍需要测试设备与账号。

## 重跑

Android（`rescue_xml`）：

```sh
./gradlew -I qa/adoption/qa.init.gradle :app:testDebugUnitTest :app:assembleDebug --offline --console=plain
```

后端（`rest_framework`，使用已有隔离测试环境）：

```sh
env -u V2_TEST_MYSQL_DATABASE PYTHONDONTWRITEBYTECODE=1 DJANGO_SETTINGS_MODULE=apps.adoption_v2.test_settings /private/tmp/adoption-v2-env/bin/python -m django test apps.adoption_v2.tests --noinput -v 1
```
