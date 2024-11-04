package com.rescue.flutter_720yun.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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

@Parcelize
data class UserInfo(
    val id: Int?,
    val avator: String?,
    val username: String?,
    val phone_number: String?,
    val email: String?,
    val create_time: String?,
    val wx_id: String?,
    val token: String?
): Parcelable

@Parcelize
data class TagInfoModel(
    val id: Int?,
    val tag_name: String?,
    val tag_type: Int?,
    var isSelected: Boolean = false
): Parcelable

@Parcelize
data class HomeListModel (
    val content: String?,
    val create_time: String?,
    val upload_time: String?,
    val userInfo: UserInfo?,
    val gambit_type: Int?,
    var liked: Boolean?,
    var collectioned: Boolean?,
    val tags: List<Int>?,
    val is_complete: Boolean?,
    val preview_img: List<String>?,
    val imgs: List<String>?,
    var getedcontact: Boolean?,
    val user: Int?,
    val likes_num: Int?,
    val address_info: String?,
    var contact_info: String?,
    val tagInfos: List<TagInfoModel>?,
    val topic_id: Int?,
    val commNum: Int?,
    val views_num: Int?,
    val collection_num: Int?,
): Parcelable


@Parcelize
data class LikeActionModel(
    val mark: Int,
    val like: Int
): Parcelable

@Parcelize
data class CollectionActionModel(
    val mark: Int,
    val collect: Int,
): Parcelable

@Parcelize
data class ContactInfoModel(
    val contact: String,
): Parcelable


@Parcelize
data class SearchKeywordModel(
    val id: Int,
    val keyword: String,
): Parcelable

@Parcelize
data class SearchHistoryItemModel(
    val title: String,
    val list: List<SearchKeywordModel>?
): Parcelable


@Parcelize
data class DrawerListModel(
    val icon: String,
    val name: String,
    val number: Int?
): Parcelable