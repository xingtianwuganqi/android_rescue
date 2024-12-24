package com.rescue.flutter_720yun.message.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagingSource
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityMessageSingleBinding
import com.rescue.flutter_720yun.home.activity.FindPetDetailActivity
import com.rescue.flutter_720yun.home.activity.HomeDetailActivity
import com.rescue.flutter_720yun.home.adapter.FindPetListAdapter
import com.rescue.flutter_720yun.message.adapter.MessageSingleItemAdapter
import com.rescue.flutter_720yun.message.viewmodels.MessageSingleViewModel
import com.rescue.flutter_720yun.show.activity.ShowDetailActivity
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import kotlin.properties.Delegates

class MessageSingleActivity : BaseActivity() {

    private var _binding : ActivityMessageSingleBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[MessageSingleViewModel::class.java]
    }
    private var messageType: Int = 0

    lateinit var adapter: MessageSingleItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_message_single)
        _binding = ActivityMessageSingleBinding.bind(baseBinding.contentFrame.getChildAt(2))

        messageType = intent.getIntExtra("messageType", 0)
        if (messageType == 1) {
            setupToolbar("点赞")
        }else if (messageType == 2) {
            setupToolbar("收藏")
        }else if (messageType == 3) {
            setupToolbar("评论")
        }

        addViewAction()
        addViewModelObserver()
        if (viewModel.uiState.value !is UiState.Success) {
            viewModel.loadMessageListNetworking(RefreshState.REFRESH, messageType)
        }
    }

    override fun addViewAction() {
        super.addViewAction()
        binding.refreshLayout.setRefreshHeader(MaterialHeader(this))
        binding.refreshLayout.setRefreshFooter(ClassicsFooter(this))
        binding.refreshLayout.setOnRefreshListener {
            viewModel.loadMessageListNetworking(RefreshState.REFRESH, messageType)
        }
        binding.refreshLayout.setOnLoadMoreListener {
            viewModel.loadMessageListNetworking(RefreshState.MORE, messageType)
        }

        adapter = MessageSingleItemAdapter(mutableListOf()) { item ->
            /*
             if model.msg_type == 1 || model.msg_type == 2 || model.msg_type == 3 || model.msg_type == 4 {
                self.naviService.navigatorSubject.onNext(NavigatorItem.topicDetail(topicId: model.topicInfo?.topic_id ?? 0, model: nil, changeBlock: nil))
            }else{
                self.naviService.navigatorSubject.onNext(NavigatorItem.showInfoPage(type: .showInfoList, gambitId: nil, showId: model.showInfo?.show_id))
            }
             */
            if (item.msg_type ==1 || item.msg_type == 2 || item.msg_type == 3 || item.msg_type == 4) {
                val intent = Intent(this, HomeDetailActivity::class.java)
                intent.putExtra("topic_id", item.topicInfo?.topic_id)
                startActivity(intent)
            }else if (item.msg_type == 5 || item.msg_type == 6 || item.msg_type == 7 || item.msg_type == 8){
                val intent = Intent(this, ShowDetailActivity::class.java)
                intent.putExtra("show_id", item.showInfo?.show_id)
                startActivity(intent)
            }else if (item.msg_type == 10 || item.msg_type == 11 || item.msg_type == 12 || item.msg_type == 13) {
                // 找宠
            }
        }
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