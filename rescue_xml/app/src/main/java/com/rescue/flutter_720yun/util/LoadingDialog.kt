package com.rescue.flutter_720yun.util

import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.rescue.flutter_720yun.R

object LoadingDialog {

    private var dialog: Dialog? = null
    private var owner: AppCompatActivity? = null
    private var observer: DefaultLifecycleObserver? = null
    private val main = Handler(Looper.getMainLooper())

    fun show(context: Context) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post { show(context) }
            return
        }
        val activity = context as? AppCompatActivity ?: return
        if (activity.isFinishing || activity.isDestroyed ||
            activity.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        // 如果已经显示，则不再创建新的
        if (owner === activity && dialog?.isShowing == true) return
        hide()

        // 创建 Dialog
        val loading = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false) // 禁止点击外部关闭
            setContentView(LayoutInflater.from(context).inflate(R.layout.loading_dialog, null))

            // 设置全屏和背景透明度
            window?.apply {
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                setBackgroundDrawableResource(android.R.color.transparent)
            }
        }

        dialog = loading
        owner = activity
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                if (dialog === loading) hide()
            }
        }
        observer = lifecycleObserver
        activity.lifecycle.addObserver(lifecycleObserver)
        loading.setOnDismissListener { if (dialog === loading) clearOwner() }
        loading.show()
    }

    fun hide() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post { hide() }
            return
        }
        val loading = dialog
        clearOwner()
        loading?.dismiss()
    }

    private fun clearOwner() {
        observer?.let { owner?.lifecycle?.removeObserver(it) }
        observer = null
        owner = null
        dialog = null
    }
}
