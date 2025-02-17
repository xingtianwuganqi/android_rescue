package com.rescue.flutter_720yun.user.activity

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityBlackListBinding
import com.rescue.flutter_720yun.home.adapter.HomeListAdapter
import com.rescue.flutter_720yun.user.adapter.BlackListAdapter
import com.rescue.flutter_720yun.user.viewmodels.BlackListViewModel
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.toastString
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader

class BlackListActivity : BaseActivity() {

    private var _binding: ActivityBlackListBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[BlackListViewModel::class.java]
    }
    private lateinit var adapter: BlackListAdapter

    var detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {

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
        val intent = Intent(this, BlackDetailActivity::class.java)
        detailLauncher.launch(intent)
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

        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        adapter = BlackListAdapter(mutableListOf())
        binding.recyclerview.adapter = adapter
    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()
        viewModel.isLoading.observe(this) {
            if (it == false) {
                binding.refreshLayout.finishRefresh()
                binding.refreshLayout.finishLoadMore()
                if (viewModel.isLastPage.value == true) {
                    binding.refreshLayout.finishLoadMoreWithNoMoreData()
                }
            }
        }

        viewModel.uiState.observe(this) {
            when (it) {
                is UiState.FirstLoading -> {
                    showLoading()
                }

                is UiState.Success -> {
                    showSuccess()
                    val list = it.data
                    if (viewModel.refreshState.value == RefreshState.REFRESH) {
                        adapter.refreshItem(list)
                    }else if (viewModel.refreshState.value == RefreshState.MORE) {
                        adapter.addItems(list)
                    }
                }

                is UiState.Error -> {
                    showError(it.message ?: "")
                }
            }
        }

        viewModel.isLoading.observe(this) {
            if (it == false) {
                binding.refreshLayout.finishRefresh()
                binding.refreshLayout.finishLoadMore()
                if (viewModel.isLastPage.value == true) {
                    binding.refreshLayout.finishLoadMoreWithNoMoreData()
                }
            }
        }

        viewModel.errorMsg.observe(this) {
            it?.toastString()
        }

//        viewModel.changeModel.observe(this) {
//            adapter.uploadItem(it)
//        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}