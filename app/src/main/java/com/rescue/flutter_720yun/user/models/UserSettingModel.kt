package com.rescue.flutter_720yun.user.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class UserSettingModel(
    val icon: String?,
    val title: String?,
): Parcelable