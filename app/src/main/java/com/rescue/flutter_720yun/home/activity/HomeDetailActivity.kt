package com.rescue.flutter_720yun.home.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityHomeDetailBinding
import com.rescue.flutter_720yun.home.adapter.DetailImgClickListener
import com.rescue.flutter_720yun.home.adapter.HomeDetailAdapter
import com.rescue.flutter_720yun.home.fragment.HomeDetailMoreFragment
import com.rescue.flutter_720yun.home.models.HomeDetailModel
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.viewmodels.HomeDetailViewModel
import com.rescue.flutter_720yun.util.getImages
import com.rescue.flutter_720yun.util.lazyLogin
import com.rescue.flutter_720yun.util.toastString
import com.wei.wimagepreviewlib.WImagePreviewBuilder


class HomeDetailActivity : BaseActivity(), DetailImgClickListener {

    private var _binding: ActivityHomeDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[HomeDetailViewModel::class.java]
    }

    private lateinit var adapter: HomeDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_home_detail)
        _binding = ActivityHomeDetailBinding.bind(baseBinding.contentFrame.getChildAt(2))
        setupToolbar("详情")

        viewModel.topicId = intent.getIntExtra("topic_id", 1)
        viewModel.topicFrom = intent.getIntExtra("topic_from", 0)

        addViewAction()
        addViewModelObserver()
        addBackListener()

        viewModel.topicId?.let {
            viewModel.loadDetailNetworking(it)
        }
    }


    override fun addViewModelObserver() {
        viewModel.homeData.observe(this) {
            uploadViews(it)
            uploadBottom(it)
        }

//        viewModel.changeModel.observe(this) {
//            uploadBottom(it)
//        }

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
        }

        viewModel.deleted.observe(this) {

        }

    }

    private fun uploadViews(homeData: HomeListModel?) {


        val detailList = mutableListOf<HomeDetailModel>()
        val contentModel = HomeDetailModel(0, homeData, null, viewModel.topicFrom)
        detailList.add(contentModel)
        homeData?.getImages()?.forEach {
            val imageModel = HomeDetailModel(1, null, it, viewModel.topicFrom)
            detailList.add(imageModel)
        }
        adapter.reloadItems(detailList)
//        val adapter = HomeDetailAdapter(detailList)
//        adapter.setOnClickListener(this)
//        imgRecyclerView.adapter = adapter
    }

    override fun clickItem(model: List<HomeDetailModel>, position: Int) {
        val imgUrls = model.filter {
            it.imageStr != null
        }.map {
            "http://img.rxswift.cn/${it.imageStr}"
        }
        WImagePreviewBuilder
            .load(this)
            .setData(imgUrls)
            .setPosition(position-1)
            .start()
    }

    override fun moreClick(model: HomeDetailModel) {
        showMoreAlert(viewModel.homeData.value)
    }

    private fun showMoreAlert(model: HomeListModel?) {
        val status = if (model?.is_complete == true) 1 else 0
        Log.d("TAG", "$status")
        val bottomAlert = HomeDetailMoreFragment.newInstance(status)
        bottomAlert.show(supportFragmentManager, bottomAlert.tag)
        bottomAlert.clickCallBack = { value ->
            if (value == 0 || value == 1) {
                viewModel.changeCompleteStatus(model)
            }else if (value == 2) {
                showDeleteDialog(model)
            }

        }
    }

    private fun showDeleteDialog(model: HomeListModel?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.topic_delete_title))
        builder.setMessage(resources.getString(R.string.topic_delete_content))
        builder.setCancelable(false)
        builder.setPositiveButton(resources.getString(R.string.confirm_d), DialogInterface.OnClickListener { dialogInterface, i ->
            viewModel.deleteTopic(model)
        })
        builder.setNegativeButton(resources.getString(R.string.cancel), DialogInterface.OnClickListener { dialogInterface, i ->

        })
        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.color_system))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.color_node))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).textSize = 16F
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).textSize = 16F
    }

    private fun uploadBottom(homeData: HomeListModel?) {
        if (homeData?.liked == true) {
            // 获取 drawable 资源（图标）
            val newIcon: Drawable? = ContextCompat.getDrawable(this, R.drawable.icon_zan_se)
            // 设置新图标
            binding.likeButton.icon = newIcon
        }else{
            // 获取 drawable 资源（图标）
            val newIcon: Drawable? = ContextCompat.getDrawable(this, R.drawable.icon_zan_un)
            // 设置新图标
            binding.likeButton.icon = newIcon
        }

        if (homeData?.collectioned == true) {
            // 获取 drawable 资源（图标）
            val newIcon: Drawable? = ContextCompat.getDrawable(this, R.drawable.icon_collection_se)
            // 设置新图标
            binding.collectButton.icon = newIcon
        }else{
            // 获取 drawable 资源（图标）
            val newIcon: Drawable? = ContextCompat.getDrawable(this, R.drawable.icon_collection_un)
            // 设置新图标
            binding.collectButton.icon = newIcon
        }

        if (homeData?.is_complete == true) {
            binding.getContactBtn.text = resources.getString(R.string.user_completion)
        }else {
            if (homeData?.getedcontact == true && homeData.contact_info != null) {
                binding.getContactBtn.text = homeData.contact_info
            } else {
                binding.getContactBtn.text =
                    BaseApplication.context.resources.getString(R.string.click_get_contact)
            }
        }

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
        if (viewModel.homeDataChanged.value == true) {
            val intent = Intent()
            intent.putExtra("result_model", viewModel.homeData.value)
            setResult(Activity.RESULT_OK, intent)
        }
    }

    override fun addViewAction() {
        val imgRecyclerView = binding.imagesRecyclerview
        imgRecyclerView.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.VERTICAL,
            false)
        adapter = HomeDetailAdapter(mutableListOf())
        adapter.setOnClickListener(this)
        imgRecyclerView.adapter = adapter

        binding.likeButton.setOnClickListener{
            lazyLogin(this) {
                viewModel.homeData.value?.let {
                    viewModel.likeActionNetworking(it)
                }
            }
        }

        binding.collectButton.setOnClickListener {
            lazyLogin(this) {
                viewModel.homeData.value?.let {
                    viewModel.collectionActionNetworking(it)
                }
            }
        }

        binding.commentButton.setOnClickListener {
            lazyLogin(this) {

            }
        }

        binding.getContactBtn.setOnClickListener {
            lazyLogin(this) {
                if (viewModel.homeData.value?.is_complete == false) {
                    if (viewModel.homeData.value?.getedcontact == true && viewModel.homeData.value?.contact_info != null) {
                        // 复制
                        viewModel.homeData.value?.contact_info?.let {
                            copy(this, it)
                            resources.getString(R.string.copy_success_copy).toastString()
                        }
                    } else {
                        viewModel.homeData.value?.let {
                            viewModel.clickGetContactInfoNetworking(it)
                        }
                    }
                }else{
                    resources.getString(R.string.user_completion).toastString()
                }
            }
        }
    }

    //系统剪贴板-复制:   s为内容
    private fun copy(context: Context, s: String?) {
        // 获取系统剪贴板
        val clipboard: ClipboardManager =
            context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        // 创建一个剪贴数据集，包含一个普通文本数据条目（需要复制的数据）
        val clipData = ClipData.newPlainText(null, s)
        // 把数据集设置（复制）到剪贴板
        clipboard.setPrimaryClip(clipData)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}