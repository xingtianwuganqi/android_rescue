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
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityUserFeedbackBinding

class UserFeedbackActivity : BaseActivity() {

    private var _binding: ActivityUserFeedbackBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_user_feedback)
        setupToolbar(resources.getString(R.string.user_suggestion))


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
        
    }

    override fun addViewAction() {
        super.addViewAction()

    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}