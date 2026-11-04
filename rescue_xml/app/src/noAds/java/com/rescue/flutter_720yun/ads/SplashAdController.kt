package com.rescue.flutter_720yun.ads

import android.app.Activity
import android.view.ViewGroup

/** Continue startup as soon as the host resumes, without an ad timeout. */
class SplashAdController(
    private val activity: Activity,
    private val container: ViewGroup,
    private val onComplete: () -> Unit,
    loadTimeoutMs: Int = 5_000
) {
    private var completed = false
    fun load() = Unit
    fun onResume() {
        if (!completed && !activity.isFinishing && !activity.isDestroyed) {
            completed = true
            onComplete()
        }
    }
    fun onPause() = Unit
    fun destroy() { completed = true; container.removeAllViews() }
}
