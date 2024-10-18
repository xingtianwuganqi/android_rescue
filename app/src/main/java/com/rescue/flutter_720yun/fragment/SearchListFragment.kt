package com.rescue.flutter_720yun.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.FragmentSearchHistoryBinding
import com.rescue.flutter_720yun.databinding.FragmentSearchListBinding
import com.rescue.flutter_720yun.viewmodels.SearchViewModel

class SearchListFragment : Fragment() {

    private var _binding: FragmentSearchListBinding? = null
    private val binding: FragmentSearchListBinding get() = _binding!!
    private lateinit var viewModel: SearchViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchListBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]
        addViewModelObserver()

        return binding.root
    }

    private fun addViewModelObserver() {
        viewModel.searchWord.observe(viewLifecycleOwner) {

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

//    companion object {
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            SearchListFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
}