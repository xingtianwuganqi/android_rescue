package com.rescue.flutter_720yun.user.models

import android.os.Parcelable
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import kotlinx.parcelize.Parcelize


@Parcelize
data class BlackListModel(
    var id: Int?,
    var name: String?,
    var contact: String?,
    var desc: String?,
    var wx_num: String?,
    var black_status: String?,
    var from_userId: String?,
    //1: 领养人，2：送养人
    var black_type: Int?
): Parcelable


@Parcelize
data class BlackDetailModel(
    val title: String?,
    val placeholder: String?,
    var desc: String?,
    var photos: MutableList<CoachReleasePhoto>?,
    var isMust: Boolean?,
    var order: Int?,
): Parcelable

@Parcelize
data class ReleaseReportInfoModel(
    val phone: String?,
    val wx_num: String?,
    val name: String?,
    val black_type: String?,
    val desc: String?,
    val photos: String
): Parcelable


//struct BlackDetailModel: HandyJSON {
//    var id: Int?
//    var name: String?
//    var contact: String?
//    var desc: String?
//    var wx_num: String?
//    var images: [String]?
//    var black_status: Int?
//    var from_userId: Int?
//
//    //1: 领养人，2：送养人
//    var black_type: Int?
//}

@Parcelize
data class BlackDetailItemModel(
    val id: Int?,
    val name: String?,
    val contact: String?,
    val desc: String?,
    val wx_num: String?,
    val images: List<String>?,
    val black_status: Int?,
    val from_userId: Int?,
    val black_type: Int?
): Parcelable