package com.rescue.flutter_720yun.home.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportTypeModel(
    var id: Int?,
    var create_time: String?,
    var vio_name: String?,
    @Transient
    var selected: Boolean = false
): Parcelable