package com.rescue.flutter_720yun.user.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.engine.CompressFileEngine
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityUserInfoEditBinding
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.home.models.UserInfoModel
import com.rescue.flutter_720yun.user.viewmodels.UserInfoEditViewModel
import com.rescue.flutter_720yun.util.GlideEngine
import com.rescue.flutter_720yun.util.LoadingDialog
import com.rescue.flutter_720yun.util.dateFormatter
import com.rescue.flutter_720yun.util.randomString
import com.rescue.flutter_720yun.util.toImgUrl
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

class UserInfoEditActivity : BaseActivity() {

    private var _binding: ActivityUserInfoEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy {
        ViewModelProvider(this)[UserInfoEditViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_user_info_edit)
        _binding = ActivityUserInfoEditBinding.bind(baseBinding.contentFrame.getChildAt(2))
        setupToolbar(resources.getString(R.string.user_edit))

        val userInfo = intent.getParcelableArrayListExtra<UserInfoModel>("userInfo")
        userInfo?.first()?.let {
            viewModel.uploadUserModel(it)
        }

        addViewAction()
        addViewModelObserver()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.findItem(R.id.action_publish)
        menuItem?.title = resources.getString(R.string.save)
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
                saveClickAction()
                true
            }else ->{
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun saveClickAction() {
        if (viewModel.userInfo.value?.username != null) {
            LoadingDialog.show(this)
            viewModel.uploadNetworking(viewModel.userInfo.value?.username!!, viewModel.userInfo.value?.avator)
        }else{
            resources.getString(R.string.user_edit_nickname).toastString()
        }
    }

    override fun addViewAction() {
        super.addViewAction()
        binding.headImg.setOnClickListener {
            uploadHeadImage()
        }

        binding.username.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                viewModel.uploadNickname(text)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })
    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()
        viewModel.userInfo.observe(this) {
            Glide.with(this).load(it.avator?.toImgUrl())
                .placeholder(R.drawable.icon_eee)
                .into(binding.headImg)

            binding.username.setText(it.username)
        }

        viewModel.uploadToken.observe(this) {
            it?.let { token ->
                viewModel.imageUpload(token)
            }
        }

        viewModel.releasePhoto.observe(this) {
            it?.let { photo ->
                uploadPhoto(photo)
            }
        }

        viewModel.uploadComplete.observe(this) {
            if (it == true) {
                viewModel.releasePhoto.value?.photoKey?.let { it1 ->
                    viewModel.uploadUserInfo(it1)
                    Glide.with(this)
                        .load(it1.toImgUrl())
                        .placeholder(R.drawable.icon_eee)
                        .into(binding.headImg)
                }
            }else{
                resources.getString(R.string.show_upload_error).toastString()
            }
        }

        viewModel.uploadSucc.observe(this) {
            if (it == true) {
                LoadingDialog.hide()
                resources.getString(R.string.save_success).toastString()
                GlobalScope.launch(Dispatchers.Main) {
                    delay(2000)
                    sendResultAndFinish()
                    finish()
                }
            }else{
                resources.getString(R.string.save_fail).toastString()
            }

        }
    }

    fun uploadPhoto(photo: CoachReleasePhoto) {
        if(viewModel.uploadToken.value != null) {
            viewModel.uploadToken.value?.let {
                viewModel.imageUpload(it)
            }
        }else{
            viewModel.getUploadTokenNetworking()
        }
    }

    private fun uploadHeadImage() {
        val allowSize = 1
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
                    if (photos?.first() != null) {

                        viewModel.uploadReleasePhoto(photos.first())
                    }
                }

                override fun onCancel() {

                }

            })
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