package com.rescue.flutter_720yun.user.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.FragmentUserTopicBinding
import com.rescue.flutter_720yun.home.activity.HomeDetailActivity
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.models.LoginEvent
import com.rescue.flutter_720yun.show.activity.ShowDetailActivity
import com.rescue.flutter_720yun.show.activity.ShowReleaseActivity
import com.rescue.flutter_720yun.show.adapter.ShowPageListAdapter
import com.rescue.flutter_720yun.show.models.ShowPageModel
import com.rescue.flutter_720yun.user.adapter.UserShowListAdapter
import com.rescue.flutter_720yun.user.adapter.UserTopicListAdapter
import com.rescue.flutter_720yun.user.viewmodels.UserTopicViewModel
import com.rescue.flutter_720yun.user.viewmodels.UserViewModel
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.toastString
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class UserTopicFragment: Fragment() {

    private var _binding: FragmentUserTopicBinding? = null
    private val binding get() = _binding!!
    private var from: Int? = null


    // 处理反向传值
    private val detailActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("TAG", "反向传值了")
            val data: Intent? = result.data
            val resultData = data?.getParcelableExtra<HomeListModel>("result_model")
            Log.d("TAG", "data is $data")
            Log.d("TAG", "resultData is $resultData")
            Log.d("TAG", "result data is ${resultData?.topic_id}")
            val deletedValue = data?.getIntExtra("deleted", 0)
            if (deletedValue == 1) {
                deleteItem(resultData)
            }else {
                uploadItem(resultData)
            }
        }
    }

    private val adapter by lazy {
        UserTopicListAdapter(mutableListOf(), { item ->
            val intent = Intent(activity, HomeDetailActivity::class.java)
            intent.putExtra("topic_id", item.topic_id)
            intent.putExtra("topic_from", 1)
            detailActivityLauncher.launch(intent)
        })
    }
    private val showAdapter by lazy {
        UserShowListAdapter(mutableListOf(), { item ->
            val intent = Intent(activity, ShowDetailActivity::class.java)
            intent.putExtra("show_id", item.show_id)
            startActivity(intent)
        })
    }

    private val viewModel by lazy {
        ViewModelProvider(requireParentFragment())[UserViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            from = it.getInt("from")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserTopicBinding.inflate(inflater, container, false)

        // ⚠️ 先移除观察者，防止重复注册
        viewModel.userIdLiveData.removeObservers(viewLifecycleOwner)

        // 监听 ViewModel 数据变化
        viewModel.userIdLiveData.observe(viewLifecycleOwner) { userId ->
            // 更新 UI
            if (userId != 0) {
                loadData(RefreshState.REFRESH)
            }else{
                viewModel.cleanData()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addViewModelObserver()

        binding.refreshLayout.setRefreshHeader(MaterialHeader(activity))
        binding.refreshLayout.setOnRefreshListener {
            loadData(RefreshState.REFRESH)
        }
        binding.refreshLayout.setRefreshFooter(ClassicsFooter(activity))

        binding.refreshLayout.setOnLoadMoreListener {
            loadData(RefreshState.MORE)
        }

        if (from == 0) {
            binding.recyclerview.layoutManager = GridLayoutManager(context, 2)

            binding.recyclerview.adapter = adapter
        }else{
            binding.recyclerview.layoutManager = GridLayoutManager(context, 2)

            binding.recyclerview.adapter = showAdapter
        }

        if (viewModel.uiState.value !is UiState.Success) {
            loadData(RefreshState.REFRESH)
        }

    }

    private fun loadData(refresh: RefreshState) {
        if (from == 0) {
            viewModel.loadUserTopicListNetworking(refresh)
        }else{
            viewModel.loadUserShowListNetworking(refresh)
        }
    }

    private fun addViewModelObserver() {
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
                        if (list.isEmpty()) {
                            adapter.cleanItems()
                        }else if (list.first() is HomeListModel) {
                            val items = list.map {
                                it as HomeListModel
                            }
                            adapter.refreshItem(items)
                        }else if (list.first() is ShowPageModel) {
                            val items = list.map {
                                it as ShowPageModel
                            }
                            showAdapter.refreshItem(items)
                        }
                    }else if (viewModel.refreshState.value == RefreshState.MORE) {
                        if (list.isEmpty()) {
                            return@observe
                        }else if (list.first() is HomeListModel) {
                            val items = list.map {
                                it as HomeListModel
                            }
                            adapter.addItems(items)
                        }else if (list.first() is ShowPageModel) {
                            val items = list.map {
                                it as ShowPageModel
                            }
                            showAdapter.addItems(items)
                        }
                    }
                }

                is UiState.Error -> {
                    showError(it.message ?: "")
                }
            }
        }

        viewModel.isLastPage.observe(viewLifecycleOwner) {
            if (it == false) {
                binding.refreshLayout.finishRefresh()
                binding.refreshLayout.finishLoadMore()
                if (viewModel.isLastPage.value == true) {
                    binding.refreshLayout.finishLoadMoreWithNoMoreData()
                }
            }
        }

    }

    // 更新item
    private fun uploadItem(model: HomeListModel?) {
        model?.let {
            adapter.uploadItem(model)
        }
    }

    private fun deleteItem(model: HomeListModel?) {
        model?.let {
            viewModel.deleteItem(it)
        }
    }

    private fun deleteItemShowInfo(model: ShowPageModel?) {
        model?.let {
            viewModel.deleteShowInfo(model)
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

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(from: Int) =
            UserTopicFragment().apply {
                arguments = Bundle().apply {
//                    putInt("userId", userId)
                    putInt("from", from)
                }
            }
    }
}