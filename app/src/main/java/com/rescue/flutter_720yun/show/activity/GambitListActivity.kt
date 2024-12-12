package com.rescue.flutter_720yun.show.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityGambitListBinding
import com.rescue.flutter_720yun.show.adapter.GambitListAdapter
import com.rescue.flutter_720yun.show.viewmodels.GambitListViewModel
import com.rescue.flutter_720yun.util.UiState
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader

class GambitListActivity : BaseActivity() {

    private var _binding: ActivityGambitListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: GambitListAdapter
    private val viewModel by lazy {
        ViewModelProvider(this)[GambitListViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_gambit_list)
        setupToolbar("话题")
        _binding = ActivityGambitListBinding.bind(baseBinding.contentFrame.getChildAt(2))

        addViewAction()
        addViewModelObserver()
        refreshData()
    }

    override fun addViewAction() {
        super.addViewAction()
        binding.refreshLayout.setRefreshHeader(MaterialHeader(this))
        binding.refreshLayout.setOnRefreshListener {
            refreshData()
        }

        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        adapter = GambitListAdapter(mutableListOf())
        binding.recyclerview.adapter = adapter
    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()

        viewModel.isLoading.observe(this) {
            if (it == false) {
                binding.refreshLayout.finishRefresh()
            }
        }

        viewModel.uiState.observe(this) {
            when(it) {
                is UiState.FirstLoading -> {
                    showLoading()
                }

                is UiState.Success -> {
                    showSuccess()
                    val data = it.data
                    adapter.refreshItem(data)
                }

                is UiState.Error -> {
                    showError(it.message)
                }
            }
        }
    }

    private fun refreshData() {
        viewModel.gambitListNetworking()
    }
}