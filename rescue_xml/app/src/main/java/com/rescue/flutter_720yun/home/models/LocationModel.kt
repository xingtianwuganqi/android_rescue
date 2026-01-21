package com.rescue.flutter_720yun.home.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CityModel(
    val code: Int,
    val name: String,
): Parcelable


@Parcelize
data class AddressItem(
    val code: String,
    val name: String,
    val children: List<AddressItem>?
): Parcelable