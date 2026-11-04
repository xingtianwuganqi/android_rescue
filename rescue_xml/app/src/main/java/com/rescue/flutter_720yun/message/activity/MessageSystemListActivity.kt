package com.rescue.flutter_720yun.message.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.home.activity.LoginActivity
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
    private var loginPending=false
    private val login=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        loginPending=false
        if(UserManager.isLogin) viewModel.systemMessageListNetworking(RefreshState.REFRESH) else finish()
    }
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
        addBackListener()
        UserManager.sessionRevision.observe(this) { if(!UserManager.isLogin) ensureLogin() }
        if(UserManager.isLogin && viewModel.uiState.value !is UiState.Success) viewModel.systemMessageListNetworking(RefreshState.REFRESH)
        else if(!UserManager.isLogin) ensureLogin()
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
        baseBinding.errorView.setOnClickListener { if(UserManager.isLogin) viewModel.systemMessageListNetworking(RefreshState.REFRESH) else ensureLogin() }
    }
    private fun ensureLogin() {
        if(!loginPending) { loginPending=true;login.launch(Intent(this,LoginActivity::class.java)) }
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
                    if(list.isEmpty()) { adapter.refreshItem(emptyList());showError(getString(R.string.no_data));return@observe }
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

    private fun addBackListener() {
        // 注册返回事件的回调
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                sendResultAndFinish()
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    // 返回按钮
    override fun finishAction() {
        super.finishAction()
        sendResultAndFinish()
    }

    private fun sendResultAndFinish() {
        val intent = Intent()
        intent.putExtra("message_result", "1")
        setResult(Activity.RESULT_OK, intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
