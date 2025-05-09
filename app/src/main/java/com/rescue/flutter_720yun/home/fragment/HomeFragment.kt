package com.rescue.flutter_720yun.home.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.media.metrics.Event
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import androidx.core.content.ContextCompat
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.activity.ReleaseTopicActivity
import com.rescue.flutter_720yun.home.activity.TopicReportActivity
import com.rescue.flutter_720yun.home.adapter.OnItemClickListener
import com.rescue.flutter_720yun.home.adapter.TopicReportAdapter
import com.rescue.flutter_720yun.home.models.LoginEvent
import com.rescue.flutter_720yun.message.activity.CommentListActivity
import com.rescue.flutter_720yun.util.SharedPreferencesUtil
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.getImages
import com.rescue.flutter_720yun.util.lazyLogin
import com.rescue.flutter_720yun.util.toastString
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.wei.wimagepreviewlib.WImagePreviewBuilder
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

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

    private val releaseLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("TAG", "反向传值了")
            val data: Intent? = result.data
            val resultValue = data?.getBooleanExtra("published", false)
            if (resultValue == true) {
                // 刷新列表
                refreshData()
            }
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
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
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

        binding.refreshLayout.setRefreshHeader(MaterialHeader(activity))
        binding.refreshLayout.setRefreshFooter(ClassicsFooter(activity))
        binding.refreshLayout.setOnRefreshListener {
            refreshData()
        }
        binding.refreshLayout.setOnLoadMoreListener {
            loadMoreData()
        }


        if (homeViewModel.pageType == "1") {
            binding.refreshLayout.isEnabled = false
            binding.tip.visibility = View.GONE
        }else{
            binding.refreshLayout.isEnabled = true
            binding.tip.visibility = View.VISIBLE
        }

        binding.tip.setOnClickListener {
            activity?.let { it1 ->
                lazyLogin(it1) {
                    val intent = Intent(activity, ReleaseTopicActivity::class.java)
                    releaseLauncher.launch(intent)
                }
            }
        }

        addListObserver()
        if (homeViewModel.uiState.value !is UiState.Success) {
            loadData(RefreshState.REFRESH)
        }
    }

    private fun addListObserver() {

        // 观察是否到达最后一页
        homeViewModel.isLastPage.observe(viewLifecycleOwner)  { isLastPage ->

        }

        homeViewModel.isLoading.observe(viewLifecycleOwner) {
            if (it == false) {
                binding.refreshLayout.finishRefresh()
                binding.refreshLayout.finishLoadMore()
                if (homeViewModel.isLastPage.value == true) {
                    binding.refreshLayout.finishLoadMoreWithNoMoreData()
                }
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoginEvent(event: LoginEvent) {
        refreshData()
    }

    private fun refreshData() {
        loadData(RefreshState.REFRESH)
    }

    private fun loadMoreData() {
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
            "3" -> {
                homeViewModel.loadUserCollection(RefreshState.MORE)
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
            "3" -> {
                homeViewModel.loadUserCollection(RefreshState.REFRESH)
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
        binding.refreshLayout.visibility = View.GONE
    }

    private fun showSuccess() {
        binding.progressBar.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.refreshLayout.visibility = View.VISIBLE
    }

    private fun showError(error: String? = null) {
        binding.progressBar.visibility = View.GONE
        binding.errorView.visibility = View.VISIBLE
        binding.refreshLayout.visibility = View.GONE
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
            val intent = Intent(activity, CommentListActivity::class.java)
            intent.putExtra("topicId", model?.topic_id)
            intent.putExtra("topicType", 1)
            intent.putExtra("toUid", model?.userInfo?.id)
            startActivity(intent)
        }else{
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    override fun moreButtonClick(model: HomeListModel?) {
        lazyLogin(requireActivity()) {
            val moreBottomShow = MoreBottomFragment()
            moreBottomShow.show(childFragmentManager, moreBottomShow.tag)
            moreBottomShow.clickCallBack = { index ->
                val value = index as String
                when (value) {
                    "0" -> {
                        showBlackDialog(model)
                    }
                    "1" -> {
                        val intent = Intent(activity, TopicReportActivity::class.java)
                        intent.putExtra("reportId", model?.topic_id)
                        intent.putExtra("reportType", 1)
                        startActivity(intent)
                        moreBottomShow.dismiss()

                    }

                    "2" -> {
                        moreBottomShow.dismiss()
                    }
                }
            }
        }
    }

    private fun showBlackDialog(model: HomeListModel?) {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(resources.getString(R.string.more_black_desc))
        builder.setCancelable(false)
        builder.setPositiveButton(resources.getString(R.string.confirm_d), DialogInterface.OnClickListener { dialogInterface, i ->
            moreBlackAction(model)
            if (model != null) {
                adapter.deleteItem(model)
            }
        })
        builder.setNegativeButton(resources.getString(R.string.cancel), DialogInterface.OnClickListener { dialogInterface, i ->

        })
        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireActivity(), R.color.color_system))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireActivity(), R.color.color_node))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).textSize = 16F
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).textSize = 16F
    }

    private fun moreBlackAction(model: HomeListModel?) {
        val topicId = model?.topic_id ?: 0
        val value = SharedPreferencesUtil.getStringSet("black_home_list", requireActivity())
        if (value.isNullOrEmpty()) {
            val list: Set<String> = mutableSetOf("$topicId")
            SharedPreferencesUtil.putStringSet("black_home_list", list, requireActivity())
        }else{
            val data = value.toMutableSet()
            data.add("$topicId")
            SharedPreferencesUtil.putStringSet("black_home_list",
                    data, requireActivity())

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }
}