package com.rescue.flutter_720yun.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.flexbox.FlexboxLayoutManager
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adapter.ReleaseImagesAdapter
import com.rescue.flutter_720yun.adapter.TagListAdapter
import com.rescue.flutter_720yun.adapter.TagListClickListener
import com.rescue.flutter_720yun.databinding.ActivityReleaseTopicBinding
import com.rescue.flutter_720yun.models.BaseListResp
import com.rescue.flutter_720yun.models.TagInfoModel
import com.rescue.flutter_720yun.viewmodels.ReleaseTopicViewModel
import android.graphics.Rect
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.engine.ImageEngine
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.rescue.flutter_720yun.util.GlideEngine
import com.rescue.flutter_720yun.adapter.ReleaseImageClickListener
import com.rescue.flutter_720yun.models.CoachReleasePhoto
import com.rescue.flutter_720yun.util.dateFormatter
import com.rescue.flutter_720yun.util.randomString

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
        setupToolbar("发布")

        viewModelObserver()
        addClick()

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

//        val rootView = window.decorView.findViewById<View>(android.R.id.content)
//        rootView.viewTreeObserver.addOnGlobalLayoutListener {
//            val rect = Rect()
//            rootView.getWindowVisibleDisplayFrame(rect)
//
//            val screenHeight = rootView.rootView.height
//            val keywordHeight = screenHeight - rect.bottom
//
//            // 如果键盘弹出，键盘高度会大于屏幕的1/4
//            if (keywordHeight > screenHeight * 0.25) {
//                binding.address.translationY = -keywordHeight.toFloat()
//                binding.contact.translationY = -keywordHeight.toFloat()
//                binding.imagesRecyclerview.translationY = -keywordHeight.toFloat()
//            }else{
//                binding.address.translationY = 0f
//                binding.contact.translationY = 0f
//                binding.imagesRecyclerview.translationY = 0f
//            }
//        }
    }

    private fun addClick() {
        binding.addTags.setOnClickListener {
            val intent = Intent(this, TagListActivity::class.java)
            tagSelectLauncher.launch(intent)
        }
    }



    private fun uploadTagAdapter(items: ArrayList<TagInfoModel>?) {
        viewModel.uploadSelectTags(items?.toList() ?: listOf())
    }

    private fun viewModelObserver() {
        viewModel.selectTags.observe(this) {
            Log.d("TAG", "selectTags changed $it")
            if (it.isNotEmpty()) {
                adapter.addItems(it)
                binding.tagsRecyclerview.visibility = View.VISIBLE
                binding.addTags.visibility = View.GONE
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
            viewModel.releaseInfo.photos.add(CoachReleasePhoto(true,
                null,
                null,
                null)
            )
        }
        imagesAdapter.refreshItems(viewModel.releaseInfo.photos.toList())
    }
}