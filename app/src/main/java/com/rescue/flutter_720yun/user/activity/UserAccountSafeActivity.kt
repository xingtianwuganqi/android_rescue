package com.rescue.flutter_720yun.user.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityUserAccountSafeBinding
import com.rescue.flutter_720yun.user.viewmodels.UserAccountSafeViewModel

class UserAccountSafeActivity : BaseActivity() {

    private var _binding: ActivityUserAccountSafeBinding? = null
    private val binding get() = _binding!!
    private var localUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_user_account_safe)
        setupToolbar(resources.getString(R.string.user_account))
        _binding = ActivityUserAccountSafeBinding.bind(baseBinding.contentFrame.getChildAt(2))
        localUrl = intent.getStringExtra("localUrl")
        addViewAction()
        addViewModelObserver()
    }


    @SuppressLint("SetJavaScriptEnabled")
    override fun addViewAction() {
        super.addViewAction()
        val webView = binding.webView
        webView.settings.javaScriptEnabled = true // 启用 JavaScript
        webView.addJavascriptInterface(AccountSafeClickInterface(this, {
            val intent = Intent(this, UserAccountSafeActivity::class.java)
            intent.putExtra("localUrl","file:///android_asset/account_safe.html")
            startActivity(intent)
        }),"AccountSafeClickInterface")
        localUrl?.let {
            webView.loadUrl(it)
        }
    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()

    }

    class AccountSafeClickInterface(private val context: Context, private val clickCallback: () -> Unit) {

        @JavascriptInterface
        fun accountSafeClick() {
            clickCallback()
        }
    }

}