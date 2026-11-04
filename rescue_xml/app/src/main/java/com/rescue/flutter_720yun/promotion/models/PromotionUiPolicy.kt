package com.rescue.flutter_720yun.promotion.models

object PromotionUiPolicy {
    fun ended(complete: Boolean, workflow: String?) = complete || workflow in listOf("adopted","closed")
    fun enabled(ads: Boolean, own: Boolean, complete: Boolean, workflow: String?, summary: PromotionSummary?, busy: Boolean) =
        ads && own && !ended(complete,workflow) && summary?.can_promote != false && !busy
    fun title(complete: Boolean, workflow: String?, summary: PromotionSummary?) = when {
        workflow == "adopted" -> "完成领养"
        workflow == "closed" -> "结束领养"
        complete -> "领养已结束"
        summary?.can_promote == false -> when(summary.reason) {
            "FEATURE_DISABLED" -> "推广暂未开放"
            "DAILY_LIMIT_REACHED" -> "今日次数已用完"
            else -> "暂不可推广"
        }
        summary?.is_active == true -> "观看视频，延长推广"
        else -> "观看视频，增加曝光"
    }
    fun duration(seconds: Int): String {
        val value = seconds.takeIf { it > 0 } ?: 3600
        return listOf(value / 3600 to "小时", value % 3600 / 60 to "分钟", value % 60 to "秒")
            .filter { it.first > 0 }.joinToString("") { "${it.first}${it.second}" }
    }
    fun pending(record: PendingReward, account: Int, topic: Int, now: Long) =
        record.account_id == account && record.topic_id == topic && record.retry_state == "pending" &&
            record.reward_observed_at <= now && record.retry_until > now
    fun acceptsLoaded(phase: String, active: Boolean, sameAccount: Boolean) = phase == "preparing" && active && sameAccount
}
