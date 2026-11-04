package com.rescue.flutter_720yun.ads

import android.app.Activity
import android.view.ViewGroup

class BannerAdController(activity: Activity, private val container: ViewGroup) {
    fun load() = Unit
    fun destroy() { container.removeAllViews() }
}
