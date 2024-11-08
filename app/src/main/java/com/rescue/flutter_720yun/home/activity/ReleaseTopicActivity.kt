package com.rescue.flutter_720yun.home.activity

import android.app.Activity
import android.content.Intent
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
import com.rescue.flutter_720yun.util.GlideEngine
import com.rescue.flutter_720yun.home.adapter.ReleaseImageClickListener
import com.rescue.flutter_720yun.home.fragment.LocationSheetFragment
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.util.dateFormatter
import com.rescue.flutter_720yun.util.randomString
import com.rescue.flutter_720yun.util.toastString
import top.zibin.luban.Luban
import top.zibin.luban.OnNewCompressListener
import java.io.File

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

        // 发布
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

    override fun addViewModelObserver() {
        viewModel.selectTags.observe(this) {
            Log.d("TAG", "selectTags changed $it")
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
    }

    override fun onItemClick(item: TagInfoModel) {
        val intent = Intent(this, TagListActivity::class.java)
        val list = viewModel.selectTags.value?.toMutableList()?.let { ArrayList(it) }
        intent.putParcelableArrayListExtra("tagModels", list)
        tagSelectLauncher.launch(intent)
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
                Luban.with(context).load(source).ignoreBy(60).setCompressListener(object : OnNewCompressListener{
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
                            "${dateFormatter("yyyy-MM")}/preview/${randomString(8)}.jpeg",
                             it
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
                null)
            )
        }
        imagesAdapter.refreshItems(viewModel.releaseInfo.photos.toList())
    }
}