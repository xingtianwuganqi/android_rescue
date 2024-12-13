package com.rescue.flutter_720yun.show.activity

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityGambitListBinding
import com.rescue.flutter_720yun.home.models.TagInfoModel
import com.rescue.flutter_720yun.show.adapter.GambitListAdapter
import com.rescue.flutter_720yun.show.models.GambitListModel
import com.rescue.flutter_720yun.show.viewmodels.GambitListViewModel
import com.rescue.flutter_720yun.util.UiState
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import kotlinx.coroutines.selects.select

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

        // 获取页面类型
        val selectModel = intent.getParcelableArrayListExtra<GambitListModel>("gambit_arr")
        viewModel.selectModel = selectModel?.first()

        addViewAction()
        addViewModelObserver()
        refreshData()
        addBackListener()
    }

    override fun addViewAction() {
        super.addViewAction()
        binding.refreshLayout.setRefreshHeader(MaterialHeader(this))
        binding.refreshLayout.setOnRefreshListener {
            refreshData()
        }

        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        adapter = GambitListAdapter(mutableListOf(), tapListener = { item ->
            adapter.uploadItem(item)
            viewModel.selectModel = item
        })
        binding.recyclerview.adapter = adapter
    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()

        viewModel.isLoading.observe(this) {
            if (it == false) {
                binding.refreshLayout.finishRefresh()
            }
        }

        viewModel.uiState.observe(this) { it ->
            when(it) {
                is UiState.FirstLoading -> {
                    showLoading()
                }

                is UiState.Success -> {
                    showSuccess()
                    var data = it.data
                    viewModel.selectModel?.let { selectModel ->
                        data = data.map { item ->
                            item.selected = item.id == selectModel.id
                            item
                        }.toList()
                    }
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

    // 返回按钮
    override fun finishAction() {
        super.finishAction()
        sendResultAndFinish()
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



    private fun sendResultAndFinish() {
        viewModel.selectModel?.let {
            val intent = Intent()
            intent.putParcelableArrayListExtra("result_arr", arrayListOf(it))
            setResult(Activity.RESULT_OK, intent)
        }


    }
}