package com.rescue.flutter_720yun.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adapter.HomeListAdapter
import com.rescue.flutter_720yun.databinding.FragmentSearchHistoryBinding
import com.rescue.flutter_720yun.databinding.FragmentSearchListBinding
import com.rescue.flutter_720yun.models.HomeListModel
import com.rescue.flutter_720yun.ui.home.OnItemClickListener
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.toastString
import com.rescue.flutter_720yun.viewmodels.SearchViewModel

class SearchListFragment : Fragment(), OnItemClickListener {

    private var _binding: FragmentSearchListBinding? = null
    private val binding: FragmentSearchListBinding get() = _binding!!
    private lateinit var viewModel: SearchViewModel
    private lateinit var adapter: HomeListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchListBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]
        addViewModelObserver()

        context?.let {
            adapter = HomeListAdapter(mutableListOf(), it, this)
            binding.searchList.layoutManager = LinearLayoutManager(context)
            binding.searchList.adapter = adapter
        }

        binding.searchList.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!viewModel.isLastPage.value!! &&
                    lastVisibleItem + 2 >= totalItemCount) {
                    viewModel.searchWord.value?.let {
                        viewModel.searchListNetworking(it, RefreshState.MORE)
                    }
                }
            }
        })

        return binding.root
    }

    private fun addViewModelObserver() {
        viewModel.searchWord.observe(viewLifecycleOwner) {
            viewModel.searchListNetworking(it, RefreshState.REFRESH)
        }

        viewModel.uiState.observe(viewLifecycleOwner) {
            Log.d("TAG", "uiState is Changed $it")
            when (it) {
                is UiState.FirstLoading -> {
                    showFirstLoading()
                }
                is UiState.Success -> {
                    showData()
                    if (viewModel.refreshState.value == RefreshState.REFRESH) {
                        adapter.refreshItem(it.data)
                    }else{
                        adapter.addItems(it.data)
                    }
                }
                is UiState.Error -> {
                    showError(it.message)
                }
            }
        }

        // 观察是否到达最后一页
        viewModel.isLastPage.observe(viewLifecycleOwner)  { isLastPage ->
            if (isLastPage) {
                Log.d("TAG", "is Last Page $isLastPage")
                adapter.showNoMoreData()
            } else {
                adapter.hideNoMoreData()
            }
        }

//        // 观察某个元素发生变化了
//        viewModel.changeModel.observe(viewLifecycleOwner) {
//            it?.let {
//                adapter.uploadItem(it)
//                homeViewModel.cleanChangedModel()
//            }
//        }
//
//        viewModel.errorMsg.observe(viewLifecycleOwner) {
//            it.toastString()
//        }
    }

    private fun showFirstLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.searchList.visibility = View.GONE
        binding.errorView.visibility = View.GONE
    }

    private fun showData() {
        binding.progressBar.visibility = View.GONE
        binding.searchList.visibility = View.VISIBLE
        binding.errorView.visibility = View.GONE
    }

    private fun showError(message: String?) {
        binding.progressBar.visibility = View.GONE
        binding.searchList.visibility = View.GONE
        binding.errorView.visibility = View.VISIBLE

        // 设置错误消息
        binding.errorView.text = message
    }

    override fun userClick(model: HomeListModel?) {

    }

    override fun onItemClick(model: HomeListModel?) {

    }

    override fun onImgClick(model: HomeListModel?, position: Int) {

    }

    override fun likeActionClick(model: HomeListModel?) {

    }

    override fun collectionClick(model: HomeListModel?) {

    }

    override fun commentClick(model: HomeListModel?) {

    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}