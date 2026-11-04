package com.rescue.flutter_720yun.findpet

import com.google.gson.annotations.SerializedName

/** Public recommendation fields only; no contact or authorization data is consumed here. */
data class PetRecommendation(
    val recommendation_id: Int, val find_id: Int?, val topic_id: Int, val message_id: Int?,
    val read: Boolean, val created_at: String?, val topic_status: String?, val topic_available: Boolean,
    val imgs: List<String>?, val preview_img: List<String>?, val content: String?, val city_name: String?
) {
    val available get() = topic_available && topic_status in listOf("open", "adopted", "closed") && topic_id > 0
    val image get() = if (available) (preview_img.orEmpty() + imgs.orEmpty()).firstOrNull { it.isNotBlank() } else null
    val statusLabel get() = if(!available) "帖子暂不可用" else when (topic_status) {
        "open" -> "送养中"; "adopted" -> "送养已结束 · 已完成送养"
        "closed" -> "送养已结束 · 已关闭送养"; else -> "帖子暂不可用"
    }
}
data class PetRecommendations(
    val find_id: Int?, val state: String?, val items: List<PetRecommendation>?, val page: Int?,
    val size: Int?, val total: Int?, val has_more: Boolean?, val unread_count: Int?
)
data class RecommendationRead(val recommendation_id: Int, val read: Boolean)
data class MyPetRequest(
    @SerializedName(value = "id", alternate = ["findId"]) val id: Int,
    val pet_type: Int?, val address: String?, val desc: String?, val contact: String?,
    val effective: Int?, val is_delete: Int?, val city_id: String?
)
