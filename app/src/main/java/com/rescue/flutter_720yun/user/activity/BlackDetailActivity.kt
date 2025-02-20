package com.rescue.flutter_720yun.user.activity

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.rescue.flutter_720yun.databinding.ActivityBlackListBinding
import com.rescue.flutter_720yun.home.adapter.ReleaseImageClickListener
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.user.adapter.BlackDetailAdapter
import com.rescue.flutter_720yun.user.viewmodels.BlackDetailViewModel
import com.rescue.flutter_720yun.user.viewmodels.BlackListViewModel
import com.rescue.flutter_720yun.util.GlideEngine
import com.rescue.flutter_720yun.util.dateFormatter
import com.rescue.flutter_720yun.util.randomString
import top.zibin.luban.Luban
import top.zibin.luban.OnNewCompressListener
import java.io.File

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
        addViewAction()
        addViewModelObserver()
    }

    override fun addViewAction() {
        super.addViewAction()
        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        adapter = BlackDetailAdapter(mutableListOf(), this)
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
                override fun onResult(result: java.util.ArrayList<LocalMedia>?) {

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
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
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


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}