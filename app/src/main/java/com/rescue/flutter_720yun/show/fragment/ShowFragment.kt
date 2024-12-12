package com.rescue.flutter_720yun.show.fragment

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.databinding.FragmentShowBinding
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.show.activity.ShowReleaseActivity
import com.rescue.flutter_720yun.show.adapter.ShowPageListAdapter
import com.rescue.flutter_720yun.show.viewmodels.ShowViewModel
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.toastString
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader

class ShowFragment : Fragment() {

    private var _binding: FragmentShowBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[ShowViewModel::class.java]
    }
    private lateinit var adapter: ShowPageListAdapter

    private val releaseLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("TAG", "反向传值了")
//            val data: Intent? = result.data
//            val resultData = data?.getParcelableExtra<HomeListModel>("result_model")
//            Log.d("TAG", "data is $data")
//            Log.d("TAG", "resultData is $resultData")
//            Log.d("TAG", "result data is ${resultData?.topic_id}")
//            uploadItem(resultData)
            val data: Intent? = result.data
            val resultValue = data?.getIntExtra("result_value", 0)
            if (resultValue == 1) {
                // 刷新列表

            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShowBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModelAddObserver()
        if (viewModel.uiState.value !is UiState.Success) {
            viewModel.showPageListNetworking(RefreshState.REFRESH)
        }

        binding.refreshLayout.setRefreshHeader(MaterialHeader(activity))
        binding.refreshLayout.setRefreshFooter(ClassicsFooter(activity))
        binding.refreshLayout.setOnRefreshListener {
            viewModel.showPageListNetworking(RefreshState.REFRESH)
        }
        binding.refreshLayout.setOnLoadMoreListener {
            viewModel.showPageListNetworking(RefreshState.MORE)
        }

        binding.showList.layoutManager = LinearLayoutManager(context)
        adapter = ShowPageListAdapter(mutableListOf())
        binding.showList.adapter = adapter

        binding.tip.setOnClickListener {
            activity?.let {
                val intent = Intent(it, ShowReleaseActivity::class.java)
                releaseLauncher.launch(intent)
            }

        }
    }

    private fun viewModelAddObserver() {

        viewModel.isLoading.observe(viewLifecycleOwner) {
            if (it == false) {
                binding.refreshLayout.finishRefresh()
                binding.refreshLayout.finishLoadMore()
                if (viewModel.isLastPage.value == true) {
                    binding.refreshLayout.finishLoadMoreWithNoMoreData()
                }
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.FirstLoading -> {
                    showLoading()
                }

                is UiState.Success -> {
                    showSuccess()
                    val list = it.data
                    if (viewModel.refreshState.value == RefreshState.REFRESH) {
                        adapter.refreshItem(list)
                    }else if (viewModel.refreshState.value == RefreshState.MORE) {
                        adapter.addItems(list)
                    }
                }

                is UiState.Error -> {
                    showError(it.message ?: "")
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            if (it == false) {
                binding.refreshLayout.finishRefresh()
                binding.refreshLayout.finishLoadMore()
                if (viewModel.isLastPage.value == true) {
                    binding.refreshLayout.finishLoadMoreWithNoMoreData()
                }
            }
        }

        viewModel.errorMsg.observe(viewLifecycleOwner) {
            it?.toastString()
        }

        viewModel.changeModel.observe(viewLifecycleOwner) {
//            adapter.uploadItem(it)
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorView.visibility = View.GONE
        binding.refreshLayout.visibility = View.GONE
    }

    private fun showSuccess() {
        binding.progressBar.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.refreshLayout.visibility = View.VISIBLE
    }

    private fun showError(error: String) {
        binding.progressBar.visibility = View.GONE
        binding.errorView.visibility = View.VISIBLE
        binding.refreshLayout.visibility = View.GONE
        binding.errorView.text = error
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}