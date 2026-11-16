package com.rescue.flutter_720yun.safety

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.BuildConfig
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.ui.notice
import com.google.android.material.button.MaterialButton

class StartupSafetyGuideActivity : BaseActivity() {
    private lateinit var web: WebView
    private lateinit var next: MaterialButton
    private lateinit var optOut: CheckBox
    private var remaining=10_000L
    private var foreground=false
    private var timer: CountDownTimer?=null
    private var deadline=0L
    private fun dp(v: Int)=(v*resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);setupToolbar(getString(R.string.drawer_guideline),false)
        if(!BuildConfig.STARTUP_SAFETY_ENABLED) { finish();return }
        remaining=savedInstanceState?.getLong("remaining",10_000) ?: 10_000
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE) }
        web=WebView(this).apply {
            settings.javaScriptEnabled=false;settings.allowFileAccess=true;settings.allowContentAccess=false
            settings.blockNetworkLoads=true;settings.defaultTextEncodingName="UTF-8";settings.defaultFontSize=34
            webViewClient=object: WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView,request: WebResourceRequest)=true
                override fun onReceivedError(view: WebView,request: WebResourceRequest,error: WebResourceError) {
                    if(request.isForMainFrame) { notice(getString(R.string.safety_guide_load_failed)) }
                }
            }
        }
        root.addView(web,LinearLayout.LayoutParams(-1,0,1f))
        val footer=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(20),dp(12),dp(20),dp(12)) }
        footer.addView(TextView(this).apply { text=getString(R.string.safety_read_carefully);textSize=13f;gravity=Gravity.CENTER;setTextColor(Color.GRAY) })
        next=MaterialButton(this).apply {
            text=getString(R.string.safety_reading_format,(remaining+999)/1000);isEnabled=false;isAllCaps=false;cornerRadius=dp(24);insetTop=0;insetBottom=0
            setTextColor(Color.WHITE)
            backgroundTintList=android.content.res.ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_enabled),intArrayOf()),
                intArrayOf(ContextCompat.getColor(this@StartupSafetyGuideActivity,R.color.color_aaa),ContextCompat.getColor(this@StartupSafetyGuideActivity,R.color.color_system))
            )
            setOnClickListener {
                if(remaining>0) return@setOnClickListener
                if(StartupSafetyStore(this@StartupSafetyGuideActivity).complete(optOut.isChecked)) finish() else notice(getString(R.string.safety_save_failed))
            }
        }
        footer.addView(next,LinearLayout.LayoutParams(-1,dp(48)).apply { topMargin=dp(15) })
        optOut=CheckBox(this).apply {
            text=getString(R.string.safety_no_more_prompt);textSize=13f;setTextColor(Color.GRAY)
            buttonTintList=android.content.res.ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked),intArrayOf()),
                intArrayOf(ContextCompat.getColor(this@StartupSafetyGuideActivity,R.color.color_system),ContextCompat.getColor(this@StartupSafetyGuideActivity,R.color.color_desc))
            )
            visibility=if(StartupSafetyPolicy.canOptOut(StartupSafetyStore(this@StartupSafetyGuideActivity).completions)) View.VISIBLE else View.GONE
            isChecked=savedInstanceState?.getBoolean("opt_out",false) ?: false
        }
        footer.addView(optOut,LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply { gravity=Gravity.CENTER_HORIZONTAL })
        root.addView(footer);baseBinding.contentFrame.addView(root)
        web.loadUrl("file:///android_asset/prevention.html")
        onBackPressedDispatcher.addCallback(this,object: OnBackPressedCallback(true) { override fun handleOnBackPressed() { finishAffinity() } })
    }
    private fun startTimer() {
        if(timer!=null || !foreground || !::next.isInitialized) return
        if(remaining<=0) { next.text=getString(R.string.safety_read_done);next.isEnabled=true;return }
        next.isEnabled=false
        deadline=SystemClock.elapsedRealtime()+remaining
        timer=object: CountDownTimer(remaining,1000) {
            override fun onTick(value: Long) { remaining=value;next.text=getString(R.string.safety_reading_format,(value+999)/1000) }
            override fun onFinish() { remaining=0;timer=null;next.text=getString(R.string.safety_read_done);next.isEnabled=true }
        }.start()
    }
    private fun pauseTimer() { if(timer!=null) remaining=(deadline-SystemClock.elapsedRealtime()).coerceAtLeast(0);timer?.cancel();timer=null }
    override fun onResume() { super.onResume();foreground=true;startTimer() }
    override fun onPause() { foreground=false;pauseTimer();super.onPause() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("remaining",if(timer!=null) (deadline-SystemClock.elapsedRealtime()).coerceAtLeast(0) else remaining)
        if(::optOut.isInitialized) outState.putBoolean("opt_out",optOut.isChecked)
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() { pauseTimer();if(::web.isInitialized) { web.stopLoading();(web.parent as? android.view.ViewGroup)?.removeView(web);web.destroy() };super.onDestroy() }
}
