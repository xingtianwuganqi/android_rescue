package com.rescue.flutter_720yun.user.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityBlackListBinding
import com.rescue.flutter_720yun.home.adapter.HomeListAdapter
import com.rescue.flutter_720yun.user.viewmodels.BlackListViewModel
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader

class BlackListActivity : BaseActivity() {

    private var _binding: ActivityBlackListBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[BlackListViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun addViewAction() {
        super.addViewAction()
        val recyclerView: RecyclerView = binding.recyclerview
        recyclerView.layoutManager = LinearLayoutManager(this)

        binding.refreshLayout.setRefreshHeader(MaterialHeader(this))
        binding.refreshLayout.setRefreshFooter(ClassicsFooter(this))
        binding.refreshLayout.setOnRefreshListener {

        }
        binding.refreshLayout.setOnLoadMoreListener {

        }
    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()

    }

    override fun onDestroy() {
        super.onDestroy()

    }
}