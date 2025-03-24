package com.rescue.flutter_720yun.home.activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.flexbox.FlexboxLayoutManager
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.adapter.ReleaseImagesAdapter
import com.rescue.flutter_720yun.home.adapter.TagListAdapter
import com.rescue.flutter_720yun.home.adapter.TagListClickListener
import com.rescue.flutter_720yun.databinding.ActivityReleaseTopicBinding
import com.rescue.flutter_720yun.home.models.TagInfoModel
import com.rescue.flutter_720yun.home.viewmodels.ReleaseTopicViewModel
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.engine.CompressFileEngine
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.BaseApplication.Companion.context
import com.rescue.flutter_720yun.util.GlideEngine
import com.rescue.flutter_720yun.home.adapter.ReleaseImageClickListener
import com.rescue.flutter_720yun.home.fragment.LocationSheetFragment
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.util.LoadingDialog
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.dateFormatter
import com.rescue.flutter_720yun.util.randomString
import com.rescue.flutter_720yun.util.toastString
import top.zibin.luban.Luban
import top.zibin.luban.OnNewCompressListener
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.*

class ReleaseTopicActivity : BaseActivity(), TagListClickListener, ReleaseImageClickListener {

    private var _binding: ActivityReleaseTopicBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TagListAdapter

    private lateinit var imagesAdapter: ReleaseImagesAdapter

    private val tagSelectLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        if (it.resultCode == Activity.RESULT_OK) {
            val data:Intent? = it.data
            val resultData = data?.getParcelableArrayListExtra<TagInfoModel>("result_tag")
            uploadTagAdapter(resultData)
        }
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[ReleaseTopicViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_release_topic)
        _binding = ActivityReleaseTopicBinding.bind(baseBinding.contentFrame.getChildAt(2))
        setupToolbar(BaseApplication.context.resources.getString(R.string.release_push))

        adapter = TagListAdapter(mutableListOf())
        val layoutManager = FlexboxLayoutManager(this)
        binding.tagsRecyclerview.layoutManager = layoutManager
        binding.tagsRecyclerview.adapter = adapter
        adapter.setListener(this)

        val gridManager = GridLayoutManager(this, 3)
        binding.imagesRecyclerview.layoutManager = gridManager
        imagesAdapter = ReleaseImagesAdapter(viewModel.releaseInfo.photos.toMutableList())
        binding.imagesRecyclerview.adapter = imagesAdapter
        imagesAdapter.setClickListener(this)

        addViewModelObserver()
        addViewAction()
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
        if (binding.contentEdit.text.trim().isEmpty()){
            BaseApplication.context.getString(R.string.release_context_desc).toastString()
            return
        }
        viewModel.releaseInfo.content = binding.contentEdit.text.trim().toString()
        val photos = viewModel.releaseInfo.photos.filter {
            !it.isAdd
        }
        if (photos.isEmpty()) {
            BaseApplication.context.getString(R.string.release_photos_desc).toastString()
            return
        }
        if (binding.contactEdit.text.trim().isEmpty()) {
            BaseApplication.context.getString(R.string.release_contact_desc).toastString()
            return
        }
        viewModel.releaseInfo.contact = binding.contactEdit.text.trim().toString()

