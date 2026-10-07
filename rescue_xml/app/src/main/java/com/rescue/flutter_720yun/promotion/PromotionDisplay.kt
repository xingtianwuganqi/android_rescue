package com.rescue.flutter_720yun.promotion

import com.rescue.flutter_720yun.promotion.models.*
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PromotionDisplay {
    fun time(value: String?)=runCatching { OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull() ?: "待刷新"
    fun label(promotion: PromotionSummary?)=when {
        promotion?.is_active==true -> "推广中 · 截至${time(promotion.effective_until)}"
        promotion?.promotion_id!=null -> "推广已结束"
        else -> ""
    }
}
