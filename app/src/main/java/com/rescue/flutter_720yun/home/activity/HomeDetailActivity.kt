package com.rescue.flutter_720yun.home.activity

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.adapter.HomeDetailAdapter
import com.rescue.flutter_720yun.databinding.ActivityHomeDetailBinding
import com.rescue.flutter_720yun.home.models.HomeDetailModel
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.util.getImages
import com.rescue.flutter_720yun.util.lazyLogin
import com.rescue.flutter_720yun.home.viewmodels.HomeDetailViewModel
import kotlinx.coroutines.launch

class HomeDetailActivity : BaseActivity() {

    private var _binding: ActivityHomeDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeDetailViewModel
    private var topicId: Int? = null
    private var topic: HomeListModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_home_detail)
        _binding = ActivityHomeDetailBinding.bind(baseBinding.contentFrame.getChildAt(2))
        setupToolbar("详情")
        viewModel = ViewModelProvider(this)[HomeDetailViewModel::class.java]

        topicId = intent.getIntExtra("topic_id", 1)

        topicId?.let {
            viewModel.loadDetailNetworking(it)
        }

        addViewModelObserver()
        addViewAction()
    }


    override fun addViewModelObserver() {
        viewModel.homeData.observe(this) {
            uploadViews(it)
            uploadBottom(it)
        }

        viewModel.changeModel.observe(this) {
            uploadBottom(it)
            topic = it
        }

        viewModel.statusCode.observe(this) {
            it?.let { code ->
                when (code) {
                    209 -> {
                        // 去绑定手机号
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.putExtra("type", "bindPhone")
                        startActivity(intent)
                    }
                    210 -> {
                        // 去校验手机号
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.putExtra("type", "checkPhone")
                        startActivity(intent)
                    }
                }
            }
            viewModel.cleanStatusCode()
        }

    }

    private fun uploadViews(homeData: HomeListModel?) {
        val imgRecyclerView = binding.imagesRecyclerview
        imgRecyclerView.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.VERTICAL,
            false)

        val detailList = mutableListOf<HomeDetailModel>()
        val contentModel = HomeDetailModel(0, homeData, null)
        detailList.add(contentModel)
        homeData?.getImages()?.forEach {
            val imageModel = HomeDetailModel(1, null, it)
            detailList.add(imageModel)
        }
        val adapter = HomeDetailAdapter(detailList)
        imgRecyclerView.adapter = adapter
    }


    private fun uploadBottom(homeData: HomeListModel?) {
        if (homeData?.liked == true) {
            // 获取 drawable 资源（图标）
            val newIcon: Drawable? = ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_zan_se)
            // 设置新图标
            binding.likeButton.icon = newIcon
        }else{
            // 获取 drawable 资源（图标）
            val newIcon: Drawable? = ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_zan_un)
            // 设置新图标
            binding.likeButton.icon = newIcon
        }

        if (homeData?.collectioned == true) {
            // 获取 drawable 资源（图标）
            val newIcon: Drawable? = ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_collection_se)
            // 设置新图标
            binding.collectButton.icon = newIcon
        }else{
            // 获取 drawable 资源（图标）
            val newIcon: Drawable? = ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_collection_un)
            // 设置新图标
            binding.collectButton.icon = newIcon
        }

        if (homeData?.getedcontact == true && homeData?.contact_info != null) {
            binding.getContactBtn.text = homeData?.contact_info
        }else{
            binding.getContactBtn.text = BaseApplication.context.getString(R.string.click_get_contact)
        }

        addBackListener()
    }

    private fun addBackListener() {
        // 注册返回事件的回调
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                sendResultAndFinish()
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun finishAction() {
        super.finishAction()
        sendResultAndFinish()
    }

    private fun sendResultAndFinish() {
        topic?.let {
            val intent = Intent()
            intent.putExtra("result_model", it)
            setResult(Activity.RESULT_OK, intent)
        }

    }

    override fun addViewAction() {
        val likeBtn = binding.likeButton
        likeBtn.setOnClickListener{
            lazyLogin(this) {
                lifecycleScope.launch {
                    viewModel.homeData.value?.let {
                        viewModel.likeActionNetworking(it)
                    }
                }
            }
        }

        val collectionBtn = binding.collectButton
        collectionBtn.setOnClickListener {
            lazyLogin(this) {
                viewModel.homeData.value?.let {
                    viewModel.collectionActionNetworking(it)
                }
            }
        }

        val commentBtn = binding.commentButton
        commentBtn.setOnClickListener {
            lazyLogin(this) {

            }
        }

        binding.getContactBtn.setOnClickListener {
            lazyLogin(this) {
                viewModel.homeData.value?.let {
                    viewModel.clickGetContactInfoNetworking(it)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}