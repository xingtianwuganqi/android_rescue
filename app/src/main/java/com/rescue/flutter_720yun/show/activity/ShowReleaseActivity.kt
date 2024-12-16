package com.rescue.flutter_720yun.show.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.engine.CompressFileEngine
import com.luck.picture.lib.entity.LocalMedia
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityShowReleaseBinding
import com.rescue.flutter_720yun.home.adapter.ReleaseImageClickListener
import com.rescue.flutter_720yun.home.adapter.ReleaseImagesAdapter
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.show.models.GambitListModel
import com.rescue.flutter_720yun.show.viewmodels.ShowReleaseViewModel
import com.rescue.flutter_720yun.util.GlideEngine
import com.rescue.flutter_720yun.util.dateFormatter
import com.rescue.flutter_720yun.util.randomString
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropImageEngine
import top.zibin.luban.Luban
import top.zibin.luban.OnNewCompressListener
import java.io.File

class ShowReleaseActivity : BaseActivity(), ReleaseImageClickListener {

    private var _binding: ActivityShowReleaseBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[ShowReleaseViewModel::class.java]
    }

    private lateinit var adapter: ReleaseImagesAdapter

    private val gambitLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val gambitInfo = data?.getParcelableArrayListExtra<GambitListModel>("result_arr")
            gambitInfo?.first()?.let {
                uploadGambit(it)
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_show_release)
        setupToolbar(resources.getString(R.string.release_upload))
        _binding = ActivityShowReleaseBinding.bind(baseBinding.contentFrame.getChildAt(2))
        addViewAction()
        addViewModelObserver()
    }

    override fun addViewAction() {
        super.addViewAction()
        binding.addGambit.setOnClickListener {
            val intent = Intent(this, GambitListActivity::class.java)
            viewModel.gambitModel?.let {
                intent.putParcelableArrayListExtra("gambit_arr", arrayListOf(it))
            }
            gambitLauncher.launch(intent)
        }

        val gridManager = GridLayoutManager(this, 3)
        binding.recyclerview.layoutManager = gridManager
        adapter = ReleaseImagesAdapter(viewModel.releaseInfo.photos.toMutableList())
        binding.recyclerview.adapter = adapter
        adapter.setClickListener(this)
    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()

    }

    private fun uploadGambit(model: GambitListModel) {
        viewModel.gambitModel = model
        binding.addGambit.visibility = View.GONE
        binding.gambitButton.visibility = View.VISIBLE
        binding.gambitButton.text = model.descript
    }

    override fun addImageClick() {
        val images = viewModel.releaseInfo.photos.filter {
            !it.isAdd
        }
        val allowSize = 6 - images.size
        PictureSelector.create(this)
            .openGallery(SelectMimeType.ofImage())
            .setImageEngine(GlideEngine.createGlideEngine())
            .setMaxSelectNum(allowSize)
            .isDisplayCamera(false)
            .setCompressEngine(CompressFileEngine { context, source, call ->
                Luban.with(context).load(source).ignoreBy(50).setCompressListener(object :
                    OnNewCompressListener {
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
            .setCropEngine { fragment, srcUri, destinationUri, dataSource, requestCode ->
                val ucrop = UCrop.of(srcUri, destinationUri, dataSource)

                ucrop.setImageEngine(object : UCropImageEngine {
                    override fun loadImage(context: Context?, url: String?, imageView: ImageView?) {
                        if (context != null) {
                            if (imageView != null) {
                                Glide.with(context).load(url).into(imageView)
                            }
                        }
                    }

                    override fun loadImage(
                        context: Context?,
                        url: Uri?,
                        maxWidth: Int,
                        maxHeight: Int,
                        call: UCropImageEngine.OnCallbackListener<Bitmap>?
                    ) {
                        if (context != null) {
                            Glide.with(context).asBitmap().override(maxWidth, maxHeight).load(url)
                                .into(object :
                                    CustomTarget<Bitmap>() {
                                    override fun onResourceReady(
                                        resource: Bitmap,
                                        transition: Transition<in Bitmap>?
                                    ) {
                                        call?.onCall(resource)
                                    }

                                    override fun onLoadFailed(errorDrawable: Drawable?) {
                                        call?.onCall(null)
                                    }

                                    override fun onLoadCleared(placeholder: Drawable?) {

                                    }

                                })
                        }
                    }
                })
                ucrop.withOptions(UCrop.Options())
                ucrop.withAspectRatio(1F, 1F)
                ucrop.start(this, requestCode)
            }
            .forResult(PictureConfig.CHOOSE_REQUEST) // 设置回调

//            .forResult(object : OnResultCallbackListener<LocalMedia> {
//                override fun onResult(result: java.util.ArrayList<LocalMedia>?) {
//
//                    val photos = result?.map {
//                        CoachReleasePhoto(
//                            false,
//                            "${dateFormatter("yyyy-MM")}/origin/${randomString(8)}.jpeg",
//                            "${dateFormatter("yyyy-MM")}/preview/${randomString(8)}.jpeg",
//                            it,
//                            null,
//                        )
//                    }
//                    if (photos != null) {
//                        viewModel.releaseInfo.photos.addAll(0, photos)
//                        if (viewModel.releaseInfo.photos.filter {
//                                !it.isAdd
//                            }.size == 6) {
//                            viewModel.releaseInfo.photos = viewModel.releaseInfo.photos.filter {
//                                !it.isAdd
//                            }.toMutableList()
//                        }
//                        adapter.refreshItems(viewModel.releaseInfo.photos.toList())
//                    }
//                }
//
//                override fun onCancel() {
//
//                }
//
//            })
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == PictureConfig.CHOOSE_REQUEST) {
            // 获取选择的图片列表
            val selectList: List<LocalMedia> = PictureSelector.obtainSelectorList(data)
            val photos = selectList.map {
                CoachReleasePhoto(
                    false,
                    "${dateFormatter("yyyy-MM")}/origin/${randomString(8)}.jpeg",
                    "${dateFormatter("yyyy-MM")}/preview/${randomString(8)}.jpeg",
                    it,
                    null,
                )
            }
            viewModel.releaseInfo.photos.addAll(0, photos)
            if (viewModel.releaseInfo.photos.filter {
                    !it.isAdd
                }.size == 6) {
                viewModel.releaseInfo.photos = viewModel.releaseInfo.photos.filter {
                    !it.isAdd
                }.toMutableList()
            }
            adapter.refreshItems(viewModel.releaseInfo.photos.toList())

        }
    }

    override fun deleteImageClick(item: CoachReleasePhoto) {
        viewModel.releaseInfo.photos = viewModel.releaseInfo.photos.filter {
            it.photoKey != item.photoKey
        }.toMutableList()
        if (viewModel.releaseInfo.photos.filter {
                !it.isAdd
            }.size < 6 && viewModel.releaseInfo.photos.filter {
                it.isAdd
            }.isEmpty()) {
            viewModel.releaseInfo.photos.add(
                CoachReleasePhoto(true,
                    null,
                    null,
                    null,
                    null
                )
            )
        }
        adapter.refreshItems(viewModel.releaseInfo.photos.toList())
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}