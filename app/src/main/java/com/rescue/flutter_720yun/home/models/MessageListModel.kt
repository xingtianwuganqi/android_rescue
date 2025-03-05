package com.rescue.flutter_720yun.home.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageListModel(
    val icon: String,
    val title: String,
    var unread: Int? = 0
): Parcelable