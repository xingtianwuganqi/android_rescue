package com.rescue.flutter_720yun.ads

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

/** Returning from the background must retain the current page and navigation stack. */
class WarmSplashActivity : AppCompatActivity() {
    private var controller: SplashAdController? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = FrameLayout(this)
        setContentView(container)
        controller = SplashAdController(this, container, ::finish, 3_000).also { it.load() }
    }
    override fun onResume() { super.onResume(); controller?.onResume() }
    override fun onPause() { controller?.onPause(); super.onPause() }
    override fun onDestroy() { controller?.destroy(); controller = null; super.onDestroy() }
}
