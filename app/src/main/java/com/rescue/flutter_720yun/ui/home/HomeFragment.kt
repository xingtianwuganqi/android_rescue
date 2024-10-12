package com.rescue.flutter_720yun.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.activity.HomeDetailActivity
import com.rescue.flutter_720yun.activity.LoginActivity
import com.rescue.flutter_720yun.adapter.HomeListAdapter
import com.rescue.flutter_720yun.databinding.FragmentHomeBinding
import com.rescue.flutter_720yun.models.HomeListModel
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.viewmodels.HomeViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment(), OnItemClickListener {

    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!
    private lateinit var homeAdapter: HomeListAdapter
    private lateinit var adapter: HomeListAdapter
    private lateinit var homeViewModel: HomeViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val recyclerView: RecyclerView = binding.recyclerview
        recyclerView.layoutManager = LinearLayoutManager(context)

        context?.let {
            adapter = HomeListAdapter(mutableListOf(), it, this)
            recyclerView.adapter = adapter
        }


        val swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        recyclerView.addOnScrollListener(object :RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!homeViewModel.isLoading.value!! &&
                    !homeViewModel.isLastPage.value!! &&
                    lastVisibleItem + 1 >= totalItemCount) {
                    loadMoreData()
                }
            }
        })

        addListObserver()
        loadData(RefreshState.REFRESH)

        return root
    }

    private fun addListObserver() {
        homeViewModel.models.observe(viewLifecycleOwner) {
            Log.d("TAG", "fuck it size ${it.size.toString()}")
            adapter.addItems(it)
            homeViewModel.loadingFinish()
        }

        // 观察是否到达最后一页
        homeViewModel.isLastPage.observe(viewLifecycleOwner)  { isLastPage ->
            if (isLastPage) {
                adapter.showNoMoreData()
            } else {
                adapter.hideNoMoreData()
            }
        }

        // 观察某个元素发生变化了
        homeViewModel.changeModel.observe(viewLifecycleOwner) {
            it?.let {
                adapter.uploadItem(it)
                homeViewModel.cleanChangedModel()
            }
        }
    }

    private fun refreshData() {
        binding.swipeRefreshLayout.isRefreshing = true
        adapter.clearItems()
        loadData(RefreshState.REFRESH)
        binding.swipeRefreshLayout.isRefreshing = false
    }

    fun loadMoreData() {
        loadData(RefreshState.MORE)
    }

    private fun loadData(refresh: RefreshState) {
        lifecycleScope.launch {
            homeViewModel.loadListData(refresh)
        }
    }

    // 点赞
    private fun likeActionNetworking(model: HomeListModel?) {
        homeViewModel.likeActionNetworking(model)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun userClick(model: HomeListModel?) {

    }

    override fun onItemClick(model: HomeListModel?) {
        val intent = Intent(activity, HomeDetailActivity::class.java)
        intent.putExtra("topic_id", model?.topic_id)
        startActivity(intent)
    }

    override fun onImgClick(model: HomeListModel?, position: Int) {

    }

    override fun likeActionClick(model: HomeListModel?) {
        if (UserManager.isLogin) {
            // 点赞
            likeActionNetworking(model)
        }else{
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    override fun collectionClick(model: HomeListModel?) {
        if (UserManager.isLogin) {
            // 收藏

        }else{
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    override fun commentClick(model: HomeListModel?) {
        if (UserManager.isLogin) {

        }else{
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }
    }

}