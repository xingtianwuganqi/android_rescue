package com.rescue.flutter_720yun.promotion.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PromotionSummary(val promotion_id: Int? = null, val is_active: Boolean = false,
    val create_time: String? = null, val update_time: String? = null, val effective_until: String? = null,
    val can_promote: Boolean = false, val reason: String? = null, val duration_seconds: Int? = null,
    val remaining_today: Int? = null, val server_time: String? = null,
    val is_promoted: Boolean = false, val label: String? = null): Parcelable

data class PromotionState(val topic_id: Int, val can_promote: Boolean, val reason: String?,
    val duration_seconds: Int, val remaining_today: Int?, val promotion: PromotionSummary?,
    val server_time: String?, val promotion_version: String?)
data class PromotionWrite(val reward_attempt_id: String, val scene: String)
data class PromotionResult(val topic_id: Int, val promotion_id: Int, val is_active: Boolean,
    val create_time: String?, val update_time: String?, val effective_until: String?,
    val duration_seconds: Int, val created: Boolean, val remaining_today: Int?, val server_time: String?)

/** Only observed rewards are persisted. Never contains credentials or adoption data. */
data class PendingReward(val account_id: Int, val topic_id: Int, val scene: String,
    val reward_attempt_id: String, val reward_observed_at: Long, val retry_until: Long,
    val next_retry_at: Long = 0, val retry_state: String = "pending")
object PromotionPolicy {
    const val RECOVERY_WINDOW=24*60*60*1000L
    // Backend rate windows are 60s; allow headroom for connection/write/read timeouts.
    // Persist before POST so
    // losing the subsequent retry-state write cannot cause an immediate restart replay.
    const val UNKNOWN_RESULT_BACKOFF=120_000L
    fun recoverable(record: PendingReward, account: Int?, now: Long)=
        record.account_id==account && record.retry_until>now && record.next_retry_at<=now && record.retry_state=="pending"
    fun retryable(http: Int)=http==0 || http==429 || http>=500
    fun reason(value: String?)=when(value) {
        "DAILY_LIMIT_REACHED" -> "今日推广次数已用完"
        "TOPIC_NOT_OPEN" -> "帖子已结束送养"
        "NOT_OWNER" -> "仅送养人可推广此帖"
        "ACCOUNT_RESTRICTED" -> "当前账号无法推广"
        else -> "当前暂不能推广，请刷新后重试"
    }
}
