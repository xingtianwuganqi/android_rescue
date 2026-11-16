package com.rescue.flutter_720yun.adoption.models

import com.google.gson.JsonObject
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.util.localized
import com.google.gson.annotations.SerializedName

data class V2Response<T>(val code: Int, val message: String?, val data: T?, val errorData: JsonObject? = null)
data class AdoptionProfile(val age: Int?, val residence_city: String?, val residence_district: String?,
    val housing_type: String?, val employment_status: String?, val pet_experience: String?, val version: Int? = null)
data class ProfileData(val profile: AdoptionProfile?, val is_complete: Boolean, val missing_fields: List<String>?, val version: Int?)
data class ProfileWrite(val age: Int, val residence_city: String, val residence_district: String,
    val housing_type: String, val employment_status: String, val pet_experience: String, val version: Int?)
data class ApplicationState(val topic_id: Int, val workflow_status: String?, @SerializedName(value="workflow_version", alternate=["version"]) val workflow_version: Int?,
    val contact_version: Int?, val can_apply: Boolean, val can_manage: Boolean,
    val application_id: Int?, val application_status: String?, val application_result: String?,
    val application_version: Int?, val contact_authorized: Boolean, val application_count: Int? = null, val unread_application_count: Int? = null)
data class ApplicationWrite(val profile_version: Int, val statement: String)
data class ApplicationAction(val action: String, val version: Int, val end_note: String? = null)
data class TopicAction(val action: String, val confirmed: Boolean, val version: Int, val application_id: Int? = null, val end_note: String? = null)
data class PublicApplicant(val user_id: Int?, val nickname: String?, val avatar: String?)
data class TopicSummary(val topic_id: Int, val preview_img: String?, val content_excerpt: String?,
    val address_info: String?, val is_complete: Boolean?, val is_delete: Int?, val unavailable: Boolean = false,
    val getedcontact: Boolean = false, val contact_info: String? = null)
data class AdoptionApplication(val application_id: Int, val status: String?, val result: String?, val version: Int,
    val created_at: String?, val applicant: PublicApplicant?, val profile_snapshot: AdoptionProfile?,
    val profile_version: Int?, val statement: String?, val end_note: String?, val profile_label: String?,
    val topic: TopicSummary?, val contact_authorized: Boolean = false, val end_reason: String? = null)
data class V2Page<T>(val items: List<T>?, val page: Int, val size: Int, val total: Int, val has_more: Boolean, val unread_count: Int? = null,
    val mine_unread_count: Int? = null, val received_unread_count: Int? = null)
data class AdoptionNotification(val id: Int, val message: String?, val topic_id: Int?, val application_id: Int?,
    val target: String?, val read: Boolean, val created_at: String?,
    val kind: String? = null, val role: String? = null)
data class UnreadCount(val unread_count: Int, val mine_unread_count: Int? = null, val received_unread_count: Int? = null)

object AdoptionLabels {
    val housing: Map<String, String> get() = linkedMapOf("owned" to localized(R.string.housing_owned,"自有住房"), "whole_rent" to localized(R.string.housing_whole_rent,"整租"), "shared_rent" to localized(R.string.housing_shared_rent,"合租"),
        "with_family" to localized(R.string.housing_with_family,"与家人同住"), "dormitory" to localized(R.string.housing_dormitory,"宿舍"), "other" to localized(R.string.housing_other,"其他"))
    val employment: Map<String, String> get() = linkedMapOf("employed" to localized(R.string.employment_employed,"在职"), "self_employed" to localized(R.string.employment_self_employed,"自由职业/个体经营"),
        "student" to localized(R.string.employment_student,"学生"), "not_working" to localized(R.string.employment_not_working,"暂未工作"), "retired" to localized(R.string.employment_retired,"退休"))
    fun status(status: String?, result: String? = null, endReason: String? = null, endNote: String? = null): String = when(status) {
        "applying" -> localized(R.string.adoption_filter_applying,"申请中"); "communicating" -> localized(R.string.adoption_filter_communicating,"沟通中")
        "ended" -> if(result == "not_completed" && endReason == "owner_ended" && endNote == "拒绝沟通申请") localized(R.string.adoption_ended_rejected,"已结束 · 被拒绝") else localized(if(result == "completed") R.string.adoption_ended_completed else R.string.adoption_ended_not_completed, if(result == "completed") "已结束 · 完成送养" else "已结束 · 未完成送养")
        else -> localized(R.string.adoption_no_applications,"暂无申请")
    }
    fun summary(p: AdoptionProfile?): String = if(p == null) localized(R.string.adoption_profile_unavailable,"资料已不可用") else {
        val age=p.age?.toString() ?: localized(R.string.adoption_age_unfilled,"未填写")
        val housingLabel=housing[p.housing_type] ?: localized(R.string.adoption_housing_unfilled,"未填写住房")
        val employmentLabel=employment[p.employment_status] ?: localized(R.string.adoption_employment_unfilled,"未填写工作")
        localized(R.string.adoption_summary_format,"${age}岁 · ${p.residence_city.orEmpty()} ${p.residence_district.orEmpty()}\n$housingLabel · $employmentLabel",age,p.residence_city.orEmpty(),p.residence_district.orEmpty(),housingLabel,employmentLabel)
    }
}
