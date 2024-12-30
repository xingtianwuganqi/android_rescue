package com.rescue.flutter_720yun.message.activity

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityCommentListBinding
import com.rescue.flutter_720yun.message.adapter.CommentListAdapter
import com.rescue.flutter_720yun.message.viewmodels.CommentListViewModel
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.toastString
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader

class CommentListActivity : BaseActivity() {

    private var _binding: ActivityCommentListBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[CommentListViewModel::class.java]
    }

    private lateinit var adapter: CommentListAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_comment_list)
        val commentText = resources.getString(R.string.home_common)
        setupToolbar(commentText)
        _binding = ActivityCommentListBinding.bind(baseBinding.contentFrame.getChildAt(2))

        val topicType = intent.getIntExtra("topicType", 0)
        val topicId = intent.getIntExtra("topicId", 0)
        viewModel.topicType = topicType
        viewModel.topicId = topicId

        addViewAction()
        addViewModelObserver()

        if (viewModel.uiState.value !is UiState.Success) {
            viewModel.commentListNetworking(RefreshState.REFRESH)
        }
    }

    override fun addViewAction() {
        super.addViewAction()
        binding.refreshLayout.setRefreshHeader(MaterialHeader(this))
        binding.refreshLayout.setRefreshFooter(ClassicsFooter(this))
        binding.refreshLayout.setOnRefreshListener {
            viewModel.commentListNetworking(RefreshState.REFRESH)
        }

        binding.refreshLayout.setOnLoadMoreListener {
            viewModel.commentListNetworking(RefreshState.MORE)
        }

        adapter = CommentListAdapter(mutableListOf())
        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        binding.recyclerview.adapter = adapter

    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()
        viewModel.uiState.observe(this) {
            when(it) {
                is UiState.FirstLoading -> {
                    newShowLoading()
                }

                is UiState.Success -> {
                    newShowData()
                    val list = it.data
                    if (viewModel.refreshState.value == RefreshState.REFRESH) {
                        adapter.refreshItem(list)
                    }else if (viewModel.refreshState.value == RefreshState.MORE) {
                        adapter.addItems(list)
                    }
                }

                is UiState.Error -> {
                    newShowError(it.message ?: "")
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

//        viewModel.errorMsg.observe(this) {
//            it?.toastString()
//        }

//        viewModel.changeModel.observe(this) {
//            adapter.uploadItem(it)
//        }
    }


    fun newShowLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorMessage.visibility = View.GONE
        binding.refreshLayout.visibility = View.GONE
    }

    fun newShowData() {
        binding.progressBar.visibility = View.GONE
        binding.errorMessage.visibility = View.GONE
        binding.refreshLayout.visibility = View.VISIBLE
    }

    fun newShowError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.errorMessage.visibility = View.VISIBLE
        binding.refreshLayout.visibility = View.GONE
        binding.errorMessage.text = message
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}