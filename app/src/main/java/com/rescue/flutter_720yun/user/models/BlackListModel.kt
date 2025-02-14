package com.rescue.flutter_720yun.user.models

import android.os.Parcelable
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