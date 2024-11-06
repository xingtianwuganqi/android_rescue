package com.rescue.flutter_720yun.home.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.FragmentLocationSheetBinding
import android.graphics.Point
import com.google.android.material.tabs.TabLayoutMediator
import com.rescue.flutter_720yun.home.adapter.LocationViewPagerAdapter

class LocationSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentLocationSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLocationSheetBinding.inflate(inflater, container, false)

        binding.viewPager.adapter = LocationViewPagerAdapter(this)
        TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager
        ) { tab, position ->
            when(position) {
                0 -> tab.text = "请选择"
                1 -> tab.text = "请选择"
                2 -> tab.text = "请选择"
            }

        }.attach()

        return binding.root
    }

//    override fun onStart() {
//        super.onStart()
//
//        // 获取当前窗口的宽高
//        val window = dialog?.window
//        val display = window?.windowManager?.defaultDisplay
//        val size = Point()
//        display?.getSize(size)
//        val height = size.y
//
//        // 设置 BottomSheet 高度为屏幕高度的一半
//        val layoutParams = window?.attributes
//        layoutParams?.height = (height * 2 / 3)  // 设置为屏幕的 2/3 高度
//        window?.attributes = layoutParams
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment CustomBottomSheetFragment.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            CustomBottomSheetFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
}