package com.rescue.flutter_720yun.user.activity

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.engine.CompressFileEngine
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityBlackDetailBinding
import com.rescue.flutter_720yun.home.adapter.ReleaseImageClickListener
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.user.adapter.BlackDetailAdapter
import com.rescue.flutter_720yun.user.models.ReleaseReportInfoModel
import com.rescue.flutter_720yun.user.viewmodels.BlackDetailViewModel
import com.rescue.flutter_720yun.util.GlideEngine
import com.rescue.flutter_720yun.util.dateFormatter
import com.rescue.flutter_720yun.util.randomString
import com.rescue.flutter_720yun.util.toastString
import com.wei.wimagepreviewlib.WImagePreviewBuilder
import top.zibin.luban.Luban
import top.zibin.luban.OnNewCompressListener
import java.io.File
import java.util.ArrayList

class BlackDetailActivity : BaseActivity(), ReleaseImageClickListener {

    private var _binding: ActivityBlackDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[BlackDetailViewModel::class.java]
    }
    private lateinit var adapter: BlackDetailAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_black_detail)
        setupToolbar(resources.getString(R.string.user_report))
        _binding = ActivityBlackDetailBinding.bind(baseBinding.contentFrame.getChildAt(2))

        val blackId = intent.getIntExtra("blackId", 0)
        viewModel.blackId = blackId
        if (blackId != 0) {
            Log.d("TAG", "start request $blackId")
            viewModel.getBlackDetail(viewModel.blackId)
        }

        addViewAction()
        addViewModelObserver()
    }

    override fun addViewAction() {
        super.addViewAction()
        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        adapter = BlackDetailAdapter(viewModel.blackId, mutableListOf(), this)
        binding.recyclerview.adapter = adapter
    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()
        viewModel.dataModels.observe(this) {
            adapter.refreshItem(it)
        }
    }

    override fun addImageClick() {
        val images = viewModel.photos.filter {
            !it.isAdd
        }
        val allowSize = 6 - images.size
        PictureSelector.create(this)
            .openGallery(SelectMimeType.ofImage())
            .setImageEngine(GlideEngine.createGlideEngine())
            .setMaxSelectNum(allowSize)
            .isDisplayCamera(false)
            .setCompressEngine(CompressFileEngine { context, source, call ->
                Luban.with(context).load(source).ignoreBy(50).setCompressListener(object : OnNewCompressListener{
                    override fun onStart() {

                    }

                    override fun onSuccess(source: String?, compressFile: File?) {
                        call?.onCallback(source, compressFile?.absolutePath)
                    }

                    override fun onError(source: String?, e: Throwable?) {
                        call?.onCallback(source, null)
                    }
                }).launch()
            })
            .forResult(object : OnResultCallbackListener<LocalMedia>{
                override fun onResult(result: ArrayList<LocalMedia>?) {

                    val photos = result?.map {
                        CoachReleasePhoto(
                            false,
                            "${dateFormatter("yyyy-MM")}/origin/${randomString(8)}.jpeg",
                            it,
                            null,
                        )
                    }
                    if (photos != null) {
                        viewModel.photos.addAll(0, photos)
                        if (viewModel.photos.filter {
                                !it.isAdd
                            }.size == 6) {
                            viewModel.photos = viewModel.photos.filter {
                                !it.isAdd
                            }.toMutableList()
                        }
                        viewModel.uploadImageData()
                    }
                }

                override fun onCancel() {

                }

            })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (viewModel.blackId == 0) {
            menuInflater.inflate(R.menu.menu_toolbar, menu)
            return true
        }else{
            return false
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.findItem(R.id.action_publish)
        menuItem?.title = resources.getString(R.string.user_push)
        // 检查 menuItem 是否不为空
        menuItem?.let {
            val spanString = SpannableString(it.title) // 确保 title 不为空

            // 设置颜色和大小
            val color = ContextCompat.getColor(this, R.color.color_system)
            spanString.setSpan(ForegroundColorSpan(color), 0, spanString.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            spanString.setSpan(AbsoluteSizeSpan(16, true), 0, spanString.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)

            // 设置带格式的标题
            it.title = spanString
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_publish -> {
                startPush()
                true
            }else ->{
                super.onOptionsItemSelected(item)
            }
        }
    }

    // 先上传图片，再提交
    private fun startPush() {

        Log.d("TAG","value is ${viewModel.dataModels.value?.get(0)?.desc}")

        if (viewModel.dataModels.value?.get(0)?.desc.isNullOrEmpty()) {
            viewModel.dataModels.value?.get(0)?.placeholder?.toastString()
            return
        }

        if (viewModel.dataModels.value?.get(4)?.desc.isNullOrEmpty()) {
            viewModel.dataModels.value?.get(4)?.placeholder?.toastString()
            return
        }

        if (viewModel.dataModels.value?.get(5)?.photos?.none {
                !it.isAdd
            } == true) {
            viewModel.dataModels.value?.get(5)?.placeholder?.toastString()
            return
        }
        val model = viewModel.dataModels.value?.get(5)?.photos?.filter {
            !it.isAdd
        }?.map {
            it.photoKey
        }?.let {
            ReleaseReportInfoModel(
                viewModel.dataModels.value?.get(0)?.desc ?: "",
                wx_num = viewModel.dataModels.value?.get(1)?.desc ?: "",
                name = viewModel.dataModels.value?.get(2)?.desc ?: "",
                black_type = viewModel.dataModels.value?.get(3)?.desc ?: "",
                desc = viewModel.dataModels.value?.get(4)?.desc ?: "",
                photos = it.joinToString(","),
            )
        }
        Log.d("model is ","$model")
        if (model != null) {
            viewModel.releaseReportNetworking(model)
        }
    }

    override fun deleteImageClick(item: CoachReleasePhoto) {
        viewModel.photos = viewModel.photos.filter {
            it.photoKey != item.photoKey
        }.toMutableList()
        if (viewModel.photos.filter {
                !it.isAdd
            }.size < 6 && viewModel.photos.filter {
                it.isAdd
            }.isEmpty()) {
            viewModel.photos.add(
                CoachReleasePhoto(true,
                    null,
                    null,
                    null,
                )
            )
        }
        viewModel.uploadImageData()
    }

    override fun photoClickCallBack(position: Int) {
        val imageArr = viewModel.dataModels.value?.last()?.photos?.map {
            "http://img.rxswift.cn/${it.photoKey}"
        }
        imageArr?.let {
            WImagePreviewBuilder
                .load(this)
                .setData(it)
                .setPosition(position)
                .start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}