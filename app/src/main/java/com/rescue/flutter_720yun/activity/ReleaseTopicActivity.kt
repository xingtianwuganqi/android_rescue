package com.rescue.flutter_720yun.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityReleaseTopicBinding
import com.rescue.flutter_720yun.models.BaseListResp
import com.rescue.flutter_720yun.models.TagInfoModel
import com.rescue.flutter_720yun.viewmodels.ReleaseTopicViewModel

class ReleaseTopicActivity : BaseActivity() {

    private var _binding: ActivityReleaseTopicBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ReleaseTopicViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_release_topic)
        _binding = ActivityReleaseTopicBinding.bind(baseBinding.contentFrame.getChildAt(2))
        setupToolbar("发布")
        viewModel = ViewModelProvider(this)[ReleaseTopicViewModel::class.java]

        addClick()

    }

    private fun addClick() {
        binding.addTags.setOnClickListener {
            val intent = Intent(this, TagListActivity::class.java)
            startActivity(intent)
        }
    }

    fun viewModelObserver() {
        viewModel
    }
}