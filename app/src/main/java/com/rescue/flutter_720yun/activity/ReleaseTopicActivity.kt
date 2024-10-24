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
import com.google.android.flexbox.FlexboxLayoutManager
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adapter.TagListAdapter
import com.rescue.flutter_720yun.adapter.TagListClickListener
import com.rescue.flutter_720yun.databinding.ActivityReleaseTopicBinding
import com.rescue.flutter_720yun.models.BaseListResp
import com.rescue.flutter_720yun.models.TagInfoModel
import com.rescue.flutter_720yun.viewmodels.ReleaseTopicViewModel

class ReleaseTopicActivity : BaseActivity(), TagListClickListener {

    private var _binding: ActivityReleaseTopicBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TagListAdapter

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
        val list = ArrayList(viewModel.selectTags.value?.toMutableList())
        intent.putParcelableArrayListExtra("tagModels", list)
        tagSelectLauncher.launch(intent)
    }
}