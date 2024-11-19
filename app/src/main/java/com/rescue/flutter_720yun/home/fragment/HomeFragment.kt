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
import androidx.activity.result.contract.ActivityResultContracts
import com.rescue.flutter_720yun.home.activity.ReleaseTopicActivity
import com.rescue.flutter_720yun.home.adapter.OnItemClickListener
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.getImages
import com.rescue.flutter_720yun.util.lazyLogin
import com.rescue.flutter_720yun.util.toastString
import com.wei.wimagepreviewlib.WImagePreviewBuilder

class HomeFragment : Fragment(), OnItemClickListener {
    private var rootView : View ?= null
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HomeListAdapter
    private val homeViewModel: HomeViewModel by lazy {
        ViewModelProvider(this)[HomeViewModel::class.java]
    }

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


    companion object {
        fun newInstance(pageType: String): HomeFragment {
            val fragment = HomeFragment()
            val args = Bundle()
            args.putString("pageType", pageType)  // 将参数放入 Bundle
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = arguments?.getString("pageType")  // 从 Bundle 中读取参数
        homeViewModel.pageType = title ?: "0"
        Log.d("TAG","Home Fragment onCreate")
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        Log.d("TAG","Home Fragment onCreateView networking")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        if (homeViewModel.pageType == "1") {
            binding.swipeRefreshLayout.isEnabled = false
            binding.tip.visibility = View.GONE
        }else{
            binding.swipeRefreshLayout.isEnabled = true
            binding.tip.visibility = View.VISIBLE
        }

        binding.tip.setOnClickListener {
            activity?.let { it1 ->
                lazyLogin(it1) {
                    val intent = Intent(activity, ReleaseTopicActivity::class.java)
                    startActivity(intent)
                }
            }
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
        if (homeViewModel.uiState.value !is UiState.Success) {
            loadData(RefreshState.REFRESH)
        }

        Log.d("TAG","Home Fragment onViewCreated networking")
    }

    private fun addListObserver() {

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
            }
        }

        homeViewModel.errorMsg.observe(viewLifecycleOwner) {
            it.toastString()
        }

        homeViewModel.uiState.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.FirstLoading -> {
                    showLoading()
                }

                is UiState.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    showSuccess()
                    if (homeViewModel.refreshState.value == RefreshState.REFRESH) {
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
    }

    private fun refreshData() {
        binding.swipeRefreshLayout.isRefreshing = true
        loadData(RefreshState.REFRESH)
        binding.swipeRefreshLayout.isRefreshing = false
    }

    fun loadMoreData() {
        when (homeViewModel.pageType) {
            "0" -> {
                loadData(RefreshState.MORE)
            }
            "1" -> {
                homeViewModel.searchKeyword?.let { homeViewModel.searchListNetworking(it, RefreshState.MORE) }
            }
            "2" -> {
                homeViewModel.cityName?.let {
                    homeViewModel.localListNetworking(it, RefreshState.MORE)
                }
            }
        }

    }

    private fun loadData(refresh: RefreshState) {
        when (homeViewModel.pageType) {
            "0" -> {
                homeViewModel.loadListData(refresh)
            }

            "1" -> {
                homeViewModel.searchKeyword?.let { homeViewModel.searchListNetworking(it, RefreshState.REFRESH) }
            }

            "2" -> {
                homeViewModel.cityName?.let {
                    homeViewModel.localListNetworking(it, RefreshState.REFRESH)
                }
            }
        }
    }

    // 开始搜索
    fun beginSearch(keyword: String) {
        homeViewModel.searchListNetworking(keyword, RefreshState.REFRESH)
    }

    fun beginLoadLocalCity(city: String) {
        homeViewModel.localListNetworking(city, RefreshState.REFRESH)
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorView.visibility = View.GONE
        binding.swipeRefreshLayout.visibility = View.GONE
    }

    private fun showSuccess() {
        binding.progressBar.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.swipeRefreshLayout.visibility = View.VISIBLE
    }

    private fun showError(error: String? = null) {
        binding.progressBar.visibility = View.GONE
        binding.errorView.visibility = View.VISIBLE
        binding.swipeRefreshLayout.visibility = View.GONE
        error?.let {
            binding.errorView.text = error
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
        val imageArr = model?.getImages()?.map {
            "http://img.rxswift.cn/$it"
        }
        imageArr?.let {
            WImagePreviewBuilder
                .load(this)
                .setData(it)
                .setPosition(position)
                .start()
        }
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