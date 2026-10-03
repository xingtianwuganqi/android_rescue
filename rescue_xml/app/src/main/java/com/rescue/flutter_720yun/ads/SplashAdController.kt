package com.rescue.flutter_720yun.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import com.anythink.core.api.ATAdInfo
import com.anythink.core.api.AdError
import com.anythink.splashad.api.ATSplashAd
import com.anythink.splashad.api.ATSplashAdExtraInfo
import com.anythink.splashad.api.ATSplashAdListener
import com.rescue.flutter_720yun.BuildConfig

class SplashAdController(
    private val activity: Activity,
    private val container: ViewGroup,
    private val onComplete: () -> Unit,
    private val loadTimeoutMs: Int = 5_000
) {
    private val handler = Handler(Looper.getMainLooper())
    private var ad: ATSplashAd? = null
    private var completed = false
    private var showing = false
    private var loaded = false
    private var foreground = false
    private val loadTimeout = Runnable { complete() }
    // Also covers SDKs that never call dismiss after show; suspended when opening a landing page.
    private val showTimeout = Runnable { complete() }

    fun load() {
        if (!TakuAds.initialize(activity) || BuildConfig.TAKU_SPLASH_ID.isBlank()) {
            complete()
            return
        }
        handler.postDelayed(loadTimeout, loadTimeoutMs.toLong())
        ad = ATSplashAd(activity, BuildConfig.TAKU_SPLASH_ID, object : ATSplashAdListener {
            override fun onAdLoaded(isTimeout: Boolean) = dispatch {
                if (!isTimeout) {
                    loaded = true
                    tryShow()
                } else complete()
            }
            override fun onAdLoadTimeout() = dispatch { complete() }
            override fun onNoAdError(error: AdError?) = dispatch {
                TakuAds.logError("splash", error)
                complete()
            }
            override fun onAdShow(info: ATAdInfo?) = Unit
            override fun onAdClick(info: ATAdInfo?) = Unit
            override fun onAdDismiss(info: ATAdInfo?, extra: ATSplashAdExtraInfo?) = dispatch { complete() }
        }, loadTimeoutMs)
        ad?.loadAd()
    }

    fun onResume() {
        foreground = true
        if (completed) deliverCompletion() else {
            tryShow()
            if (showing) handler.postDelayed(showTimeout, 15_000)
        }
    }

    fun onPause() {
        foreground = false
        handler.removeCallbacks(showTimeout)
    }

    private fun tryShow() {
        if (completed || showing || !loaded || !foreground || activity.isFinishing || activity.isDestroyed) return
        if (ad?.isAdReady == true) {
            showing = true
            handler.removeCallbacks(loadTimeout)
            handler.postDelayed(showTimeout, 15_000)
            ad?.show(activity, container)
        } else complete()
    }

    private fun dispatch(action: () -> Unit) {
        handler.post { if (!completed && !activity.isDestroyed) action() }
    }

    private fun complete() {
        if (completed) return
        completed = true
        handler.removeCallbacksAndMessages(null)
        deliverCompletion()
    }

    private fun deliverCompletion() {
        if (foreground && !activity.isFinishing && !activity.isDestroyed) onComplete()
    }

    fun destroy() {
        completed = true
        handler.removeCallbacksAndMessages(null)
        ad?.setAdListener(null)
        ad?.onDestory()
        ad = null
        container.removeAllViews()
    }
}
