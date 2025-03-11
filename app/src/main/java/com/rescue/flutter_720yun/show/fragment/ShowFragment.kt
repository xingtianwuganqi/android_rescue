package com.rescue.flutter_720yun.show.fragment

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.FragmentShowBinding
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.models.LoginEvent
import com.rescue.flutter_720yun.message.activity.CommentListActivity
import com.rescue.flutter_720yun.show.activity.ShowReleaseActivity
import com.rescue.flutter_720yun.show.adapter.ShowItemClickListener
import com.rescue.flutter_720yun.show.adapter.ShowPageListAdapter
import com.rescue.flutter_720yun.show.models.ShowPageModel
import com.rescue.flutter_720yun.show.viewmodels.ShowViewModel
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.util.lazyLogin
import com.rescue.flutter_720yun.util.toastString
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ShowFragment : Fragment(), ShowItemClickListener {

    private var _binding: FragmentShowBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[ShowViewModel::class.java]
    }
    private lateinit var adapter: ShowPageListAdapter


    private val releaseLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("TAG", "反向传值了")
            val data: Intent? = result.data
            val resultValue = data?.getBooleanExtra("published", false)
            if (resultValue == true) {
                // 刷新列表
                loadData(RefreshState.REFRESH)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            viewModel.showId = arguments?.getInt("show_id")
        }
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoginEvent(event: LoginEvent) {
        loadData(RefreshState.REFRESH)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShowBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModelAddObserver()
        if (viewModel.uiState.value !is UiState.Success) {
            loadData(RefreshState.REFRESH)
        }


        binding.refreshLayout.setRefreshHeader(MaterialHeader(activity))
        binding.refreshLayout.setOnRefreshListener {
            loadData(RefreshState.REFRESH)
        }
        if (viewModel.showId == null || viewModel.showId == -1) {
            binding.refreshFooter.visibility = View.VISIBLE
            binding.refreshLayout.setRefreshFooter(ClassicsFooter(activity))

            binding.refreshLayout.setOnLoadMoreListener {
                loadData(RefreshState.MORE)
            }
        }else{
            binding.refreshFooter.visibility = View.GONE
        }

        binding.showList.layoutManager = LinearLayoutManager(context)
        adapter = ShowPageListAdapter(mutableListOf(), this)
        binding.showList.adapter = adapter

        binding.tip.setOnClickListener {
            activity?.let {
                val intent = Intent(it, ShowReleaseActivity::class.java)
                releaseLauncher.launch(intent)
            }

        }
    }

    private fun viewModelAddObserver() {

        viewModel.isLoading.observe(viewLifecycleOwner) {
            if (it == false) {
                binding.refreshLayout.finishRefresh()
                binding.refreshLayout.finishLoadMore()
                if (viewModel.isLastPage.value == true) {
                    binding.refreshLayout.finishLoadMoreWithNoMoreData()
                }
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) {
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

        viewModel.isLoading.observe(viewLifecycleOwner) {
            if (it == false) {
                binding.refreshLayout.finishRefresh()
                binding.refreshLayout.finishLoadMore()
                if (viewModel.isLastPage.value == true) {
                    binding.refreshLayout.finishLoadMoreWithNoMoreData()
                }
            }
        }

        viewModel.errorMsg.observe(viewLifecycleOwner) {
            it?.toastString()
        }

        viewModel.changeModel.observe(viewLifecycleOwner) {
            adapter.uploadItem(it)
        }
    }

    private fun loadData(refreshState: RefreshState) {
        if (viewModel.showId == -1) {
            viewModel.userShowCollectionList(refreshState)
        }else{
            viewModel.showPageListNetworking(refreshState)
        }

    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorView.visibility = View.GONE
        binding.refreshLayout.visibility = View.GONE
    }

    private fun showSuccess() {
        binding.progressBar.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.refreshLayout.visibility = View.VISIBLE
    }

    private fun showError(error: String) {
        binding.progressBar.visibility = View.GONE
        binding.errorView.visibility = View.VISIBLE
        binding.refreshLayout.visibility = View.GONE
        binding.errorView.text = error
    }
    companion object {
        fun newInstance(showId: Int): ShowFragment {
            val args = Bundle()
            args.putInt("show_id", showId)
            val fragment = ShowFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun likeClick(item: ShowPageModel) {
        lazyLogin(this.requireActivity()) {
            viewModel.showLikeNetworking(item)
        }
    }

    override fun collectionClick(item: ShowPageModel) {
        lazyLogin(this.requireActivity()) {
            viewModel.showCollectionNetworking(item)
        }
    }

    override fun commentClick(item: ShowPageModel) {
        lazyLogin(this.requireActivity()) {
            val intent = Intent(activity, CommentListActivity::class.java)
            intent.putExtra("topicId", item.show_id)
            intent.putExtra("topicType", 2)
            intent.putExtra("toUid", item.user?.id)
            startActivity(intent)
        }
    }

    override fun moreClick(item: ShowPageModel) {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }
}