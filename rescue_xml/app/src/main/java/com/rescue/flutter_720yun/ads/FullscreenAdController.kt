package com.rescue.flutter_720yun.ads

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.anythink.core.api.ATAdInfo
import com.anythink.core.api.AdError
import com.anythink.interstitial.api.ATInterstitial
import com.anythink.interstitial.api.ATInterstitialListener
import com.anythink.rewardvideo.api.ATRewardVideoAd
import com.anythink.rewardvideo.api.ATRewardVideoListener
import com.rescue.flutter_720yun.BuildConfig

/** Owned by one Activity. Loading never automatically shows a fullscreen ad. */
class FullscreenAdController(
    private val activity: Activity,
    private val owner: LifecycleOwner,
    private val onEvent: (String) -> Unit = {},
    private val onReward: (ATAdInfo) -> Unit = {}
) : DefaultLifecycleObserver {
    private var interstitial: ATInterstitial? = null
    private var rewarded: ATRewardVideoAd? = null
    private var destroyed = false
    private var rewardDelivered = false
    private var rewardShowing = false
    private var interstitialShowing = false

    init { owner.lifecycle.addObserver(this) }

    fun loadInterstitial(): Boolean {
        if (destroyed || interstitialShowing || BuildConfig.TAKU_INTERSTITIAL_ID.isBlank() || !TakuAds.initialize(activity)) return false
        if (interstitial == null) {
            interstitial = ATInterstitial(activity, BuildConfig.TAKU_INTERSTITIAL_ID).apply {
                setAdListener(object : ATInterstitialListener {
                    override fun onInterstitialAdLoaded() = event("interstitial loaded")
                    override fun onInterstitialAdLoadFail(error: AdError?) = failure("interstitial load", error)
                    override fun onInterstitialAdClicked(info: ATAdInfo?) = Unit
                    override fun onInterstitialAdShow(info: ATAdInfo?) = event("interstitial shown")
                    override fun onInterstitialAdClose(info: ATAdInfo?) = update {
                        interstitialShowing = false
                        TakuAds.fullscreenPresented = false
                        onEvent("interstitial closed")
                    }
                    override fun onInterstitialAdVideoStart(info: ATAdInfo?) = Unit
                    override fun onInterstitialAdVideoEnd(info: ATAdInfo?) = Unit
                    override fun onInterstitialAdVideoError(error: AdError?) = update {
                        interstitialShowing = false
                        TakuAds.fullscreenPresented = false
                        failure("interstitial play", error)
                    }
                })
            }
        }
        if (interstitial?.isAdReady != true) interstitial?.load()
        return true
    }

    fun showInterstitial(): Boolean {
        if (!canShow() || interstitialShowing || rewardShowing || interstitial?.isAdReady != true) return false
        interstitialShowing = true
        TakuAds.fullscreenPresented = true
        interstitial?.show(activity)
        return true
    }

    fun loadRewarded(): Boolean {
        if (destroyed || rewardShowing || BuildConfig.TAKU_REWARDED_ID.isBlank() || !TakuAds.initialize(activity)) return false
        if (rewarded == null) {
            rewarded = ATRewardVideoAd(activity, BuildConfig.TAKU_REWARDED_ID).apply {
                setAdListener(object : ATRewardVideoListener {
                    override fun onRewardedVideoAdLoaded() = event("rewarded loaded")
                    override fun onRewardedVideoAdFailed(error: AdError?) = failure("rewarded load", error)
                    override fun onRewardedVideoAdPlayStart(info: ATAdInfo?) = event("rewarded shown")
                    override fun onRewardedVideoAdPlayEnd(info: ATAdInfo?) = Unit
                    override fun onRewardedVideoAdPlayFailed(error: AdError?, info: ATAdInfo?) = update {
                        rewardShowing = false
                        TakuAds.fullscreenPresented = false
                        failure("rewarded play", error)
                    }
                    override fun onRewardedVideoAdClosed(info: ATAdInfo?) = update {
                        rewardShowing = false
                        TakuAds.fullscreenPresented = false
                        onEvent("rewarded closed")
                    }
                    override fun onRewardedVideoAdPlayClicked(info: ATAdInfo?) = Unit
                    override fun onReward(info: ATAdInfo?): Unit = update {
                        // Closing or finishing playback alone must never grant a reward.
                        if (!rewardDelivered && info != null) {
                            rewardDelivered = true
                            this@FullscreenAdController.onReward(info)
                        }
                    }
                })
            }
        }
        if (rewarded?.isAdReady != true) rewarded?.load()
        return true
    }

    fun showRewarded(): Boolean {
        if (!canShow() || rewardShowing || interstitialShowing || rewarded?.isAdReady != true) return false
        rewardDelivered = false
        rewardShowing = true
        TakuAds.fullscreenPresented = true
        rewarded?.show(activity)
        return true
    }

    private fun canShow() = !destroyed && !activity.isFinishing && !activity.isDestroyed &&
        owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

    private fun update(action: () -> Unit) {
        activity.runOnUiThread { if (!destroyed && !activity.isDestroyed) action() }
    }
    private fun event(message: String) = update { onEvent(message) }
    private fun failure(format: String, error: AdError?) {
        TakuAds.logError(format, error)
        event("$format failed: ${error?.code}")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        destroyed = true
        if (rewardShowing || interstitialShowing) TakuAds.fullscreenPresented = false
        interstitial?.setAdListener(null)
        interstitial?.destroyAd()
        rewarded?.setAdListener(null)
        rewarded?.destroyAd()
        interstitial = null
        rewarded = null
    }
}
