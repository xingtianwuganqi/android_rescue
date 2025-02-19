package com.rescue.flutter_720yun.user.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityBlackDetailBinding
import com.rescue.flutter_720yun.databinding.ActivityBlackListBinding
import com.rescue.flutter_720yun.user.adapter.BlackDetailAdapter
import com.rescue.flutter_720yun.user.viewmodels.BlackDetailViewModel
import com.rescue.flutter_720yun.user.viewmodels.BlackListViewModel

class BlackDetailActivity : BaseActivity() {

    private var _binding: ActivityBlackDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[BlackDetailViewModel::class.java]
    }
    private lateinit var adapter: BlackDetailAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_black_detail)
        setupToolbar(resources.getString(R.string.user_report))
        _binding = ActivityBlackDetailBinding.bind(baseBinding.contentFrame.getChildAt(2))
        addViewAction()
        addViewModelObserver()
    }

    override fun addViewAction() {
        super.addViewAction()
        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        adapter = BlackDetailAdapter(mutableListOf())
        binding.recyclerview.adapter = adapter
    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()
        viewModel.dataModels.observe(this) {
            adapter.refreshItem(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}