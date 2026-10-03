package com.rescue.flutter_720yun.adoption.models

import com.google.gson.JsonObject

data class V2Response<T>(val code: Int, val message: String?, val data: T?)
data class AdoptionProfile(val age: Int?, val residence_city: String?, val residence_district: String?,
    val housing_type: String?, val employment_status: String?, val pet_experience: String?, val version: Int? = null)
data class ProfileData(val profile: AdoptionProfile?, val is_complete: Boolean, val missing_fields: List<String>?, val version: Int?)
data class ProfileWrite(val age: Int, val residence_city: String, val residence_district: String,
    val housing_type: String, val employment_status: String, val pet_experience: String, val version: Int?)
data class ApplicationState(val topic_id: Int, val workflow_status: String?, val workflow_version: Int?,
    val contact_version: Int?, val can_apply: Boolean, val can_manage: Boolean,
    val application_id: Int?, val application_status: String?, val application_result: String?,
    val application_version: Int?, val contact_authorized: Boolean, val application_count: Int? = null)
data class ApplicationWrite(val profile_version: Int, val statement: String)
data class ApplicationAction(val action: String, val version: Int, val end_note: String? = null)
data class TopicAction(val action: String, val confirmed: Boolean, val version: Int, val application_id: Int? = null)
data class PublicApplicant(val user_id: Int?, val nickname: String?, val avatar: String?)
data class TopicSummary(val topic_id: Int, val preview_img: String?, val content_excerpt: String?,
    val address_info: String?, val is_complete: Boolean?, val is_delete: Int?, val unavailable: Boolean = false,
    val getedcontact: Boolean = false, val contact_info: String? = null)
data class AdoptionApplication(val application_id: Int, val status: String?, val result: String?, val version: Int,
    val created_at: String?, val applicant: PublicApplicant?, val profile_snapshot: AdoptionProfile?,
    val profile_version: Int?, val statement: String?, val end_note: String?, val profile_label: String?,
    val topic: TopicSummary?, val contact_authorized: Boolean = false)
data class V2Page<T>(val items: List<T>?, val page: Int, val size: Int, val total: Int, val has_more: Boolean, val unread_count: Int? = null,
    val mine_unread_count: Int? = null, val received_unread_count: Int? = null)
data class AdoptionNotification(val id: Int, val message: String?, val topic_id: Int?, val application_id: Int?,
    val target: String?, val read: Boolean, val created_at: String?,
    val kind: String? = null, val role: String? = null)
data class UnreadCount(val unread_count: Int, val mine_unread_count: Int? = null, val received_unread_count: Int? = null)

object AdoptionLabels {
    val housing = linkedMapOf("owned" to "自有住房", "whole_rent" to "整租", "shared_rent" to "合租",
        "with_family" to "与家人同住", "dormitory" to "宿舍", "other" to "其他")
    val employment = linkedMapOf("employed" to "在职", "self_employed" to "自由职业/个体经营",
        "student" to "学生", "not_working" to "暂未工作", "retired" to "退休")
    fun status(status: String?, result: String? = null): String = when(status) {
        "applying" -> "申请中"; "communicating" -> "沟通中"
        "ended" -> "已结束 · " + if(result == "completed") "完成送养" else "未完成送养"
        else -> "暂无申请"
    }
    fun summary(p: AdoptionProfile?): String = if(p == null) "资料已不可用" else
        "${p.age ?: "未填写"}岁 · ${p.residence_city.orEmpty()} ${p.residence_district.orEmpty()}\n" +
        "${housing[p.housing_type] ?: "未填写住房"} · ${employment[p.employment_status] ?: "未填写工作"}"
}
