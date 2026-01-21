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
import com.rescue.flutter_720yun.databinding.ActivityUserFeedbackBinding
import com.rescue.flutter_720yun.user.viewmodels.UserFeedbackViewModel
import com.rescue.flutter_720yun.util.LoadingDialog
import com.rescue.flutter_720yun.util.toastString
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UserFeedbackActivity : BaseActivity() {

    private var _binding: ActivityUserFeedbackBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy {
        ViewModelProvider(this)[UserFeedbackViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_user_feedback)
        setupToolbar(resources.getString(R.string.user_suggestion))
        _binding = ActivityUserFeedbackBinding.bind(baseBinding.contentFrame.getChildAt(2))
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
        if (binding.contentEdit.text.trim().toString().isEmpty()) {
            resources.getString(R.string.user_suggestion_title).toastString()
            return
        }

        if (binding.contactEdit.text.trim().toString().isEmpty()) {
            resources.getString(R.string.user_suggestion_phone).toastString()
            return
        }
        LoadingDialog.show(this)
        viewModel.feedBackNetworking(
            binding.contentEdit.text.trim().toString(),
            binding.contactEdit.text.trim().toString()
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun addViewModelObserver() {
        super.addViewModelObserver()
        viewModel.completion.observe(this) {
            LoadingDialog.hide()
            if (it == true) {
                resources.getString(R.string.push_success).toastString()
                GlobalScope.launch(Dispatchers.Main) {
                    delay(2000)
                    finish()
                }
            }else{
                resources.getString(R.string.push_fail).toastString()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}