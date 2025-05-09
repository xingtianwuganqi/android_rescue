package com.rescue.flutter_720yun.home.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivitySearchBinding
import com.rescue.flutter_720yun.home.fragment.HomeFragment
import com.rescue.flutter_720yun.home.fragment.SearchHistoryFragment
import com.rescue.flutter_720yun.home.viewmodels.SearchViewModel

class SearchActivity : AppCompatActivity() {

    private var _binding: ActivitySearchBinding? = null
    private val binding get() = _binding
    private lateinit var viewModel: SearchViewModel
    private lateinit var hotFragment: Fragment
    private lateinit var listFragment: HomeFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        // 仅在 Activity 首次创建时添加 Fragment，避免重复添加
        if (savedInstanceState == null) {
            hotFragment = SearchHistoryFragment()
            listFragment = HomeFragment.newInstance("1")

            // 添加 Fragment 到 FrameLayout
            supportFragmentManager.beginTransaction()
                .add(R.id.search_frame, hotFragment) // 添加第一个 Fragment
                .add(R.id.search_frame, listFragment) // 添加第二个 Fragment
                .hide(listFragment) // 隐藏第二个 Fragment
                .commit()
        }

        // 添加点击
        addClickListener()
        addViewModelObserver()
    }

    fun showHotFragment() {
        supportFragmentManager.beginTransaction()
            .hide(listFragment) // 隐藏列表 Fragment
            .show(hotFragment) // 显示历史记录 Fragment
            .commit()
    }

    private fun showListFragment() {
        supportFragmentManager.beginTransaction()
            .hide(hotFragment) // 隐藏历史记录 Fragment
            .show(listFragment) // 显示列表 Fragment
            .commit()
    }


    private fun addClickListener() {
        binding?.backButton?.setOnClickListener {
            finish()
        }

        binding?.searchButton?.setOnClickListener {
            val text = binding?.editText?.text?.trim()
            if (text != null) {
                if (text.isNotEmpty()) {
                    startSearch(text.toString())
                    viewModel.addSearchKeyToLocalJson(text.toString())
                }
            }
        }

        binding?.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (binding?.editText?.text.isNullOrBlank() || binding?.editText?.text.isNullOrEmpty()) {
                    showHotFragment()
                    viewModel.cleanSearchList()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })

    }

    private fun startSearch(keyword: String) {
        showListFragment()
//        viewModel.beginSearch(keyword)
        listFragment.beginSearch(keyword)
    }

    private fun addViewModelObserver() {
        viewModel.keyword.observe(this) {
            Log.d("TAG", "search key changed $it")
            binding?.editText?.setText(it)
            // 开始搜索
            startSearch(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}