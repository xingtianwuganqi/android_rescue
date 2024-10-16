package com.rescue.flutter_720yun.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivitySearchBinding
import com.rescue.flutter_720yun.fragment.SearchHistoryFragment
import com.rescue.flutter_720yun.viewmodels.SearchViewModel

class SearchActivity : AppCompatActivity() {

    private var _binding: ActivitySearchBinding? = null
    private val binding get() = _binding
    private lateinit var viewModel: SearchViewModel
    private lateinit var hotFragment: Fragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        // 仅在 Activity 首次创建时添加 Fragment，避免重复添加
        if (savedInstanceState == null) {
            hotFragment = SearchHistoryFragment()
            // 将Fragment添加到Activity中
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.search_frame,
                    hotFragment
                ) // R.id.fragment_container是Activity布局文件中Fragment要放置的ViewGroup的ID
                .commit()

        }
        // 添加点击
        addClickListener()
    }

    private fun addClickListener() {
        binding?.backButton?.setOnClickListener {
            finish()
        }

        binding?.searchButton?.setOnClickListener {

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}