package com.rescue.flutter_720yun.message.activity

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityCommentListBinding
import com.rescue.flutter_720yun.message.adapter.CommentClickListener
import com.rescue.flutter_720yun.message.adapter.CommentListAdapter
import com.rescue.flutter_720yun.message.viewmodels.CommentListViewModel
import com.rescue.flutter_720yun.show.models.CommentListModel
import com.rescue.flutter_720yun.show.models.ReplyListModel
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.toastString
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import java.util.Timer
import java.util.TimerTask


class CommentListActivity : BaseActivity(), CommentClickListener {

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
        val toUid = intent.getIntExtra("toUid", 0)
        viewModel.topicType = topicType
        viewModel.topicId = topicId
        viewModel.toUid = toUid

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

        adapter = CommentListAdapter(mutableListOf(), this)
        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        binding.recyclerview.adapter = adapter

        binding.commentEdit.setOnFocusChangeListener(object : OnFocusChangeListener{
            override fun onFocusChange(v: View?, hasFocus: Boolean) {
                if (hasFocus) {
                    if (viewModel.currentCommentModel != null && viewModel.currentReplyModel == null) {
                        binding.commentEdit.hint = "回复${viewModel.currentCommentModel?.userInfo?.username}"
                    }else if (viewModel.currentCommentModel == null && viewModel.currentReplyModel != null) {
                        binding.commentEdit.hint = "回复${viewModel.currentReplyModel?.fromInfo?.username}"
                    }else{
                        binding.commentEdit.hint = resources.getString(R.string.comment_placeholder)
                    }
                }
//                else{
//                    viewModel.currentCommentModel = null
//                    viewModel.currentReplyModel = null
//                    binding.commentEdit.hint = resources.getString(R.string.comment_placeholder)
//                }
            }
        })



        binding.commentPublish.setOnClickListener {
            if (binding.commentEdit.text.trim().isNotEmpty()) {
                // 判断是评论还是回复
                if (viewModel.currentCommentModel != null && viewModel.currentReplyModel == null) {
                    binding.commentEdit.text?.let {
                        viewModel.replyActionNetworking(
                            viewModel.topicId ?: 0,
                            viewModel.topicType ?: 0,
                            it.toString(),
                            viewModel.currentCommentModel?.from_uid ?: 0,
                            viewModel.currentCommentModel?.comment_id ?: 0,
                            1,
                            viewModel.currentCommentModel?.comment_id ?: 0
                        )
                    }
                } else if (viewModel.currentCommentModel == null && viewModel.currentReplyModel != null) {
                    binding.commentEdit.text?.let {
                        viewModel.replyActionNetworking(
                            viewModel.topicId ?: 0,
                            viewModel.topicType ?: 0,
                            it.toString(),
                            viewModel.currentReplyModel?.from_uid ?: 0,
                            viewModel.currentReplyModel?.comment_id ?: 0,
                            2,
                            viewModel.currentReplyModel?.reply_id ?: 0
                        )
                    }

                } else if (viewModel.currentCommentModel == null && viewModel.currentReplyModel == null) {
                    binding.commentEdit.text?.let {
                        viewModel.commentActionNetworking(
                            viewModel.topicId ?: 0,
                            viewModel.topicType ?: 0,
                            it.toString(),
                            viewModel.toUid ?: 0
                        )
                    }

                }
            }else{
                ContextCompat.getString(this, R.string.comment_placeholder).toastString()
            }


            // 使EditText失去焦点
            binding.commentEdit.clearFocus();
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            // 隐藏软键盘
            imm.hideSoftInputFromWindow(binding.commentEdit.windowToken, 0);

            binding.commentEdit.text = null
        }

        val rootView: View = this.window.decorView
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            private var wasKeyboardOpened = true

            override fun onGlobalLayout() {
                val r: Rect = Rect()
                rootView.getWindowVisibleDisplayFrame(r)
                val screenHeight = rootView.rootView.height
                val keypadHeight: Int = screenHeight - r.bottom

                val isKeyboardOpened =
                    (keypadHeight > screenHeight * 0.15) // If more than 15% of the screen height, it's probably the keyboard

                if (isKeyboardOpened && !wasKeyboardOpened) {
                    // Keyboard just became visible
                    Log.d("Keyboard", "Keyboard just became visible")
                } else if (!isKeyboardOpened && wasKeyboardOpened) {
                    // Keyboard just became hidden
                    Log.d("Keyboard", "Keyboard just became hidden")
                    if (binding.commentEdit.hasFocus()) {
                        binding.commentEdit.clearFocus()
                    }
                }

                wasKeyboardOpened = isKeyboardOpened
            }
        })
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
                    adapter.refreshItem(list)

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

        viewModel.errorMsg.observe(this) {
            it?.toastString()
        }

//        viewModel.appendReply.observe(this) { item ->
//            item?.let {
//                adapter.insertItem(it)
//            }
//        }

//        viewModel.changeModel.observe(this) {
//            adapter.uploadItem(it)
//        }
    }

    override fun commentAction(item: CommentListModel) {
        viewModel.currentCommentModel = item
        binding.commentEdit.requestFocus() // 请求焦点
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.commentEdit, InputMethodManager.SHOW_IMPLICIT) // 显示软键盘
    }

    override fun replyAction(item: ReplyListModel) {
        viewModel.currentReplyModel = item
        binding.commentEdit.requestFocus() // 请求焦点
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.commentEdit, InputMethodManager.SHOW_IMPLICIT) // 显示软键盘
    }

    override fun loadMoreReplys(item: CommentListModel) {
        item.comment_id?.let {
            viewModel.loadMoreReplyNetworking(it, item.next_page)
        }
    }

    private fun newShowLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorMessage.visibility = View.GONE
        binding.refreshLayout.visibility = View.GONE
    }

    private fun newShowData() {
        binding.progressBar.visibility = View.GONE
        binding.errorMessage.visibility = View.GONE
        binding.refreshLayout.visibility = View.VISIBLE
    }

    private fun newShowError(message: String) {
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