package com.rescue.flutter_720yun.home.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.home.activity.HomeDetailActivity
import com.rescue.flutter_720yun.home.activity.LoginActivity
import com.rescue.flutter_720yun.home.adapter.HomeListAdapter
import com.rescue.flutter_720yun.databinding.FragmentHomeBinding
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.home.viewmodels.HomeViewModel
import kotlinx.coroutines.launch
import androidx.activity.result.contract.ActivityResultContracts
import com.rescue.flutter_720yun.home.activity.ReleaseTopicActivity
import com.rescue.flutter_720yun.ui.home.OnItemClickListener
import com.rescue.flutter_720yun.util.toastString


class HomeFragment : Fragment(), OnItemClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HomeListAdapter
    private lateinit var homeViewModel: HomeViewModel

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
            uploadItem(resultData)
        }
    }

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

        binding.tip.setOnClickListener {
            val intent = Intent(activity, ReleaseTopicActivity::class.java)
            startActivity(intent)
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
            if (homeViewModel.isRefreshing.value == true) {
                adapter.refreshItem(it)
                homeViewModel.cleanIsRefreshing()
            }else {
                adapter.addItems(it)
            }
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

        homeViewModel.errorMsg.observe(viewLifecycleOwner) {
            it.toastString()
        }
    }

    private fun refreshData() {
        binding.swipeRefreshLayout.isRefreshing = true
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

    // 收藏
    private fun collectionActionNetworking(model: HomeListModel?) {
        homeViewModel.collectionActionNetworking(model)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 更新item
    private fun uploadItem(model: HomeListModel?) {
        model?.let {
            adapter.uploadItem(model)
        }
    }

    // 用户点击
    override fun userClick(model: HomeListModel?) {

    }

    override fun onItemClick(model: HomeListModel?) {
        val intent = Intent(activity, HomeDetailActivity::class.java)
        intent.putExtra("topic_id", model?.topic_id)
        detailActivityLauncher.launch(intent)
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
            collectionActionNetworking(model)
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