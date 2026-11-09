package com.rescue.flutter_720yun.safety

import android.content.Intent
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.rescue.flutter_720yun.BuildConfig
import com.rescue.flutter_720yun.ads.TakuAds
import com.rescue.flutter_720yun.user.activity.BlackListActivity
import com.rescue.flutter_720yun.user.activity.WebPageActivity
import com.rescue.flutter_720yun.util.AppBuildConfig
import com.rescue.flutter_720yun.util.SharedPreferencesUtil

class StartupSafetyFlow(private val host: FragmentActivity, private val checked: ()->Boolean,private val markChecked: ()->Unit): DefaultLifecycleObserver {
    private var dialog: AlertDialog?=null
    private var retries=0
    private val attempt=Runnable { show() }
    init { host.lifecycle.addObserver(this) }
    fun schedule() { retries=0;host.window.decorView.removeCallbacks(attempt);host.window.decorView.postDelayed(attempt,600) }
    private fun show() {
        if(checked() || host.isFinishing || !host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        if(SharedPreferencesUtil.getString("firstOpen",host)!="1") return
        if(!host.hasWindowFocus() || TakuAds.fullscreenPresented) {
            if(retries++<30) host.window.decorView.postDelayed(attempt,300)
            return
        }
        val store=StartupSafetyStore(host)
        when(store.display(BuildConfig.STARTUP_SAFETY_ENABLED)) {
            StartupSafetyPolicy.Display.NONE -> markChecked()
            StartupSafetyPolicy.Display.GUIDE -> {
                markChecked();host.startActivity(Intent(host,StartupSafetyGuideActivity::class.java))
            }
            StartupSafetyPolicy.Display.DAILY -> {
                val body="特别提醒：请勿相信以任何名义(包括运费、押金、定金等)要求的提前转帐与打款的行为，不要提前转账或打款，若是红包领养，请当面给送养人。请提高警惕，以防被骗！"
                val cases="查看案例->"
                val text=TextView(host).apply {
                    textSize=16f;setPadding(40,24,40,24);movementMethod=LinkMovementMethod.getInstance();highlightColor=Color.TRANSPARENT
                    text=SpannableString(body+cases).apply {
                        fun link(start: Int,end: Int,color: Int,target: Class<*>,url: String?=null) {
                            setSpan(object: ClickableSpan() {
                                override fun onClick(widget: View) {
                                    dialog?.dismiss()
                                    host.startActivity(Intent(host,target).apply { url?.let { putExtra("webUrl",it) } })
                                }
                                override fun updateDrawState(ds: TextPaint) { ds.color=color;ds.isUnderlineText=false }
                            },start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        link(0,body.length,Color.rgb(246,131,31),WebPageActivity::class.java,AppBuildConfig.BASEURL+AppBuildConfig.PREVENTION)
                        link(body.length,body.length+cases.length,Color.rgb(246,111,11),BlackListActivity::class.java)
                    }
                }
                val maxHeight = (host.resources.displayMetrics.heightPixels * 0.55f).toInt()
                val scroll = com.rescue.flutter_720yun.views.MaxHeightScrollView(host).apply {
                    this.maxHeight = maxHeight
                    addView(text)
                }
                val noticeDialog = AlertDialog.Builder(host).setTitle("特别提醒").setView(scroll)
                    .setPositiveButton("确定",null).create()
                dialog = noticeDialog
                noticeDialog.show()
                store.markDaily();markChecked()
            }
        }
    }
    override fun onPause(owner: LifecycleOwner) { host.window.decorView.removeCallbacks(attempt);dialog?.dismiss();dialog=null }
    override fun onDestroy(owner: LifecycleOwner) { host.window.decorView.removeCallbacks(attempt);dialog?.dismiss();dialog=null }
}
