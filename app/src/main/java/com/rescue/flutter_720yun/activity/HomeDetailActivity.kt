package com.rescue.flutter_720yun.activity

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adapter.HomeDetailAdapter
import com.rescue.flutter_720yun.databinding.ActivityHomeDetailBinding
import com.rescue.flutter_720yun.databinding.ActivityLoginBinding
import com.rescue.flutter_720yun.models.HomeDetailModel
import com.rescue.flutter_720yun.models.HomeListModel
import com.rescue.flutter_720yun.ui.home.OnItemClickListener
import com.rescue.flutter_720yun.ui.home.TagInfoAdapter
import com.rescue.flutter_720yun.ui.home.TopicImgAdapter
import com.rescue.flutter_720yun.util.getImages
import com.rescue.flutter_720yun.util.toImgUrl
import com.rescue.flutter_720yun.viewmodels.HomeDetailViewModel
import kotlinx.coroutines.launch

class HomeDetailActivity : BaseActivity() {

    private var _binding: ActivityHomeDetailBinding? = null
    val binding get() = _binding!!
    private lateinit var viewModel: HomeDetailViewModel
    private var topic_id: Int? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_home_detail)
        _binding = ActivityHomeDetailBinding.bind(baseBinding.contentFrame.getChildAt(0))
        setupToolbar("详情")
        viewModel = ViewModelProvider(this)[HomeDetailViewModel::class.java]

        topic_id = intent.getIntExtra("topic_id", 1)

        lifecycleScope.launch {
            viewModel.loadDetailNetworking(topic_id!!)
        }

        viewModel.homeData.observe(this) {
            uploadViews(it)
        }
    }

    private fun uploadViews(homeData: HomeListModel?) {
        val imgRecyclerView = binding.imagesRecyclerview
        imgRecyclerView.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.VERTICAL,
            false)

        val detailList = mutableListOf<HomeDetailModel>()
        val contentModel = HomeDetailModel(0, homeData, null)
        detailList.add(contentModel)
        homeData?.getImages()?.forEach {
            val imageModel = HomeDetailModel(1, null, it)
            detailList.add(imageModel)
        }
        val adapter = HomeDetailAdapter(detailList)
        imgRecyclerView.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

//    override fun onImgClick(model: HomeListModel?, position: Int) {
//
//    }
}