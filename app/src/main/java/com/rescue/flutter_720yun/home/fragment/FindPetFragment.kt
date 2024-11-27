package com.rescue.flutter_720yun.home.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.FragmentFindPetBinding
import com.rescue.flutter_720yun.home.activity.FindPetDetailActivity
import com.rescue.flutter_720yun.home.adapter.FindPetItemClickListener
import com.rescue.flutter_720yun.home.adapter.FindPetListAdapter
import com.rescue.flutter_720yun.home.models.FindPetModel
import com.rescue.flutter_720yun.home.viewmodels.FindPetViewModel
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.lazyLogin
import com.rescue.flutter_720yun.util.toastString
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.api.RefreshFooter
import com.scwang.smart.refresh.layout.wrapper.RefreshFooterWrapper

class FindPetFragment : Fragment(), FindPetItemClickListener {

    private var _binding: FragmentFindPetBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[FindPetViewModel::class.java]
    }

    private lateinit var adapter: FindPetListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFindPetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addViewAction()
        addViewModelObserver()
        if (viewModel.uiState.value !is UiState.Success) {
            viewModel.findPetListNetworking(RefreshState.REFRESH)
        }
    }


    private fun addViewAction() {
        binding.refreshLayout.setRefreshHeader(MaterialHeader(activity))
        binding.refreshLayout.setRefreshFooter(ClassicsFooter(activity))
        binding.refreshLayout.setOnRefreshListener {
            viewModel.findPetListNetworking(RefreshState.REFRESH)
        }
        binding.refreshLayout.setOnLoadMoreListener {
            viewModel.findPetListNetworking(RefreshState.MORE)
        }

        binding.backImage.setOnClickListener{
            val intent = Intent(activity, FindPetDetailActivity::class.java)
            startActivity(intent)
        }

        adapter = FindPetListAdapter(mutableListOf(), this)
        binding.findList.layoutManager = LinearLayoutManager(activity)
        binding.findList.adapter = adapter
    }

    private fun addViewModelObserver() {
        viewModel.uiState.observe(viewLifecycleOwner) {
            when(it) {
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
            adapter.uploadItem(it)
        }


    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorView.visibility = View.GONE
        binding.contentFrame.visibility = View.GONE
    }

    private fun showSuccess() {
        binding.progressBar.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.contentFrame.visibility = View.VISIBLE
    }

    private fun showError(error: String) {
        binding.progressBar.visibility = View.GONE
        binding.errorView.visibility = View.VISIBLE
        binding.contentFrame.visibility = View.GONE
        binding.errorView.text = error
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun userClick(model: FindPetModel?) {

    }

    override fun getContactClick(model: FindPetModel?) {
        lazyLogin(requireActivity()) {
            if (model?.getedcontact == false && model.contact == null) {
                viewModel.getFindPetContactNetworking(model)
            }else{
                // 复制到剪贴板
                BaseApplication.context.resources.getString(R.string.copy_success).toastString()
            }
        }
    }

    override fun likeActionClick(model: FindPetModel?) {

    }

    override fun collectionClick(model: FindPetModel?) {

    }

    override fun commentClick(model: FindPetModel?) {

    }
}