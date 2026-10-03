package com.rescue.flutter_720yun.ads

import android.app.Application
import android.content.Context
import android.util.Log
import com.anythink.core.api.ATSDK
import com.anythink.core.api.AdError
import com.rescue.flutter_720yun.BuildConfig
import com.rescue.flutter_720yun.util.SharedPreferencesUtil

object TakuAds {
    private var initialized = false
    internal var fullscreenPresented = false

    /** Call from a foreground Activity after privacy consent, including first-run acceptance. */
    fun initialize(context: Context): Boolean {
        if (SharedPreferencesUtil.getString("firstOpen", context) != "1" ||
            Application.getProcessName() != context.packageName ||
            BuildConfig.TAKU_APP_ID.isBlank() || BuildConfig.TAKU_APP_KEY.isBlank()) return false
        if (initialized) return true
        return try {
            ATSDK.setNetworkLogDebug(BuildConfig.DEBUG)
            ATSDK.init(context.applicationContext, BuildConfig.TAKU_APP_ID, BuildConfig.TAKU_APP_KEY)
            ATSDK.start()
            initialized = true
            true
        } catch (error: Exception) {
            Log.e("TakuAds", "SDK initialization failed", error)
            false
        }
    }

    fun logError(format: String, error: AdError?) {
        Log.w("TakuAds", "$format: ${error?.fullErrorInfo ?: "unknown error"}")
    }
}
