package com.rescue.flutter_720yun.show.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
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
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityShowReleaseBinding
import com.rescue.flutter_720yun.home.activity.LoginActivity
import com.rescue.flutter_720yun.home.adapter.ReleaseImageClickListener
import com.rescue.flutter_720yun.home.adapter.ReleaseImagesAdapter
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.home.models.TagInfoModel
import com.rescue.flutter_720yun.show.models.GambitListModel
import com.rescue.flutter_720yun.show.viewmodels.ShowReleaseViewModel
import com.rescue.flutter_720yun.util.GlideEngine
import com.rescue.flutter_720yun.util.LoadingDialog
import com.rescue.flutter_720yun.util.dateFormatter
import com.rescue.flutter_720yun.util.randomString
import com.rescue.flutter_720yun.util.toastString
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropImageEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.findItem(R.id.action_publish)

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
                pushTopicNetworking()
                true
            }else ->{
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun pushTopicNetworking() {

        viewModel.releaseInfo.content = binding.contentEdit.text.trim().toString()
        val photos = viewModel.releaseInfo.photos.filter {
            !it.isAdd
        }
        if (photos.isEmpty()) {
            BaseApplication.context.getString(R.string.show_image_desc).toastString()
            return
        }

        if (binding.contentEdit.text.trim().isEmpty()){
            BaseApplication.context.getString(R.string.show_input_desc).toastString()
            return
        }
        viewModel.showReleaseModel.instruction = binding.contentEdit.text.trim().toString()
        startPublish()
    }

    private fun startPublish() {
        LoadingDialog.show(this)
        // 发布
        if (viewModel.uploadToken.value != null) {
            viewModel.imageUpload(viewModel.uploadToken.value!!)
        }else{
            viewModel.getUploadTokenNetworking()
        }
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

        binding.gambitButton.setOnClickListener {
            val intent = Intent(this, GambitListActivity::class.java)
            viewModel.gambitModel?.let {
                intent.putParcelableArrayListExtra("gambit_arr", arrayListOf(it))
            }
            gambitLauncher.launch(intent)
        }

        binding.contentEdit.addTextChangedListener(
            beforeTextChanged = { text, start, count, after ->

            },
            onTextChanged = { charSequence: CharSequence?, i: Int, i1: Int, i2: Int ->

            },
            afterTextChanged = {
                if (it?.length != 0) {
                    val number = (it?.length ?: 0)
                    binding.limitText.text = "$number/200"
                }else{
                    binding.limitText.text = "0/200"
                }
                val maxLength = 200 // 假设最大字数为10
                if (it != null && it.length > maxLength) {
                    // 如果输入的字数超过了最大字数，将其设置为最大字数
                    binding.contentEdit.setText(it.subSequence(0, maxLength))
                    binding.contentEdit.setSelection(maxLength) // 设置光标位置
                }
            }
        )

        val gridManager = GridLayoutManager(this, 3)
        binding.recyclerview.layoutManager = gridManager
        adapter = ReleaseImagesAdapter(viewModel.releaseInfo.photos.toMutableList())
        binding.recyclerview.adapter = adapter
        adapter.setClickListener(this)
    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()

        viewModel.checkCode.observe(this) {code ->
            code?.let {
                when(code) {
                    209 -> { // 未绑定手机号
                        ContextCompat.getString(this,R.string.login_unbind).toastString()
                        GlobalScope.launch(Dispatchers.Main) {
                            delay(2000)
                        }
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.putExtra("type", "bindPhone")
                        startActivity(intent)
                    }
                    210 -> { // 未验证手机号
                        ContextCompat.getString(this,R.string.login_uncheck).toastString()
                        GlobalScope.launch(Dispatchers.Main) {
                            delay(2000)
                        }
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.putExtra("type", "checkPhone")
                        startActivity(intent)
                    }
                    300 -> {
                        BaseApplication.context.resources.getString(R.string.show_upload_error).toastString()
                    }else ->{

                    }

                }
            }
        }

        viewModel.imageUploadCompletion.observe(this) {
            if (it == 1) {
                viewModel.showReleaseNetworking()
            }else if (it == 2){
                BaseApplication.context.resources.getString(R.string.release_image_fail).toastString()
                Log.d("TAG","${BaseApplication.context.resources.getString(R.string.release_image_fail)}")
                LoadingDialog.hide()
            }
        }

        viewModel.uploadToken.observe(this) {
            it?.let { value ->
                viewModel.imageUpload(value)
            }
        }

        viewModel.releaseSuccess.observe(this) {
            if (it == 200) {
                LoadingDialog.hide()
                BaseApplication.context.resources.getString(R.string.release_success).toastString()
                GlobalScope.launch(Dispatchers.Main) {
                    delay(2000)
                    sendResultAndFinish()
                    finish()
                }
            }else{
                LoadingDialog.hide()
                BaseApplication.context.resources.getString(R.string.release_fail).toastString()
                Log.d("TAG","${BaseApplication.context.resources.getString(R.string.release_fail).toastString()}")

            }
        }
    }

    private fun uploadGambit(model: GambitListModel) {
        viewModel.gambitModel = model
        binding.addGambit.visibility = View.GONE
        binding.gambitButton.visibility = View.VISIBLE
        binding.gambitButton.text = model.descript
        viewModel.showReleaseModel.gambit_id = model.id
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
                val options = UCrop.Options()
                options.isForbidSkipMultipleCrop(true) //多图裁剪时是否支持跳过
                options.isCropDragSmoothToCenter(true) // 图片是否跟随裁剪框居中
                options.setHideBottomControls(true)
                options.setCircleDimmedLayer(false)
                ucrop.withOptions(options)
                ucrop.withAspectRatio(1F, 1F)
                ucrop.start(fragment.requireActivity(), fragment, requestCode)
            }
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
            .forResult(object : OnResultCallbackListener<LocalMedia> {
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

                override fun onCancel() {

                }

            })
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
                )
            )
        }
        adapter.refreshItems(viewModel.releaseInfo.photos.toList())
    }

    private fun sendResultAndFinish() {
        val intent = Intent()
        intent.putExtra("published", true)
        setResult(Activity.RESULT_OK, intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}