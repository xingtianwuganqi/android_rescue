package com.rescue.flutter_720yun.models

import android.os.Parcelable
import com.luck.picture.lib.entity.LocalMedia
import kotlinx.parcelize.Parcelize


@Parcelize
data class CoachReleasePhoto(
    var isAdd: Boolean,
    val photoKey: String?,
    val previewKey: String?,
    var media: LocalMedia?
): Parcelable

// 缓存的数据结构
@Parcelize
data class CoachReleaseInfo(
    var tags: List<TagInfoModel>,
    var content: String?,
    var photos: MutableList<CoachReleasePhoto>,
    var contact: String?,
    var address: String?,
): Parcelable
