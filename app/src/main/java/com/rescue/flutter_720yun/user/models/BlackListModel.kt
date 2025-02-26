package com.rescue.flutter_720yun.user.models

import android.os.Parcelable
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import kotlinx.parcelize.Parcelize


@Parcelize
data class BlackListModel(
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
    var isMust: Boolean
): Parcelable

/*
struct ReleaseReportInfo {
    var phone: String?
    var wx_num: String?
    var name: String?
    var black_type: Int = 1
    var desc: String?
    var photos: [ReleasePhotoModel] = []
}
 */
@Parcelize
data class ReleaseReportInfoModel(
    val phone: String?,
    val wx_num: String?,
    val name: String?,
    val black_type: String?,
    val desc: String?,
    val photos: String
): Parcelable