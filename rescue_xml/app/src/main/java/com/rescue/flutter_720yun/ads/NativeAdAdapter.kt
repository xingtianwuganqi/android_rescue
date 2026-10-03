package com.rescue.flutter_720yun.ads

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.anythink.core.api.ATAdConst
import com.anythink.core.api.ATAdInfo
import com.anythink.core.api.AdError
import com.anythink.nativead.api.*
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.BuildConfig

/** One optional feed row; no placeholder row on empty configuration or load failure. */
class NativeAdAdapter(
    private val activity: Activity,
    private val owner: LifecycleOwner,
    private val placementId: String = BuildConfig.TAKU_NATIVE_ID
) : RecyclerView.Adapter<NativeAdAdapter.Holder>(), DefaultLifecycleObserver {
    private var loader: ATNative? = null
    private var nativeAd: NativeAd? = null
    private var adView: ATNativeAdView? = null
    private var destroyed = false

    init { owner.lifecycle.addObserver(this) }

    fun load() {
        if (destroyed || loader != null || placementId.isBlank() || !TakuAds.initialize(activity)) return
        loader = ATNative(activity, placementId, object : ATNativeNetworkListener {
            override fun onNativeAdLoaded() {
                activity.runOnUiThread {
                    if (destroyed || activity.isDestroyed || nativeAd != null) return@runOnUiThread
                    nativeAd = loader?.nativeAd ?: return@runOnUiThread
                    if (!owner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) nativeAd?.onPause()
                    nativeAd?.setDislikeCallbackListener(object : ATNativeDislikeListener() {
                        override fun onAdCloseButtonClick(view: ATNativeAdView?, info: ATAdInfo?) {
                            activity.runOnUiThread { removeAd() }
                        }
                    })
                    notifyItemInserted(0)
                }
            }
            override fun onNativeAdLoadFail(error: AdError?) { TakuAds.logError("native", error) }
        }).apply {
            setLocalExtra(mapOf(ATAdConst.KEY.AD_WIDTH to activity.resources.displayMetrics.widthPixels))
            makeAdRequest()
        }
    }

    override fun getItemCount() = if (nativeAd != null && !destroyed) 1 else 0

    class Holder(val view: ATNativeAdView) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(ATNativeAdView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val ad = nativeAd ?: return
        if (adView === holder.view) return
        adView?.takeIf { it !== holder.view }?.let { ad.clear(it) }
        adView = holder.view
        if (ad.isNativeExpress) {
            ad.renderAdContainer(holder.view, null)
            ad.prepare(holder.view, null)
            return
        }
        val material = ad.adMaterial
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val labelRow = LinearLayout(activity)
        val label = TextView(activity).apply {
            text = activity.getString(com.rescue.flutter_720yun.R.string.ad_label)
            textSize = 12f
        }
        labelRow.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val close = Button(activity).apply {
            text = "×"
            contentDescription = activity.getString(com.rescue.flutter_720yun.R.string.ad_close)
            setOnClickListener { removeAd() }
        }
        labelRow.addView(close, LinearLayout.LayoutParams(dp(48), dp(48)))
        content.addView(labelRow)
        val title = TextView(activity).apply { text = material.title; textSize = 16f }
        content.addView(title)
        val description = TextView(activity).apply { text = material.descriptionText }
        content.addView(description)
        val media = FrameLayout(activity)
        content.addView(media, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)))
        val sdkMedia = material.getAdMediaView()
        if (sdkMedia != null) {
            (sdkMedia.parent as? ViewGroup)?.removeView(sdkMedia)
            media.addView(sdkMedia, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        } else {
            val image = ImageView(activity).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
            media.addView(image, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            Glide.with(activity).load(material.mainImageUrl).into(image)
        }
        val cta = Button(activity).apply { text = material.callToActionText }
        content.addView(cta)
        val privacy = TextView(activity).apply { text = "隐私政策" }
        val permission = TextView(activity).apply { text = "权限说明" }
        val function = TextView(activity).apply { text = "功能介绍" }
        material.adAppInfo?.let { appInfo ->
            content.addView(TextView(activity).apply {
                text = "${appInfo.appName} · ${appInfo.publisher} · ${appInfo.appVersion}"
                textSize = 12f
            })
            listOf(privacy, permission, function).forEach { content.addView(it) }
            fun bindLink(view: TextView, url: String?) {
                if (!url.isNullOrBlank()) view.setOnClickListener {
                    val uri = Uri.parse(url)
                    if (uri.scheme in listOf("https", "http")) {
                        runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    }
                }
            }
            bindLink(privacy, appInfo.appPrivacyUrl)
            bindLink(permission, appInfo.appPermissonUrl)
            bindLink(function, appInfo.functionUrl)
        }
        val logo = material.adLogoView
        if (logo != null) {
            (logo.parent as? ViewGroup)?.removeView(logo)
            content.addView(logo)
        }
        ad.renderAdContainer(holder.view, content)
        ad.prepare(holder.view, ATNativePrepareExInfo().apply {
            titleView = title
            descView = description
            mainImageView = media
            ctaView = cta
            adFromView = label
            adLogoView = logo
            clickViewList = listOf(title, description, media, cta)
            privacyClickViewList = listOf(privacy)
            permissionClickViewList = listOf(permission)
            appInfoClickViewList = listOf(function)
        })
    }

    override fun onViewRecycled(holder: Holder) {
        nativeAd?.clear(holder.view)
        if (adView === holder.view) adView = null
        super.onViewRecycled(holder)
    }

    override fun onResume(owner: LifecycleOwner) { nativeAd?.onResume() }
    override fun onPause(owner: LifecycleOwner) { nativeAd?.onPause() }
    override fun onDestroy(owner: LifecycleOwner) { destroy() }

    private fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()

    private fun removeAd() {
        if (destroyed || nativeAd == null) return
        releaseAd()
        notifyItemRemoved(0)
    }

    private fun releaseAd() {
        adView?.let { nativeAd?.clear(it); it.destory() }
        adView = null
        nativeAd?.destory()
        nativeAd = null
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        owner.lifecycle.removeObserver(this)
        loader?.setAdListener(null)
        releaseAd()
        loader?.destroyAd()
        loader = null
    }
}
