package com.rescue.flutter_720yun.message.activity

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityMessageSystemListBinding
import com.rescue.flutter_720yun.message.adapter.MessageSystemItemAdapter
import com.rescue.flutter_720yun.message.viewmodels.MessageSystemListViewModel
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.api.RefreshHeader

class MessageSystemListActivity : BaseActivity() {

    private var _binding: ActivityMessageSystemListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MessageSystemItemAdapter
    private val viewModel by lazy {
        ViewModelProvider(this)[MessageSystemListViewModel::class.java]
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_message_system_list)
        setupToolbar(BaseApplication.context.resources.getString(R.string.message_system))
        _binding = ActivityMessageSystemListBinding.bind(baseBinding.contentFrame.getChildAt(2))


        addViewAction()
        addViewModelObserver()

        if (viewModel.uiState.value !is UiState.Success) {
            viewModel.systemMessageListNetworking(RefreshState.REFRESH)
        }
    }

    override fun addViewAction() {
        super.addViewAction()
        binding.refreshLayout.setRefreshHeader(MaterialHeader(this))
        binding.refreshLayout.setOnRefreshListener {
            viewModel.systemMessageListNetworking(RefreshState.REFRESH)
        }

        adapter = MessageSystemItemAdapter(mutableListOf())
        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        binding.recyclerview.adapter = adapter
    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()
        viewModel.uiState.observe(this) {
            when(it) {
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
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}