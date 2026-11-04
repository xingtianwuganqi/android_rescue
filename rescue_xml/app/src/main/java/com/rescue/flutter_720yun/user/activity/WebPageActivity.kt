package com.rescue.flutter_720yun.user.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityWebPageBinding
import java.io.IOException

class WebPageActivity : BaseActivity() {
    private var _binding: ActivityWebPageBinding? = null
    private val binding get() = _binding!!
    private var isAdoptionFlow = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_web_page)
        _binding = ActivityWebPageBinding.bind(baseBinding.contentFrame.getChildAt(2))
        isAdoptionFlow = intent.getBooleanExtra(EXTRA_ADOPTION_FLOW, false)
        setupToolbar(if (isAdoptionFlow) getString(R.string.adoption_flow_title) else "")
        loadWebView()
    }

    private fun loadWebView() {
        val webView = binding.webView
        webView.settings.apply {
            javaScriptEnabled = false
            domStorageEnabled = !isAdoptionFlow
            useWideViewPort = true
            loadWithOverviewMode = true
            allowFileAccess = true
            if (isAdoptionFlow) {
                allowContentAccess = false
                blockNetworkLoads = true
                textZoom = (resources.configuration.fontScale * 100).toInt()
            }
        }
        // The local guide has no JavaScript bridge and cannot navigate to account actions.
        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (!isAdoptionFlow) view.loadUrl(url)
                return true
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!isAdoptionFlow) view.loadUrl(request.url.toString())
                return true
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (isAdoptionFlow && request.isForMainFrame) showGuideError()
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!isAdoptionFlow && title != null) setupToolbar(title)
            }
        }
        if (isAdoptionFlow) {
            baseBinding.errorView.setOnClickListener { loadGuide() }
            loadGuide()
        } else {
            intent.getStringExtra("webUrl")?.let { webView.loadUrl(it) }
        }
    }

    private fun loadGuide() {
        try {
            assets.open(ADOPTION_FLOW_ASSET).use { }
            showSuccess()
            binding.webView.loadUrl(ADOPTION_FLOW_URL)
        } catch (_: IOException) {
            showGuideError()
        }
    }

    private fun showGuideError() = showError(getString(R.string.adoption_flow_load_error))

    override fun onDestroy() {
        _binding?.webView?.apply { stopLoading(); destroy() }
        _binding = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ADOPTION_FLOW = "adoption_flow"
        const val ADOPTION_FLOW_ASSET = "adoptionFlow.html"
        const val ADOPTION_FLOW_URL = "file:///android_asset/adoptionFlow.html"
        fun adoptionFlowIntent(context: Context) = Intent(context, WebPageActivity::class.java)
            .putExtra(EXTRA_ADOPTION_FLOW, true)
    }
}
