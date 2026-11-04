package com.rescue.flutter_720yun.ads

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.BuildConfig
import com.rescue.flutter_720yun.util.SharedPreferencesUtil

/** Debug APK only, launched with adb; uses the app's existing privacy consent. */
class AdDemoActivity : AppCompatActivity() {
    private var banner: BannerAdController? = null
    private var native: NativeAdAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        setContentView(root)
        val status = TextView(this)
        root.addView(status)
        if (SharedPreferencesUtil.getString("firstOpen", this) != "1") {
            status.text = "请先从应用启动页同意隐私政策"
            return
        }
        status.text = if (TakuAds.initialize(this)) "SDK 已初始化" else "请配置 Android App ID / App Key"
        val fullscreen = FullscreenAdController(this, this, { status.text = it }, { status.text = "收到激励回调（未发放业务奖励）" })
        fun button(title: String, action: () -> Unit) {
            root.addView(Button(this).apply { text = title; setOnClickListener { action() } })
        }
        button("加载插屏") { if (!fullscreen.loadInterstitial()) status.text = "插屏未配置" }
        button("展示插屏") { if (!fullscreen.showInterstitial()) status.text = "插屏未就绪" }
        button("加载激励视频") { if (!fullscreen.loadRewarded()) status.text = "激励视频未配置" }
        button("展示激励视频") { if (!fullscreen.showRewarded()) status.text = "激励视频未就绪" }
        val bannerContainer = FrameLayout(this).apply { visibility = View.GONE }
        root.addView(bannerContainer)
        banner = BannerAdController(this, bannerContainer).also { it.load() }
        val feed = RecyclerView(this).apply { layoutManager = LinearLayoutManager(this@AdDemoActivity) }
        root.addView(feed, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        fun loadNative(id: String) {
            native?.destroy()
            native = NativeAdAdapter(this, this, id)
            feed.adapter = native
            native?.load()
            if (id.isBlank()) status.text = "信息流未配置"
        }
        button("加载信息流 1") { loadNative(BuildConfig.TAKU_NATIVE_ID) }
        button("加载信息流 2") { loadNative(BuildConfig.TAKU_NATIVE_SECONDARY_ID) }
    }

    override fun onDestroy() {
        banner?.destroy()
        native?.destroy()
        super.onDestroy()
    }
}
