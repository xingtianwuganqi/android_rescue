package com.rescue.flutter_720yun

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.webkit.WebView

class BaseApplication: Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        val processName = getProcessName()
        if (processName != packageName) {
            WebView.setDataDirectorySuffix(processName)
        }
        context = applicationContext
        if (processName == packageName) {
            com.rescue.flutter_720yun.promotion.RewardedTopicPromotionCoordinator.initialize()
            registerActivityLifecycleCallbacks(com.rescue.flutter_720yun.ads.ForegroundSplashObserver(this))
        }
    }
}
