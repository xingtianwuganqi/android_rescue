package com.rescue.flutter_720yun.ads

import android.app.Activity
import androidx.lifecycle.LifecycleOwner

/** Disabled implementation: never requests ads or emits reward callbacks. */
class FullscreenAdController(
    activity: Activity,
    owner: LifecycleOwner,
    onEvent: (String) -> Unit = {},
    onReward: () -> Unit = {},
    onRewardEvent: ((RewardedAdEvent) -> Unit)? = null
) {
    fun destroy() = Unit
    fun loadInterstitial() = false
    fun showInterstitial() = false
    fun loadRewarded() = false
    fun showRewarded() = false
}
