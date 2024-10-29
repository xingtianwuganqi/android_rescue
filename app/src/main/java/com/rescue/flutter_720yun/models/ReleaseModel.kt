package com.rescue.flutter_720yun.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class CoachReleasePhoto(
    var isAdd: Boolean,
    val photoKey: String?,
): Parcelable

// 缓存的数据结构
@Parcelize
data class CoachReleaseInfo(
    val tags: MutableList<TagInfoModel>,
    var content: String?,
    var photos: MutableList<CoachReleasePhoto>,
    var contact: String?,
    var address: String?,
): Parcelable
