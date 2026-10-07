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
    activity: Activity,
    owner: LifecycleOwner,
    private var onEvent: (String) -> Unit = {},
    private var onReward: (ATAdInfo) -> Unit = {},
    private val onRewardEvent: ((RewardedAdEvent) -> Unit)? = null
) : DefaultLifecycleObserver {
    private val activityRef=java.lang.ref.WeakReference(activity)
    private val ownerRef=java.lang.ref.WeakReference(owner)
    private val activity get()=activityRef.get()
    private val main=android.os.Handler(android.os.Looper.getMainLooper())
    private var interstitial: ATInterstitial? = null
    private var rewarded: ATRewardVideoAd? = null
    private var destroyed = false
    private var rewardDelivered = false
    private var rewardShowing = false
    private var interstitialShowing = false
    private val interstitialToken = Any()
    private val rewardedToken = Any()

    init { owner.lifecycle.addObserver(this) }

    fun loadInterstitial(): Boolean {
        if (destroyed || interstitialShowing || BuildConfig.TAKU_INTERSTITIAL_ID.isBlank() || !TakuAds.initialize(activity ?: return false)) return false
        if (interstitial == null) {
            interstitial = ATInterstitial(activity ?: return false, BuildConfig.TAKU_INTERSTITIAL_ID).apply {
                setAdListener(object : ATInterstitialListener {
                    override fun onInterstitialAdLoaded() = event("interstitial loaded")
                    override fun onInterstitialAdLoadFail(error: AdError?) = failure("interstitial load", error)
                    override fun onInterstitialAdClicked(info: ATAdInfo?) = Unit
                    override fun onInterstitialAdShow(info: ATAdInfo?) = event("interstitial shown")
                    override fun onInterstitialAdClose(info: ATAdInfo?) = update {
                        interstitialShowing = false
                        TakuAds.releaseFullscreen(interstitialToken)
                        onEvent("interstitial closed")
                    }
                    override fun onInterstitialAdVideoStart(info: ATAdInfo?) = Unit
                    override fun onInterstitialAdVideoEnd(info: ATAdInfo?) = Unit
                    override fun onInterstitialAdVideoError(error: AdError?) = update {
                        interstitialShowing = false
                        TakuAds.releaseFullscreen(interstitialToken)
                        failure("interstitial play", error)
                    }
                })
            }
        }
        if (interstitial?.isAdReady != true) interstitial?.load()
        return true
    }

    fun showInterstitial(): Boolean {
        if (!canShow() || TakuAds.fullscreenPresented || com.rescue.flutter_720yun.promotion.RewardedTopicPromotionCoordinator.busy || interstitialShowing || rewardShowing || interstitial?.isAdReady != true) return false
        val page = activity ?: return false
        if(!TakuAds.acquireFullscreen(interstitialToken)) return false
        interstitialShowing = true
        try { interstitial?.show(page) }
        catch(e: Exception) { interstitialShowing=false;TakuAds.releaseFullscreen(interstitialToken);return false }
        return true
    }

    fun loadRewarded(): Boolean {
        if (destroyed || rewardShowing || BuildConfig.TAKU_REWARDED_ID.isBlank() || !TakuAds.initialize(activity ?: return false)) return false
        if (rewarded == null) {
            rewarded = ATRewardVideoAd(activity ?: return false, BuildConfig.TAKU_REWARDED_ID).apply {
                setAdListener(object : ATRewardVideoListener {
                    override fun onRewardedVideoAdLoaded() { event("rewarded loaded"); rewardEvent(RewardedAdEvent.Loaded { showRewarded() }) }
                    override fun onRewardedVideoAdFailed(error: AdError?) { failure("rewarded load", error); rewardEvent(RewardedAdEvent.Failed) }
                    override fun onRewardedVideoAdPlayStart(info: ATAdInfo?) = event("rewarded shown")
                    override fun onRewardedVideoAdPlayEnd(info: ATAdInfo?) = rewardEvent(RewardedAdEvent.PlayEnded)
                    override fun onRewardedVideoAdPlayFailed(error: AdError?, info: ATAdInfo?) {
                        main.post {
                            rewardShowing=false; TakuAds.releaseFullscreen(rewardedToken)
                            failure("rewarded play",error); onRewardEvent?.invoke(RewardedAdEvent.Failed)
                        }
                    }
                    override fun onRewardedVideoAdClosed(info: ATAdInfo?) {
                        main.post {
                            rewardShowing=false; TakuAds.releaseFullscreen(rewardedToken)
                            if(!destroyed) onEvent("rewarded closed")
                            onRewardEvent?.invoke(RewardedAdEvent.Closed)
                        }
                    }
                    override fun onRewardedVideoAdPlayClicked(info: ATAdInfo?) = Unit
                    override fun onReward(info: ATAdInfo?) {
                        main.post {
                            if(!rewardDelivered) {
                                rewardDelivered=true
                                // Business reward observation must survive the presenting page.
                                onRewardEvent?.invoke(RewardedAdEvent.Reward)
                                if(!destroyed && info!=null) onReward(info)
                            }
                        }
                    }

                })
            }
        }
        if (rewarded?.isAdReady != true) rewarded?.load()
        return true
    }

    fun showRewarded(): Boolean {
        if (!canShow() || TakuAds.fullscreenPresented || com.rescue.flutter_720yun.promotion.RewardedTopicPromotionCoordinator.busy && onRewardEvent==null || rewardShowing || interstitialShowing || rewarded?.isAdReady != true) return false
        val page = activity ?: return false
        if(!TakuAds.acquireFullscreen(rewardedToken)) return false
        rewardDelivered = false
        rewardShowing = true
        try { rewarded?.show(page) }
        catch(e: Exception) { rewardShowing=false;TakuAds.releaseFullscreen(rewardedToken);return false }
        return true
    }

    private fun canShow(): Boolean {
        val page=activity ?: return false
        return !destroyed && !page.isFinishing && !page.isDestroyed &&
            ownerRef.get()?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED)==true
    }
    private fun rewardEvent(event: RewardedAdEvent) { main.post { onRewardEvent?.invoke(event) } }
    private fun update(action: () -> Unit) { main.post { if(!destroyed && activity?.isDestroyed==false) action() } }
    private fun event(message: String) = update { onEvent(message) }
    private fun failure(format: String, error: AdError?) {
        TakuAds.logError(format, error)
        event("$format failed: ${error?.code}")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        destroyed = true
        TakuAds.releaseFullscreen(interstitialToken)
        interstitial?.setAdListener(null)
        interstitial?.destroyAd()
        onEvent={}; onReward={}
        onRewardEvent?.invoke(RewardedAdEvent.Destroyed)
        val ad=rewarded
        if(onRewardEvent!=null) {
            // Keep only a context-free reward sink attached for SDK callbacks after close/destroy.
            ad?.setAdListener(LateRewardListener(onRewardEvent, rewardDelivered, rewardedToken))
            val token=rewardedToken
            main.postDelayed({ ad?.setAdListener(null);ad?.destroyAd();TakuAds.releaseFullscreen(token) },60000)
        } else { ad?.setAdListener(null);ad?.destroyAd();TakuAds.releaseFullscreen(rewardedToken) }
        owner.lifecycle.removeObserver(this)
        interstitial = null
        rewarded = null
    }
}

/** Retained briefly by the SDK, with no Activity reference. */
private class LateRewardListener(private val sink: (RewardedAdEvent)->Unit, private var delivered: Boolean, private val token: Any): ATRewardVideoListener {
    private val main=android.os.Handler(android.os.Looper.getMainLooper())
    override fun onReward(info: ATAdInfo?) { main.post { if(!delivered) { delivered=true;sink(RewardedAdEvent.Reward) } } }
    override fun onRewardedVideoAdLoaded()=Unit
    override fun onRewardedVideoAdFailed(error: AdError?)=Unit
    override fun onRewardedVideoAdPlayStart(info: ATAdInfo?)=Unit
    override fun onRewardedVideoAdPlayEnd(info: ATAdInfo?)=Unit
    override fun onRewardedVideoAdPlayFailed(error: AdError?,info: ATAdInfo?) { main.post { TakuAds.releaseFullscreen(token);sink(RewardedAdEvent.Failed) } }
    override fun onRewardedVideoAdClosed(info: ATAdInfo?) { main.post { TakuAds.releaseFullscreen(token);sink(RewardedAdEvent.Closed) } }
    override fun onRewardedVideoAdPlayClicked(info: ATAdInfo?)=Unit
}
