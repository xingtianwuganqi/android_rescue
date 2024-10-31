package com.rescue.flutter_720yun.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.flexbox.FlexboxLayoutManager
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adapter.SearchHistoryItemClickListener
import com.rescue.flutter_720yun.adapter.TagListAdapter
import com.rescue.flutter_720yun.adapter.TagListClickListener
import com.rescue.flutter_720yun.databinding.ActivityTagListBinding
import com.rescue.flutter_720yun.models.TagInfoModel
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.viewmodels.ReleaseTopicViewModel
import com.rescue.flutter_720yun.viewmodels.TagListViewModel

class TagListActivity : BaseActivity(), TagListClickListener {

    private var _binding: ActivityTagListBinding? = null
    private val binding get() = _binding!!


    private val viewModel by lazy {
        ViewModelProvider(this)[TagListViewModel::class.java]
    }

    private lateinit var adapter: TagListAdapter
    private var selectArr: List<TagInfoModel>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_tag_list)
        setupToolbar(resources.getString(R.string.release_chose_tag))
        _binding = ActivityTagListBinding.bind(baseBinding.contentFrame.getChildAt(2))

        val arrayList = intent.getParcelableArrayListExtra<TagInfoModel>("tagModels")
        if (arrayList != null) {
            selectArr = arrayList?.toList()
        }

        adapter = TagListAdapter(mutableListOf())
        val layoutManager = FlexboxLayoutManager(this)
        binding.tagsRecyclerview.layoutManager = layoutManager
        binding.tagsRecyclerview.adapter = adapter
        adapter.setListener(this)

        viewModelObserver()
        // 网络请求
        getTogsNetworking()
        // 监听系统返回
        addBackListener()
    }

    private fun getTogsNetworking() {
        viewModel.getTagsNetworking()
    }

    private fun viewModelObserver() {
        viewModel.uiState.observe(this) {
            when(it) {
                is UiState.FirstLoading ->{
                    showLoading()
                }
                is UiState.Success ->{
                    showSuccess()
                    var data = it.data
                    if (selectArr != null) {
                        val ids = selectArr!!.map {item ->
                            item.id
                        }
                        data = data.map{ dataModel ->
                           dataModel.isSelected = ids.contains(dataModel.id)
                            dataModel
                        }
                    }
                    adapter.addItems(data)
                }

                is UiState.Error ->{
                    showError(it.message)
                }
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

    // 返回按钮
    override fun finishAction() {
        super.finishAction()
        sendResultAndFinish()
    }

    private fun sendResultAndFinish() {
        var arrayList: ArrayList<TagInfoModel>?
        if (viewModel.selectTags.value != null) {
            arrayList = ArrayList(viewModel.selectTags?.value?.toMutableList())
        }else{
            arrayList = ArrayList()
        }
        val intent = Intent()
        intent.putParcelableArrayListExtra("result_tag", arrayList)
        setResult(Activity.RESULT_OK, intent)

    }

    override fun onItemClick(item: TagInfoModel) {
        viewModel.uploadSelectTag(item)
        adapter.uploadItem(item)
    }
}