package com.rescue.flutter_720yun.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityLoginBinding
import com.rescue.flutter_720yun.viewmodels.LoginViewModel
import kotlinx.coroutines.launch
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.ui.text.toLowerCase
import com.google.android.material.button.MaterialButton
import com.rescue.flutter_720yun.BaseApplication
import java.util.Locale
import kotlin.math.log


class LoginActivity : AppCompatActivity() {
    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LoginViewModel
    private var type: String = "login"
    private var phoneNum: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        // 获取页面类型
        type = intent.getStringExtra("type") ?: "login"

        // 找回密码和注册才有phoneNum
        phoneNum = intent.getStringExtra("phoneNum")


        // 返回按钮
        val backBtn = binding.backButton
        backBtn?.setOnClickListener {
            finish()
        }

        // 去注册
        val registerBtn = binding.registerButton
        registerBtn?.setOnClickListener {
            val registerCheck = Intent(this, LoginActivity::class.java)
            registerCheck.putExtra("type", "registerCheckCode")
            startActivity(registerCheck)
        }

        // 去找回密码
        val findBtn = binding.findPassword
        findBtn?.setOnClickListener {
            val findPasswordCheck = Intent(this, LoginActivity::class.java)
            findPasswordCheck.putExtra("type", "findCheckCode")
            startActivity(findPasswordCheck)
        }

        // 账号输入框
        val phoneTextField = binding.username
        // 密码输入框
        val passwordTextField = binding.password

        if (type == "register") {
            phoneTextField.setText(phoneNum)
        }

