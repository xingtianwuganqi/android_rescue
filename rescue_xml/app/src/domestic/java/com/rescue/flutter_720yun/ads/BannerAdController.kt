package com.rescue.flutter_720yun.ads

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.anythink.banner.api.ATBannerListener
import com.anythink.banner.api.ATBannerView
import com.anythink.core.api.ATAdConst
import com.anythink.core.api.ATAdInfo
import com.anythink.core.api.AdError
import com.rescue.flutter_720yun.BuildConfig

class BannerAdController(private val activity: Activity, private val container: ViewGroup) {
    private var banner: ATBannerView? = null
    private var destroyed = false

    fun load() {
        if (destroyed || banner != null || !TakuAds.initialize(activity) || BuildConfig.TAKU_BANNER_ID.isBlank()) return
        container.post {
            if (destroyed || activity.isDestroyed) return@post
            val containerWidth = container.width.takeIf { it > 0 } ?: activity.resources.displayMetrics.widthPixels
            val width = (containerWidth - container.paddingLeft - container.paddingRight).coerceAtLeast(1)
            val height = (width * 90f / 600).toInt()
            banner = ATBannerView(activity).apply {
                setPlacementId(BuildConfig.TAKU_BANNER_ID)
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
                setLocalExtra(mapOf(ATAdConst.KEY.AD_WIDTH to width, ATAdConst.KEY.AD_HEIGHT to height))
                setBannerAdListener(object : ATBannerListener {
                    override fun onBannerLoaded() = update { container.visibility = View.VISIBLE }
                    override fun onBannerFailed(error: AdError?) = update {
                        TakuAds.logError("banner", error)
                        container.visibility = View.GONE
                    }
                    override fun onBannerClose(info: ATAdInfo?) = update {
                        destroy()
                    }
                    override fun onBannerClicked(info: ATAdInfo?) = Unit
                    override fun onBannerShow(info: ATAdInfo?) = Unit
                    override fun onBannerAutoRefreshed(info: ATAdInfo?) = Unit
                    override fun onBannerAutoRefreshFail(error: AdError?) {
                        TakuAds.logError("banner refresh", error)
                    }
                })
                container.addView(this)
                loadAd()
            }
        }
    }

    private fun update(action: () -> Unit) {
        activity.runOnUiThread { if (!destroyed && !activity.isDestroyed) action() }
    }

    fun destroy() {
        destroyed = true
        banner?.setBannerAdListener(null)
        banner?.destroy()
        banner = null
        container.removeAllViews()
        container.visibility = View.GONE
    }
}
