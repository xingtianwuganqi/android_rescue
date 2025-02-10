package com.rescue.flutter_720yun.util

import android.content.Context
import android.webkit.JavascriptInterface

class AndroidWebViewInterface(private val context: Context) {

    @JavascriptInterface
    fun onLogoutClick() {
        // 执行本地逻辑，比如弹出提示框

        // 执行更多的原生逻辑，例如跳转到登录页面或清理缓存等
    }
}
