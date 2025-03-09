package com.rescue.flutter_720yun

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rescue.flutter_720yun.user.activity.WebPageActivity
import com.rescue.flutter_720yun.util.BuildConfig
import com.rescue.flutter_720yun.util.SharedPreferencesUtil
import android.graphics.Color
import android.widget.Button


class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // 判断是否已同意隐私政策
        if (SharedPreferencesUtil.getString("firstOpen",this) == "1") {
            navigateToMain()
        } else {
            showPrivacy()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showPrivacy() {
        val str = resources.getString(R.string.welcome_text)
        val inflate = LayoutInflater.from(this).inflate(R.layout.dialog_privacy_show, null)
        val tvTitle = inflate.findViewById<TextView>(R.id.tv_title)
        val privacyDesc = inflate.findViewById<TextView>(R.id.privacy_text)
        tvTitle.text = resources.getString(R.string.welcome_title)
        val tvContent = inflate.findViewById<TextView>(R.id.tv_content)
        val btnAgree = inflate.findViewById<Button>(R.id.btn_agree)
        var btnDisagree = inflate.findViewById<TextView>(R.id.btn_disagree)
        tvContent.text = str
        // 查看完整版《用户协议》和《隐私政策》
        val fullText = "查看完整版《用户协议》和《隐私政策》"
        // 创建 SpannableString
        val spannableString = SpannableString(fullText)
        // 定义"用户协议"的点击事件
        val userAgreementClick = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openUserAgree()
            }
        }
        // 定义"隐私政策"的点击事件
        val privacyPolicyClick = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openPrivacy()
            }
        }
        // 设置不同颜色
        val userAgreementColor = ForegroundColorSpan(Color.parseColor("#ffa500")) // 橙色
        val privacyPolicyColor = ForegroundColorSpan(Color.parseColor("#ffa500")) // 蓝色
        // "用户协议" 位置
        val userStart = fullText.indexOf("《用户协议》")
        val userEnd = userStart + "《用户协议》".length
        // "隐私政策" 位置
        val privacyStart = fullText.indexOf("《隐私政策》")
        val privacyEnd = privacyStart + "《隐私政策》".length
        // 应用点击事件和颜色
        spannableString.setSpan(userAgreementClick, userStart, userEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(userAgreementColor, userStart, userEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(privacyPolicyClick, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(privacyPolicyColor, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // 设置到 TextView
        privacyDesc.text = spannableString
        privacyDesc.movementMethod = LinkMovementMethod.getInstance() // 让链接生效

        btnAgree.setOnClickListener {
            SharedPreferencesUtil.putString("firstOpen", "1", this)
            navigateToMain()
        }

        btnDisagree.setOnClickListener {
            finish()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(inflate)
            .show()

        // 通过WindowManager获取
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        val params = dialog.window?.attributes
        params?.width = dm.widthPixels * 4 / 5
        params?.height = dm.heightPixels / 2
        dialog.setCancelable(false) // 屏蔽返回键
        dialog.window?.attributes = params
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun openUserAgree() {
        val intent = Intent(this, WebPageActivity::class.java)
        val baseUrl = BuildConfig.BASEURL + BuildConfig.USERAGREEN_URL
        intent.putExtra("webUrl",baseUrl)
        startActivity(intent)
    }

    private fun openPrivacy() {
        val intent = Intent(this, WebPageActivity::class.java)
        val baseUrl = BuildConfig.BASEURL + BuildConfig.PRAVICY_URL
        intent.putExtra("webUrl",baseUrl)
        startActivity(intent)
    }
}