        // 登录按钮
        val loginBtn: MaterialButton = binding.login as MaterialButton
        loginBtn.setOnClickListener {
            if (type == "login") {
                if (phoneTextField.text.trim().isEmpty()) {
                    val msg = resources.getString(R.string.login_phone_placeholder)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (passwordTextField.text.trim().isEmpty()) {
                    val msg = resources.getString(R.string.login_password_placeholder)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (viewModel.agreement.value == false) {
                    val msg = resources.getString(R.string.login_protocol)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    viewModel.loginNetworking(
                        phoneTextField.text.trim().toString(),
                        passwordTextField.text.trim().toString()
                    )
                }
            }else if (type == "findCheckCode" || type == "registerCheckCode") {
                if (phoneTextField.text.trim().isEmpty()) {
                    val msg = resources.getString(R.string.login_phone_placeholder)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (passwordTextField.text.trim().isEmpty()) {
                    val msg = resources.getString(R.string.login_password_placeholder)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    viewModel.checkCodeNetworking(
                        phoneTextField.text.trim().toString(),
                        passwordTextField.text.trim().toString()
                    )
                }
            }else if (type == "register"){
                if (passwordTextField.text.trim().isEmpty()) {
                    val msg = resources.getString(R.string.login_password_placeholder)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (viewModel.agreement.value == false) {
                    val msg = resources.getString(R.string.login_protocol)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    // 注册
                    viewModel.register(
                        phoneTextField.text.trim().toString(),
                        passwordTextField.text.trim().toString()
                    )
                }
            }else if (type == "findPassword") {
                if (phoneTextField.text.trim().isEmpty()) {
                    val msg = resources.getString(R.string.login_input_password)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (passwordTextField.text.trim().isEmpty()) {
                    val msg = resources.getString(R.string.login_input_password_again)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    // 设置新密码
                    phoneNum?.let {
                        viewModel.findPassword(it,
                            phoneTextField.text.trim().toString(),
                            passwordTextField.text.trim().toString()
                        )
                    }

                }
            }

        }

        // 展示
        val passwordEndLayout = binding.passwordEndLayout
        // 展示密码按钮
        val showPassword = binding.showPassword
        showPassword?.setOnClickListener {
            if (viewModel.showPassword.value == false) {
                viewModel.changeShowPasswordStatus(true)

            }else{
                viewModel.changeShowPasswordStatus(false)
            }
        }

        // 获取验证码
        val getCodeBtn = binding.getCode
        getCodeBtn?.setOnClickListener {
            if (viewModel.isCountDowning.value == true) {
                return@setOnClickListener
            }
            if (phoneTextField.text.trim().isEmpty()) {
                val msg = resources.getString(R.string.login_phone_placeholder)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                viewModel.startCountDown()

                try {
                    val response = viewModel.getCodeNetworking(phoneTextField.text.trim().toString())
                    if (response.code != 200) {
                        val msg = resources.getString(R.string.code_send_error)
                        Toast.makeText(BaseApplication.context, msg, Toast.LENGTH_SHORT).show()
                        viewModel.stopCountDown()
                    }else{
                        Log.d("TAG","fuck ${response.code} ${response.data}")
                    }
                }catch (e: Exception) {
                    Log.d("TAG","fuck ${e}")
                }

            }
        }

        // 协议
        val agreementLayout = binding.agreement
        val agreementBtn = binding.agreementBtn
        agreementBtn?.setOnClickListener {
            if (viewModel.agreement.value == true) {
                viewModel.changeAgreementStatus(false)
            }else{
                viewModel.changeAgreementStatus(true)
            }
        }

        // viewModel
        viewModel.loginStatus.observe(this) {response ->
            response?.let {
                if (response.code == 200) {
                    val msg = resources.getString(R.string.login_success)
                    Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()
                    viewModel.cleanLoginStatus()
                }else {
                    val msg = response.message ?: resources.getString(R.string.login_fail)
                    Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()
                    viewModel.cleanLoginStatus()
                }
            }
        }

        viewModel.agreement.observe(this) {
            if (it) {
                agreementBtn?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.icon_lo_sele))
            }else{
                agreementBtn?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.icon_lo_unse))
            }
        }

        viewModel.showPassword.observe(this) {
            if (it) {
                // 切换到明文模式
                passwordTextField.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                showPassword?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.icon_eye_b))
            }else{
                // 切换到密码模式
                passwordTextField.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                showPassword?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.icon_eye_o))
            }
        }

        viewModel.checkCodeStatus.observe(this) {
            if (it == true) {
                if (type == "findCheckCode") {
                    val register = Intent(this, LoginActivity::class.java)
                    register.putExtra("type", "findPassword")
                    register.putExtra("phoneNum", phoneTextField.text.trim().toString())
                    startActivity(register)

                }else if (type == "registerCheckCode"){
                    val register = Intent(this, LoginActivity::class.java)
                    register.putExtra("type", "register")
                    register.putExtra("phoneNum", phoneTextField.text.trim().toString())
                    startActivity(register)
                }
                viewModel.cleanCheckStatus()
            }else if (it == false){
                val msg = resources.getString(R.string.check_code_fail)
                Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()
                viewModel.cleanCheckStatus()
            }
        }

        viewModel.findPasswordStatus.observe(this) {
            if (it == true) {
                val msg = resources.getString(R.string.uploadPasswordSucc)
                Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()
                viewModel.cleanFindPasswordStatus()
            }else if (it == false){
                val msg = resources.getString(R.string.uploadPasswordFail)
                Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()
                viewModel.cleanFindPasswordStatus()
            }
        }

        viewModel.isCountDowning.observe(this) {
            if (it) {
                return@observe
            }else{
                getCodeBtn?.text = resources.getText(R.string.login_get_code)
            }
        }

        viewModel.countDownStatus.observe(this) {
            if (viewModel.isCountDowning.value == true) {
                getCodeBtn?.text = viewModel.countDownStatus.value?.let { "$it s" }
            }
        }

        viewModel.loginStatus

        // 点击背景取消输入框第一响应者
        val bgContainer = binding.container
        bgContainer.setOnClickListener {
            phoneTextField.clearFocus()
            passwordTextField.clearFocus()

            // 关闭软键盘（可选）
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(phoneTextField.windowToken, 0)
            imm.hideSoftInputFromWindow(passwordTextField.windowToken, 0)

        }

        // 设置富文本
        setProtocolText()

        // 更新页面显示
        // 更具type更新页面显示
        when (type) {
            "login" -> {
                findBtn?.visibility = View.VISIBLE
                registerBtn?.visibility = View.VISIBLE
                agreementLayout?.visibility = View.VISIBLE
                loginBtn.text = resources.getText(R.string.login_action)
                getCodeBtn?.visibility = View.GONE
                showPassword?.visibility = View.VISIBLE
            }
            "registerCheckCode", "findCheckCode" -> {
                findBtn?.visibility = View.GONE
                registerBtn?.visibility = View.GONE
                agreementLayout?.visibility = View.GONE

                passwordTextField.hint = resources.getText(R.string.register_input_code)
                passwordTextField.inputType = InputType.TYPE_CLASS_TEXT
                loginBtn.text = resources.getText(R.string.login_check_code)

                getCodeBtn?.visibility = View.VISIBLE
                showPassword?.visibility = View.GONE
            }

            "findPassword" -> {
                findBtn?.visibility = View.GONE
                registerBtn?.visibility = View.GONE
                agreementLayout?.visibility = View.GONE
                loginBtn.text = resources.getText(R.string.login_find_password)
                showPassword?.visibility = View.VISIBLE
                getCodeBtn?.visibility = View.GONE

                phoneTextField.hint = resources.getText(R.string.login_input_password)
                phoneTextField.inputType = InputType.TYPE_CLASS_TEXT
                passwordTextField.hint = resources.getText(R.string.login_input_password_again)
                passwordTextField.inputType = InputType.TYPE_CLASS_TEXT
            }

            "register" -> {
                findBtn?.visibility = View.GONE
                registerBtn?.visibility = View.GONE
                agreementLayout?.visibility = View.VISIBLE
                loginBtn.text = resources.getText(R.string.register_action)
                showPassword?.visibility = View.VISIBLE
                getCodeBtn?.visibility = View.GONE
                phoneTextField.hint = resources.getText(R.string.login_phone_placeholder)
                passwordTextField.hint = resources.getText(R.string.register_input_password)
            }
        }
    }

    private fun setProtocolText() {
        // 富文本
        val protocolText = binding.protocolLab
        // 创建 SpannableStringBuilder
        val spannableStringBuilder = SpannableStringBuilder()

        // 添加富文本
        val text1 = "阅读并同意"
        val text2 = "用户协议、"
        val text3 = "隐私政策"

        // 设置样式
        val spannable1 = SpannableString(text1).apply {
            setSpan(AbsoluteSizeSpan(12, true), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // 设置字体大小
            setSpan(
                ContextCompat.getColor(BaseApplication.context, R.color.color_node),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val spannable2 = SpannableString(text2).apply {
            setSpan(
                AbsoluteSizeSpan(12, true),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            ) // 设置字体大小
            setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // 点击事件处理

                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false // 添加下划线
                    ds.color = ContextCompat.getColor(BaseApplication.context, R.color.color_system)
                }
            }, 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val spannable3 = SpannableString(text3).apply {
            setSpan(
                AbsoluteSizeSpan(12,true),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // 设置字体大小
            setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // 点击事件处理

                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false // 添加下划线
                    ds.color = ContextCompat.getColor(BaseApplication.context, R.color.color_system)
                }
            }, 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // 拼接
        spannableStringBuilder.append(spannable1)
        spannableStringBuilder.append(spannable2)
        spannableStringBuilder.append(spannable3)

        // 设置 TextView 文本
        protocolText?.text = spannableStringBuilder
        protocolText?.movementMethod = LinkMovementMethod.getInstance() // 使点击事件生效
        protocolText?.highlightColor = Color.TRANSPARENT // 去掉高亮背景
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}