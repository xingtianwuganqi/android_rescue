package com.rescue.flutter_720yun.promotion

import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.promotion.models.*
import com.rescue.flutter_720yun.util.localized
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PromotionDisplay {
    fun time(value: String?)=runCatching { OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull() ?: localized(R.string.promotion_status_pending,"待刷新")
    fun fullTime(value: String?)=runCatching { OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) }.getOrNull() ?: localized(R.string.promotion_status_pending,"待刷新")
    fun confirmation(state: PromotionState?, fallbackSeconds: Int = 3600): String {
        if(state==null) return localized(R.string.promotion_confirmation_unknown,"推广请求已处理，当前状态暂时无法确认，请刷新查看")
        val promotion=state.promotion
        if(promotion?.is_active!=true) return localized(R.string.promotion_confirmation_ended,"本次推广已处理，当前推广已结束")
        val until=fullTime(promotion.effective_until)
        if(until==localized(R.string.promotion_status_pending,"待刷新")) return localized(R.string.promotion_confirmation_refresh,"推广请求已处理，请刷新查看当前状态")
        val duration=PromotionUiPolicy.duration(state.duration_seconds.takeIf { it>0 } ?: fallbackSeconds)
        return localized(R.string.promotion_confirmation_active,"推广已生效，每次推广时长${duration}，当前有效期至$until",duration,until)
    }
    fun label(promotion: PromotionSummary?)=when {
        promotion?.is_active==true -> localized(R.string.promotion_label_active,"推广中 · 截至${time(promotion.effective_until)}",time(promotion.effective_until))
        promotion?.promotion_id!=null -> localized(R.string.promotion_label_ended,"推广已结束")
        else -> ""
    }
}
