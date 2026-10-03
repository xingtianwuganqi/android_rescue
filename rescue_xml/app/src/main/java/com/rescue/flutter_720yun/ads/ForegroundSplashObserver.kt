package com.rescue.flutter_720yun.ads

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.rescue.flutter_720yun.BuildConfig
import com.rescue.flutter_720yun.SplashActivity
import com.rescue.flutter_720yun.user.activity.SupportActivity
import com.rescue.flutter_720yun.util.SharedPreferencesUtil
import java.lang.ref.WeakReference

/** Only an actual background round trip requests a warm splash; rotations/transitions do not. */
class ForegroundSplashObserver(application: Application) : Application.ActivityLifecycleCallbacks {
    private val handler = Handler(Looper.getMainLooper())
    private val suppressForFirstLaunch = SharedPreferencesUtil.getString("firstOpen", application) != "1"
    private var started = 0
    private var background = false
    private var eligible = false
    private var resumed: WeakReference<Activity>? = null
    private val markBackground = Runnable { background = true }

    override fun onActivityStarted(activity: Activity) {
        handler.removeCallbacks(markBackground)
        started++
    }
    override fun onActivityStopped(activity: Activity) {
        started--
        if (started == 0 && !activity.isChangingConfigurations) {
            eligible = !suppressForFirstLaunch && !TakuAds.fullscreenPresented &&
                activity !is SplashActivity && activity !is WarmSplashActivity && activity !is SupportActivity &&
                activity.javaClass.name.startsWith(activity.packageName)
            handler.postDelayed(markBackground, 700)
        }
    }
    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
        if (!background) return
        background = false
        if (!eligible || TakuAds.fullscreenPresented || activity is SupportActivity ||
            BuildConfig.TAKU_SPLASH_ID.isBlank() || !TakuAds.initialize(activity)) return
        eligible = false
        val reference = WeakReference(activity)
        val deadline = SystemClock.elapsedRealtime() + 3_000
        fun presentWhenStable() {
            val host = reference.get() ?: return
            if (resumed?.get() !== host || host.isFinishing || host.isDestroyed || TakuAds.fullscreenPresented) return
            if (host.hasWindowFocus()) host.startActivity(Intent(host, WarmSplashActivity::class.java))
            else if (SystemClock.elapsedRealtime() < deadline) handler.postDelayed({ presentWhenStable() }, 300)
        }
        handler.postDelayed({ presentWhenStable() }, 300)
    }
    override fun onActivityPaused(activity: Activity) { if (resumed?.get() === activity) resumed = null }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
