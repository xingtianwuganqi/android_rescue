package com.rescue.flutter_720yun.user.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.FragmentUserTopicBinding
import com.rescue.flutter_720yun.show.activity.ShowReleaseActivity
import com.rescue.flutter_720yun.show.adapter.ShowPageListAdapter
import com.rescue.flutter_720yun.user.adapter.UserTopicListAdapter
import com.rescue.flutter_720yun.user.viewmodels.UserTopicViewModel
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.toastString
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader

class UserTopicFragment<T> : Fragment() {

    private var _binding: FragmentUserTopicBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: UserTopicListAdapter<T>

    private val viewModel by lazy {
        ViewModelProvider(this)[UserTopicViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            viewModel.userId = it.getInt("userId")
            viewModel.from = it.getInt("from")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserTopicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addViewModelObserver()

        if (viewModel.uiState.value !is UiState.Success) {
            viewModel.loadUserTopicListNetworking(RefreshState.REFRESH)
        }

        binding.refreshLayout.setRefreshHeader(MaterialHeader(activity))
        binding.refreshLayout.setOnRefreshListener {
            viewModel.loadUserTopicListNetworking(RefreshState.REFRESH)
        }
        binding.refreshLayout.setRefreshFooter(ClassicsFooter(activity))

        binding.refreshLayout.setOnLoadMoreListener {
            viewModel.loadUserTopicListNetworking(RefreshState.MORE)
        }

        binding.recyclerview.layoutManager = GridLayoutManager(context, 2)
        adapter = UserTopicListAdapter(mutableListOf(), { item ->

        })
        binding.recyclerview.adapter = adapter

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

        viewModel.isLastPage.observe(viewLifecycleOwner) {
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
        fun newInstance(userId: Int, from: Int) =
            UserTopicFragment().apply {
                arguments = Bundle().apply {
                    putInt("userId", userId)
                    putInt("from", from)
                }
            }
    }
}