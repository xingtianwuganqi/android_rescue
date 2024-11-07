package com.rescue.flutter_720yun.home.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class ProvinceModel(
    val code: Int,
    val name: String,
    val children: List<CityModel>
): Parcelable

@Parcelize
data class CityModel(
    val code: Int,
    val name: Int,
    val children: List<AreaModel>
): Parcelable
@Parcelize
data class AreaModel(
    val code: Int,
    val name: String,
): Parcelable


@Parcelize
data class AddressItem(
    val code: String,
    val name: String,
    val children: List<AddressItem>?
): Parcelable