package com.rescue.flutter_720yun.user.activity

import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityLoginBinding
import com.rescue.flutter_720yun.databinding.ActivityWebPageBinding

class WebPageActivity : BaseActivity() {

    private var _binding: ActivityWebPageBinding? = null
    private val binding get() = _binding!!
    private lateinit var webUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_web_page)
        setupToolbar("")
        _binding = ActivityWebPageBinding.bind(baseBinding.contentFrame.getChildAt(2))
        webUrl = intent.getStringExtra("webUrl").toString()
        Log.d("TAG", webUrl)
        loadWebView()
    }

    private fun loadWebView() {
        val webView = binding.webView

        // 配置 WebView
        webView.settings.apply {
//            javaScriptEnabled = true  // 启用 JavaScript
            domStorageEnabled = true  // 启用 DOM 本地存储
            useWideViewPort = true    // 支持 ViewPort
            loadWithOverviewMode = true // 自适应屏幕
            allowFileAccess = true    // 允许访问文件
        }

        // 在 WebView 内部打开网页，而不是调用外部浏览器
        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }
        }

        // 监听网页的标题变化
        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (title != null) {
                    setupToolbar(title)
                }// 设置标题
            }
        }

        // 加载网页
        webView.loadUrl(webUrl)
    }
}