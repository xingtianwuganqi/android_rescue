package com.rescue.flutter_720yun.adoption.models

import com.rescue.flutter_720yun.home.models.AddressItem

object AdoptionRegions {
    /** The backend accepts 全市 only for catalogue cities with no district entries. */
    fun districts(city: AddressItem): List<AddressItem> = city.children?.takeIf { it.isNotEmpty() }
        ?: listOf(AddressItem(city.code, "全市", null))
}
