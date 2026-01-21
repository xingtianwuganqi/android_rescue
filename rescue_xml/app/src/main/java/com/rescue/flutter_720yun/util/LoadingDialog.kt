package com.rescue.flutter_720yun.util

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.rescue.flutter_720yun.R

object LoadingDialog {

    private var dialog: Dialog? = null

    fun show(context: Context) {
        // 如果已经显示，则不再创建新的
        if (dialog != null && dialog!!.isShowing) return

        // 创建 Dialog
        dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false) // 禁止点击外部关闭
            setContentView(LayoutInflater.from(context).inflate(R.layout.loading_dialog, null))

            // 设置全屏和背景透明度
            window?.apply {
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                setBackgroundDrawableResource(android.R.color.transparent)
            }
        }

        // 显示 Dialog
        if (context is AppCompatActivity && !context.isFinishing) {
            dialog?.show()
        }
    }

    fun hide() {
        dialog?.dismiss()
        dialog = null
    }
}
