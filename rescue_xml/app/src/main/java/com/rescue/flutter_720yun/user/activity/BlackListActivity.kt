package com.rescue.flutter_720yun.user.activity

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityBlackListBinding
import com.rescue.flutter_720yun.user.adapter.BlackListAdapter
import com.rescue.flutter_720yun.user.viewmodels.BlackListViewModel
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.lazyLogin
import com.rescue.flutter_720yun.util.toastString
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.wei.wimagepreviewlib.WImagePreviewBuilder

class BlackListActivity : BaseActivity() {

    private var _binding: ActivityBlackListBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[BlackListViewModel::class.java]
    }
    private lateinit var adapter: BlackListAdapter

    var detailLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.blackListNetworking(RefreshState.REFRESH)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_black_list)
        setupToolbar(resources.getString(R.string.user_black))
        _binding = ActivityBlackListBinding.bind(baseBinding.contentFrame.getChildAt(2))

        addViewAction()
        addViewModelObserver()

        if (viewModel.uiState.value !is UiState.Success) {
            viewModel.blackListNetworking(RefreshState.REFRESH)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.findItem(R.id.action_publish)
        menuItem?.title = resources.getString(R.string.user_report)
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
                openReportPage()
                true
            }else ->{
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun openReportPage() {
        lazyLogin(this) {
            val intent = Intent(this, BlackDetailActivity::class.java)
            detailLauncher.launch(intent)
        }
    }

    override fun addViewAction() {
        super.addViewAction()
        val recyclerView: RecyclerView = binding.recyclerview
        recyclerView.layoutManager = LinearLayoutManager(this)

        binding.refreshLayout.setRefreshHeader(MaterialHeader(this))
        binding.refreshLayout.setRefreshFooter(ClassicsFooter(this))
        binding.refreshLayout.setOnRefreshListener {
            viewModel.blackListNetworking(RefreshState.REFRESH)
        }
        binding.refreshLayout.setOnLoadMoreListener {
            viewModel.blackListNetworking(RefreshState.MORE)
        }

        adapter = BlackListAdapter(mutableListOf(), {
            val intent = Intent(this, BlackDetailActivity::class.java)
            intent.putExtra("blackId", it.id)
            detailLauncher.launch(intent)
        }, { urls, index ->
            previewEvidence(urls, index)
        })
        recyclerView.adapter = adapter

        configureEmptyView()
    }

    private fun configureEmptyView() {
        baseBinding.errorView.apply {
            gravity = Gravity.CENTER
            translationY = -resources.displayMetrics.heightPixels * 0.1f
            setOnClickListener {
                showLoading()
                viewModel.blackListNetworking(RefreshState.REFRESH)
            }
        }
    }

    private fun previewEvidence(urls: List<String>, index: Int) {
        if (urls.isEmpty()) return
        WImagePreviewBuilder
            .load(this)
            .setData(urls)
            .setPosition(index)
            .start()
    }

    private fun showEmpty(failed: Boolean) {
        baseBinding.progressBar.visibility = View.GONE
        baseBinding.contentFrame.getChildAt(2).visibility = View.GONE

        val title = SpannableString(
            resources.getString(if (failed) R.string.load_failed_title else R.string.no_data)
        ).apply {
            setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this@BlackListActivity, R.color.color_content)),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                AbsoluteSizeSpan(resources.getDimensionPixelSize(R.dimen.content_font)),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        val desc = SpannableString(
            resources.getString(if (failed) R.string.load_failed_desc else R.string.no_data_desc)
        ).apply {
            setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this@BlackListActivity, R.color.color_desc)),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                AbsoluteSizeSpan(resources.getDimensionPixelSize(R.dimen.address_font)),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        baseBinding.errorView.text = TextUtils.concat(title, "\n", desc)
        baseBinding.errorView.visibility = View.VISIBLE
    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()
        viewModel.isLoading.observe(this) {
            if (it == false) {
                val noMore = viewModel.isLastPage.value == true
                binding.refreshLayout.finishRefresh(200)
                binding.refreshLayout.finishLoadMore(200)
                binding.refreshLayout.setEnableLoadMore(adapter.itemCount > 0)
                binding.refreshLayout.setNoMoreData(noMore)
            }
        }

        viewModel.uiState.observe(this) {
            when (it) {
                is UiState.FirstLoading -> {
                    showLoading()
                }

                is UiState.Success -> {
                    val list = it.data
                    if (viewModel.refreshState.value == RefreshState.MORE) {
                        adapter.addItems(list)
                    } else {
                        adapter.refreshItem(list)
                    }
                    if (adapter.itemCount == 0) {
                        showEmpty(false)
                    } else {
                        showSuccess()
                    }
                }

                is UiState.Error -> {
                    if (adapter.itemCount == 0) {
                        showEmpty(true)
                    } else {
                        showSuccess()
                    }
                }
            }
        }

        viewModel.errorMsg.observe(this) {
            it?.toastString()
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
