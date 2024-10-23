package com.rescue.flutter_720yun.activity

import android.content.Context
import android.os.Bundle
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

class TagListActivity : BaseActivity(), TagListClickListener {

    private var _binding: ActivityTagListBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy {
        ViewModelProvider(this)[ReleaseTopicViewModel::class.java]
    }

    private lateinit var adapter: TagListAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_tag_list)
        setupToolbar(resources.getString(R.string.release_chose_tag))
        _binding = ActivityTagListBinding.bind(baseBinding.contentFrame.getChildAt(2))


        viewModel.uiState.observe(this) {
            when(it) {
                is UiState.FirstLoading ->{
                    showLoading()
                }
                is UiState.Success ->{
                    showSuccess()
                    adapter = TagListAdapter(it.data)
                    val layoutManager = FlexboxLayoutManager(this)
                    binding.tagsRecyclerview.layoutManager = layoutManager
                    binding.tagsRecyclerview.adapter = adapter
                    adapter.setListener(this)
                }

                is UiState.Error ->{
                    showError(it.message)
                }
            }
        }

        // 网络请求
        getTogsNetworking()
    }

    private fun getTogsNetworking() {
        viewModel.getTagsNetworking()
    }

    override fun onItemClick(item: TagInfoModel) {
        viewModel.uploadSelectTag(item)
    }
}