package com.rescue.flutter_720yun.home.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.FragmentFindPetBinding
import com.rescue.flutter_720yun.home.viewmodels.FindPetViewModel

class FindPetFragment : Fragment() {

    private var _binding: FragmentFindPetBinding? = null
    private val binding get() = _binding!!
    private val viewmodel by lazy {
        ViewModelProvider(this)[FindPetViewModel::class.java]
    }

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
        addViewModelObserver()
    }

    private fun addViewModelObserver() {

    }

    override fun onDestroyView() {
        super.onDestroyView()

    }
}