        if ((viewModel.releaseInfo.address?.length ?: 0) == 0) {
            BaseApplication.context.getString(R.string.release_address_desc).toastString()
            return
        }
        pushDialog()
    }

    private fun pushDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.release_push_remind))
        builder.setMessage(resources.getString(R.string.release_dialog))
        builder.setCancelable(false)
        builder.setPositiveButton(resources.getString(R.string.release_confirm_push), DialogInterface.OnClickListener { dialogInterface, i ->
                startPublish()
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

    private fun startPublish() {
        LoadingDialog.show(this)
        // 发布
        if (viewModel.uploadToken.value != null) {
            viewModel.imageUpload(viewModel.uploadToken.value!!)
        }else{
            viewModel.getUploadTokenNetworking()
        }
    }

    private fun requestCameraPermission() {
        XXPermissions.with(this)
            // 申请单个权限
            .permission(Permission.READ_MEDIA_IMAGES)
            // 申请多个权限
//            .permission(Permission.Group.CALENDAR)
            // 设置权限请求拦截器（局部设置）
            //.interceptor(new PermissionInterceptor())
            // 设置不触发错误检测机制（局部设置）
            //.unchecked()
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    if (!allGranted) {
//                        toast("获取部分权限成功，但部分权限未正常授予")
                        return
                    }
                    startSelectPhoto()
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    if (doNotAskAgain) {
                        resources.getString(R.string.photo_permission_never).toastString()
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(context, permissions)
                    } else {
                        resources.getString(R.string.photo_permission_error).toastString()
                    }
                }
            })
    }


    override fun addViewAction() {
        binding.addTags.setOnClickListener {
            val intent = Intent(this, TagListActivity::class.java)
            tagSelectLauncher.launch(intent)
        }

        binding.addressEdit.setOnClickListener {
            val bottomSheetFragment = LocationSheetFragment()
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            bottomSheetFragment.selectCompletion = { it1 ->
                val nameArr = it1.map { item ->
                    item.name
                }
                val addressStr = nameArr.joinToString(".")
                viewModel.releaseInfo.address = addressStr
                binding.addressEdit.setText(addressStr)
            }
        }
    }



    private fun uploadTagAdapter(items: ArrayList<TagInfoModel>?) {
        viewModel.uploadSelectTags(items?.toList() ?: listOf())
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun addViewModelObserver() {
        viewModel.selectTags.observe(this) {
            if (it.isNotEmpty()) {
                adapter.addItems(it)
                binding.tagsRecyclerview.visibility = View.VISIBLE
                binding.addTags.visibility = View.GONE
                viewModel.releaseInfo.tags = it
            }else{
                adapter.clearItems()
                binding.tagsRecyclerview.visibility = View.GONE
                binding.addTags.visibility = View.VISIBLE
            }
        }

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

        viewModel.uploadToken.observe(this) {uploadToken ->
            uploadToken?.let {
                viewModel.imageUpload(it)
            }
        }

        viewModel.imageUploadCompletion.observe(this) {
            if (it == 1) {
                viewModel.releaseTopicNetworking()
            }else if (it == 2){
                BaseApplication.context.resources.getString(R.string.release_image_fail).toastString()
                Log.d("TAG","${BaseApplication.context.resources.getString(R.string.release_image_fail)}")
                LoadingDialog.hide()
            }
        }

        viewModel.releaseSuccess.observe(this) {
            if (it == 200) {
                LoadingDialog.hide()
                BaseApplication.context.resources.getString(R.string.release_success).toastString()
                Log.d("TAG","${BaseApplication.context.resources.getString(R.string.release_success)}")
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

    override fun onItemClick(item: TagInfoModel) {
        val intent = Intent(this, TagListActivity::class.java)
        val list = viewModel.selectTags.value?.toMutableList()?.let { ArrayList(it) }
        intent.putParcelableArrayListExtra("tagModels", list)
        tagSelectLauncher.launch(intent)
    }

    override fun addImageClick() {
        requestCameraPermission()
    }

    private fun startSelectPhoto() {
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
                        viewModel.releaseInfo.photos.addAll(0, photos)
                        if (viewModel.releaseInfo.photos.filter {
                                !it.isAdd
                            }.size == 6) {
                            viewModel.releaseInfo.photos = viewModel.releaseInfo.photos.filter {
                                !it.isAdd
                            }.toMutableList()
                        }
                        imagesAdapter.refreshItems(viewModel.releaseInfo.photos.toList())
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
        imagesAdapter.refreshItems(viewModel.releaseInfo.photos.toList())
    }

    override fun photoClickCallBack(position: Int) {
        
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