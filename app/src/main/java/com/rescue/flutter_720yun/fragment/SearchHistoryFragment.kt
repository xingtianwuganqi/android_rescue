package com.rescue.flutter_720yun.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adapter.SearchHistoryAdapter
import com.rescue.flutter_720yun.adapter.SearchKeywordAdapter
import com.rescue.flutter_720yun.databinding.FragmentSearchHistoryBinding
import com.rescue.flutter_720yun.viewmodels.SearchViewModel

class SearchHistoryFragment : Fragment() {

    private var _binding: FragmentSearchHistoryBinding? = null
    private val binding: FragmentSearchHistoryBinding get() = _binding!!

    private lateinit var viewModel: SearchViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchHistoryBinding.inflate(inflater, container,false)
        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        viewModelRequestData()

        return binding.root
//        return inflater.inflate(R.layout.fragment_search_history, container, false)
    }

    private fun viewModelRequestData() {
        viewModel.keywordModels.observe(viewLifecycleOwner){
            it?.let {
                Log.d("TAG", "data back is $it")
                binding.historyRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.historyRecyclerview.adapter = SearchHistoryAdapter(it)
            }
        }

        viewModel.searchKeywordNetworking()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}