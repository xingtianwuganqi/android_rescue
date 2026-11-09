package com.rescue.flutter_720yun.user.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.rescue.flutter_720yun.ActivityController
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityUserAccountSafeBinding
import com.rescue.flutter_720yun.home.models.LoginEvent
import com.rescue.flutter_720yun.user.viewmodels.UserAccountSafeViewModel
import com.rescue.flutter_720yun.util.LoadingDialog
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.util.toastString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class UserAccountSafeActivity : BaseActivity() {

    private var _binding: ActivityUserAccountSafeBinding? = null
    private val binding get() = _binding!!
    private var localUrl: String? = null
    private var confirmationDialog: AlertDialog? = null

    private val viewModel by lazy {
        ViewModelProvider(this)[UserAccountSafeViewModel::class.java]
    }

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
        webView.addJavascriptInterface(AccountSafeClickInterface {
            runWhenAlive {
                val intent = Intent(this, UserAccountSafeActivity::class.java)
                intent.putExtra("localUrl","file:///android_asset/account_deletion.html")
                startActivity(intent)
            }
        },"AccountSafeClickInterface")
        webView.addJavascriptInterface(AccountDeleteClickInterface {
            runWhenAlive { pushDialog() }
        }, "AccountDeleteClickInterface")
        localUrl?.let {
            webView.loadUrl(it)
        }

        // 监听网页的标题变化
        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (title != null && !isFinishing && !isDestroyed) {
                    setupToolbar(title)
                }// 设置标题
            }
        }
    }

    private fun runWhenAlive(action: () -> Unit) {
        runOnUiThread {
            if (!isFinishing && !isDestroyed && _binding != null) action()
        }
    }

    private fun pushDialog() {
        if (confirmationDialog?.isShowing == true) return
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.remind_desc))
        builder.setMessage(resources.getString(R.string.user_account_logout))
        builder.setCancelable(false)
        builder.setPositiveButton(resources.getString(R.string.confirm), DialogInterface.OnClickListener { dialogInterface, i ->
            LoadingDialog.show(this)
            com.rescue.flutter_720yun.analytics.AnalyticsManager.track("logOff",interactive=true)
            viewModel.deleteAccountNetworking()
        })
        builder.setNegativeButton(resources.getString(R.string.cancel), DialogInterface.OnClickListener { dialogInterface, i ->

        })
        val dialog = builder.create()
        confirmationDialog = dialog
        dialog.setOnDismissListener { confirmationDialog = null }
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.color_system))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.color_node))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).textSize = 16F
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).textSize = 16F
    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()
        viewModel.deleteComplete.observe(this) {
            LoadingDialog.hide()
            if (it == true) {
                resources.getString(R.string.user_logout_success).toastString()
                UserManager.logout()
                EventBus.getDefault().post(LoginEvent(null))
                lifecycleScope.launch {
                    delay(1000)
                    ActivityController.finishToLast()
                }
            }else if (it == false){
                resources.getString(R.string.user_logout_fail).toastString()
            }
        }
    }

    override fun onDestroy() {
        confirmationDialog?.dismiss()
        confirmationDialog = null
        _binding?.webView?.let { web ->
            web.stopLoading()
            web.removeJavascriptInterface("AccountSafeClickInterface")
            web.removeJavascriptInterface("AccountDeleteClickInterface")
            web.webChromeClient = null
            (web.parent as? android.view.ViewGroup)?.removeView(web)
            web.destroy()
        }
        _binding = null
        super.onDestroy()
    }

    class AccountSafeClickInterface(private val clickCallback: () -> Unit) {

        @JavascriptInterface
        fun accountSafeClick() {
            clickCallback()
        }
    }


    class AccountDeleteClickInterface(private val clickCallback: () -> Unit) {
        @JavascriptInterface
        fun onConfirmLogout() {
            clickCallback()
        }
    }
}
