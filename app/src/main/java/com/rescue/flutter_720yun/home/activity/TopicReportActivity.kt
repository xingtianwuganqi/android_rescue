package com.rescue.flutter_720yun.home.activity

import android.adservices.topics.Topic
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.AbsoluteSizeSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.rescue.flutter_720yun.ActivityController
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityReleaseTopicBinding
import com.rescue.flutter_720yun.databinding.ActivityTopicReportBinding
import com.rescue.flutter_720yun.home.adapter.TopicReportAdapter
import com.rescue.flutter_720yun.home.models.ReportTypeModel
import com.rescue.flutter_720yun.home.viewmodels.TopicReportViewModel
import com.rescue.flutter_720yun.user.activity.WebPageActivity
import com.rescue.flutter_720yun.util.BuildConfig
import com.rescue.flutter_720yun.util.LoadingDialog
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.toastString
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TopicReportActivity : BaseActivity() {

    private var _binding: ActivityTopicReportBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[TopicReportViewModel::class.java]
    }

    private lateinit var adapter: TopicReportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_topic_report)
        setupToolbar(resources.getString(R.string.more_report))
        _binding = ActivityTopicReportBinding.bind(baseBinding.contentFrame.getChildAt(2))

        viewModel.reportId = intent.getIntExtra("reportId", 0)
        viewModel.reportType = intent.getIntExtra("reportType", 0)
        viewModel.userId = intent.getIntExtra("userId", 0)

        addViewAction()
        addViewModelObserver()
        if (viewModel.uiState.value !is UiState.Success) {
            viewModel.reportListNetworking()
        }
    }

    override fun addViewAction() {
        super.addViewAction()
        setProtocolText()
        adapter = TopicReportAdapter(mutableListOf(), { item ->
            val items = adapter.list
            val value = items.map {
                if (it.id == item.id) {
                    it.selected = true
                }else{
                    it.selected = false
                }
                it
            }
            adapter.reloadList(value)
        })
        binding.recyclerview.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerview.adapter = adapter
    }


    @OptIn(DelicateCoroutinesApi::class)
    override fun addViewModelObserver() {
        super.addViewModelObserver()

        viewModel.uiState.observe(this) {
            when (it) {
                is UiState.FirstLoading -> {
                    showLoading()
                }
                is UiState.Success -> {
                    showSuccess()
                    adapter.reloadList(it.data)
                }
                is UiState.Error -> {
                    showError(it.message)
                }
            }
        }

        viewModel.errorMsg.observe(this) {
            it?.toastString()
        }

        viewModel.pushSuccess.observe(this) {
            GlobalScope.launch(Dispatchers.Main) {
                delay(1500)
                finish()
            }
        }
    }

    private fun setProtocolText() {
        // 富文本
        val protocolText = binding.title
        // 创建 SpannableStringBuilder
        val spannableStringBuilder = SpannableStringBuilder()

        // 添加富文本
        val text1 = "${resources.getString(R.string.more_remind)}、"
        val text2 = resources.getString(R.string.more_privacy)

        // 设置样式
        val spannable1 = SpannableString(text1).apply {
            setSpan(AbsoluteSizeSpan(16, true), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // 设置字体大小
            setSpan(
                ContextCompat.getColor(BaseApplication.context, R.color.color_content),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val spannable2 = SpannableString(text2).apply {
            setSpan(
                AbsoluteSizeSpan(16, true),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            ) // 设置字体大小
            setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // 点击事件处理
                    openUserAgree()
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

        // 设置 TextView 文本
        protocolText.text = spannableStringBuilder
        protocolText.movementMethod = LinkMovementMethod.getInstance() // 使点击事件生效
        protocolText.highlightColor = Color.TRANSPARENT // 去掉高亮背景
    }

    fun openUserAgree() {
        val intent = Intent(this, WebPageActivity::class.java)
        val baseUrl = BuildConfig.BASEURL + BuildConfig.USERAGREEN_URL
        intent.putExtra("webUrl", baseUrl)
        startActivity(intent)
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
                saveClickAction()
                true
            }else ->{
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun saveClickAction() {
        val values = adapter.list.filter {
            it.selected
        }
        if (values.isNotEmpty()) {
            values.first().id?.let {
                viewModel.reportActionNetworking(it)
            }
        }else{
            resources.getString(R.string.more_select_remind).toastString()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}