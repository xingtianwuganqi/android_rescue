package com.rescue.flutter_720yun.models

data class BaseResponse<T> (
    val code: Int,
    var data: T,
    val message: String
)

data class BaseListResp<T> (
    val code: Int?,
    val data: List<T>?,
    val message: String?
)

data class HomeResponse (
    val code: Int?,
    val data: List<HomeListModel>?,
    val message: String?
)

data class UserInfo(
    val id: Int?,
    val avator: String?,
    val username: String?,
    val phone_number: String?,
    val email: String?,
    val create_time: String?,
    val wx_id: String?
)

data class TagInfoModel(
    val id: Int?,
    val tag_name: String?,
    val tag_type: Int?
)

data class HomeListModel (
    val id: Int?,
    val content: String?,
    val create_time: String?,
    val upload_time: String?,
    val userInfo: UserInfo?,
    val gambit_type: Int?,
    val liked: Boolean?,
    val collectioned: Boolean?,
    val tags: List<Int>?,
    val is_complete: Boolean?,
    val preview_img: List<String>?,
    val imgs: List<String>?,
    val getedcontact: Boolean?,
    val user: Int?,
    val likes_num: Int?,
    val address_info: String?,
    val contact_info: String?,
    val tagInfos: List<TagInfoModel>?,
    val topic_id: Int?,
    val commNum: Int?,
    val views_num: Int?,
    val collection_num: Int?,
)