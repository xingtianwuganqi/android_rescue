package com.rescue.flutter_720yun.user.activity

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityUserChangePasswordBinding
import com.rescue.flutter_720yun.user.viewmodels.UserChangePasswordViewModel
import com.rescue.flutter_720yun.util.toastString
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UserChangePasswordActivity : BaseActivity() {

    private var _binding: ActivityUserChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy {
        ViewModelProvider(this)[UserChangePasswordViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_user_change_password)
        setupToolbar(resources.getString(R.string.user_change_password))
        _binding = ActivityUserChangePasswordBinding.bind(baseBinding.contentFrame.getChildAt(2))
        addViewModelObserver()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.findItem(R.id.action_publish)
        menuItem?.title = resources.getString(R.string.user_push)
        // 检查 menuItem 是否不为空
        menuItem?.let {
            val spanString = SpannableString(it.title) // 确保 title 不为空

            // 设置颜色和大小
            val color = ContextCompat.getColor(this, R.color.color_system)
            spanString.setSpan(ForegroundColorSpan(color), 0, spanString.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            spanString.setSpan(AbsoluteSizeSpan(16, true), 0, spanString.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)

            // 设置带格式的标题
            it.title = spanString
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_publish -> {
                pushClickAction()
                true
            }else ->{
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun pushClickAction() {
        if (binding.originPassword.text.trim().toString().isEmpty()) {
            resources.getString(R.string.user_origin_password).toastString()
            return
        }

        if (binding.newPassword.text.trim().toString().isEmpty()) {
            resources.getString(R.string.user_new_password).toastString()
            return
        }

        if (binding.confirmPassword.text.trim().toString().isEmpty()) {
            resources.getString(R.string.user_confirm_password).toastString()
            return
        }

        if (binding.originPassword.text.trim().toString() != binding.newPassword.text.trim().toString()) {
            resources.getString(R.string.user_password_equal).toastString()
            return
        }

        if (binding.newPassword.text.trim().toString() != binding.confirmPassword.text.trim().toString()) {
            resources.getString(R.string.user_confirm_error).toastString()
            return
        }
        viewModel.changePasswordNetworking(
            binding.originPassword.text.trim().toString(),
            binding.newPassword.text.trim().toString(),
            binding.confirmPassword.text.trim().toString()
        )
    }

    override fun addViewAction() {
        super.addViewAction()

    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun addViewModelObserver() {
        super.addViewModelObserver()
        viewModel.statusMessage.observe(this) {
            it.toastString()
        }

        viewModel.changeSuccess.observe(this) {
            if (it == true) {
                GlobalScope.launch(Dispatchers.Main) {
                    delay(2000)
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }

}