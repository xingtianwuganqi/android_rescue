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
import androidx.recyclerview.widget.ConcatAdapter
import com.rescue.flutter_720yun.ads.DetailBannerAdapter
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityHomeDetailBinding
import com.rescue.flutter_720yun.home.adapter.DetailImgClickListener
import com.rescue.flutter_720yun.home.adapter.HomeDetailAdapter
import com.rescue.flutter_720yun.home.models.HomeDetailModel
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.viewmodels.HomeDetailViewModel
import com.rescue.flutter_720yun.message.activity.CommentListActivity
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

    private var deleteDialog: AlertDialog? = null
    private lateinit var adapter: HomeDetailAdapter
    private lateinit var imageAdapter: HomeDetailAdapter
    private lateinit var promotionEntry: com.rescue.flutter_720yun.promotion.PromotionEntry
    private lateinit var adoptionFlow: com.rescue.flutter_720yun.adoption.ui.AdoptionDetailFlow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_home_detail)
        _binding = ActivityHomeDetailBinding.bind(baseBinding.contentFrame.getChildAt(2))
        setupToolbar("详情")

        val topic = intent.getIntExtra("topic_id", 0)
        if (topic <= 0) { "帖子参数无效".toastString(); finish(); return }
        viewModel.topicId = topic
        adoptionFlow = com.rescue.flutter_720yun.adoption.ui.AdoptionDetailFlow(this, binding.getContactBtn)
        adoptionFlow.setTopic(topic)
        promotionEntry=com.rescue.flutter_720yun.promotion.PromotionEntry(this,binding.promotionButton,topic,"my_posts")
        ViewModelProvider(this)[com.rescue.flutter_720yun.adoption.viewmodels.AdoptionDetailViewModel::class.java].state.observe(this) { state ->
            promotionEntry.setOwner(state?.can_manage==true,state?.workflow_status)
        }
        viewModel.topicFrom = intent.getIntExtra("topic_from", 0)

        addViewAction()
        addViewModelObserver()
        addBackListener()

        viewModel.topicId?.let {
            viewModel.loadDetailNetworking(it)
        }
    }


    override fun addViewModelObserver() {
        viewModel.errorMsg.observe(this) { it?.toastString() }
        viewModel.homeData.observe(this) {
            uploadViews(it)
            uploadBottom(it)
        }

//        viewModel.changeModel.observe(this) {
//            uploadBottom(it)
//        }

        viewModel.deleted.observe(this) {
            if(it == true) {
                resources.getString(R.string.delete_success).toastString()
                sendResultAndFinish(); finish()
            }
        }

    }

    private fun uploadViews(homeData: HomeListModel?) {


        val contentModel = HomeDetailModel(0, homeData, null, viewModel.topicFrom)
        adapter.reloadItems(mutableListOf(contentModel))
        val imageList = mutableListOf<HomeDetailModel>()
        homeData?.getImages()?.forEach {
            val imageModel = HomeDetailModel(1, null, it, viewModel.topicFrom)
            imageList.add(imageModel)
        }
        imageAdapter.reloadItems(imageList)
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
            .setPosition(model.take(position).count { it.imageStr != null })
            .start()
    }

    override fun moreClick(model: HomeDetailModel) {
        showMoreAlert()
    }

    private fun showMoreAlert() {
        adoptionFlow.ownerMore()
    }

    fun confirmDeleteTopic() { showDeleteDialog(viewModel.homeData.value) }
    fun reopenLegacyTopic() { viewModel.reopenLegacyTopic(viewModel.homeData.value) }
    fun reloadAdoptionTopic() { viewModel.markTopicChanged(); viewModel.topicId?.let { viewModel.loadDetailNetworking(it) }; adoptionFlow.refresh() }
    fun copyAuthorizedContact(value: String) { copy(this, value); resources.getString(R.string.copy_success_copy).toastString() }

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
        deleteDialog = dialog
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

        // Contact/application text is exclusively controlled by the V2 application-state flow.
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
            intent.putExtra("result_model", viewModel.homeData.value?.also { it.contact_info = null; it.getedcontact = false })
            if (viewModel.deleted.value == true) {
                intent.putExtra("deleted", 1)
            }
            setResult(Activity.RESULT_OK, intent)
        }else{
            if (viewModel.deleted.value == true) {
                val intent = Intent()
                intent.putExtra("result_model", viewModel.homeData.value?.also { it.contact_info = null; it.getedcontact = false })
                intent.putExtra("deleted", 1)
                setResult(Activity.RESULT_OK, intent)
            }
        }
    }

    override fun addViewAction() {
        val imgRecyclerView = binding.imagesRecyclerview
        imgRecyclerView.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.VERTICAL,
            false)
        adapter = HomeDetailAdapter(mutableListOf())
        adapter.setOnClickListener(this)
        imageAdapter = HomeDetailAdapter(mutableListOf())
        imageAdapter.setOnClickListener(this)
        imgRecyclerView.adapter = ConcatAdapter(adapter, DetailBannerAdapter(this, this), imageAdapter)

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
                val intent = Intent(this, CommentListActivity::class.java)
                intent.putExtra("topicId", viewModel.homeData.value?.topic_id)
                intent.putExtra("topicType", 1)
                intent.putExtra("toUid", viewModel.homeData.value?.userInfo?.id)
                startActivity(intent)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (::adoptionFlow.isInitialized) {
            adoptionFlow.refresh()
            viewModel.topicId?.let { viewModel.loadDetailNetworking(it) }
        }
    }

    override fun onPause() {
        if (::adoptionFlow.isInitialized) adoptionFlow.clearDialog()
        deleteDialog?.dismiss(); deleteDialog = null
        super.onPause()
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
