package com.rescue.flutter_720yun.home.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.home.adapter.SearchHistoryAdapter
import com.rescue.flutter_720yun.home.adapter.SearchHistoryItemClickListener
import com.rescue.flutter_720yun.databinding.FragmentSearchHistoryBinding
import com.rescue.flutter_720yun.home.viewmodels.SearchViewModel

class SearchHistoryFragment : Fragment(), SearchHistoryItemClickListener {

    private var _binding: FragmentSearchHistoryBinding? = null
    private val binding: FragmentSearchHistoryBinding get() = _binding!!

    private lateinit var viewModel: SearchViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchHistoryBinding.inflate(inflater, container,false)
        viewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]

        viewModelRequestData()

        return binding.root
//        return inflater.inflate(R.layout.fragment_search_history, container, false)
    }

    private fun viewModelRequestData() {
        viewModel.keywordModels.observe(viewLifecycleOwner){
            it?.let {
                binding.historyRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                val adapter = SearchHistoryAdapter(it)
                adapter.setItemClickListener(this)
                binding.historyRecyclerview.adapter = adapter
            }
        }

        viewModel.searchKeywordNetworking()
    }

    override fun itemClick(keyword: String?) {
        keyword?.let {
            viewModel.addSearchKeyToLocalJson(it)
        }
    }

    override fun onDeleteClick() {
        viewModel.deleteLocalKeyword()
    }
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}